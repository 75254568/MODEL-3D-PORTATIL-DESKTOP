"""
Catálogo de diseños únicos: cada chasis declara sus fotos, medidas y piezas propias sobre photo_designs.

Lo que se puede medir solo en las fotos (perfil de los laterales, extremos de la trasera y posición de las teclas)
se calcula aquí; las piezas características de cada equipo se declaran a mano, en píxeles de su foto.
"""
import csv
import json
import math
import os

import bpy
import numpy as np

import build_laptops as core
import photo_designs as pd


# ------------------------------------------------------------------ análisis de imagen

def amj_load_rgba(photo):
    w, h = photo.w, photo.h
    return np.array(photo.img.pixels[:], dtype=np.float32).reshape(h, w, 4)[::-1]


def amj_silhouette_mask(px):
    if px[..., 3].min() < 0.9:
        return px[..., 3] > 0.5
    return px[..., :3].min(axis=2) < 0.93


def amj_side_map(photo, rear_first):
    """Perfil de un lateral: filas superior e inferior del cuerpo, ajustadas a una recta a lo largo de la foto."""
    mask = amj_silhouette_mask(amj_load_rgba(photo))
    w = photo.w
    cols = np.where(mask.any(axis=0))[0]
    c0, c1 = int(cols.min()), int(cols.max())
    xs, tops, bots = [], [], []
    for f in np.linspace(0.12, 0.88, 40):
        c = int(c0 + (c1 - c0) * f)
        rows = np.where(mask[:, c])[0]
        if len(rows):
            xs.append(f)
            tops.append(rows.min())
            bots.append(rows.max())
    a = np.polyfit(xs, tops, 1)
    b = np.polyfit(xs, bots, 1)
    t0, t1 = float(np.polyval(a, 0)), float(np.polyval(a, 1))
    b0, b1 = float(np.polyval(b, 0)) - 1.5, float(np.polyval(b, 1)) - 1.5
    t0, t1 = max(0.0, t0) + 1, max(0.0, t1) + 1
    if rear_first:
        return {"rearPx": c0, "frontPx": c1, "topRear": t0, "topFront": t1, "botRear": b0, "botFront": b1}
    return {"rearPx": c1, "frontPx": c0, "topRear": t1, "topFront": t0, "botRear": b1, "botFront": b0}


def amj_rear_map(photo):
    mask = amj_silhouette_mask(amj_load_rgba(photo))
    cols = np.where(mask.mean(axis=0) > 0.25)[0]
    rows = np.where(mask[:, int(cols.min()):int(cols.max())].mean(axis=1) > 0.6)[0]
    return {"top": int(rows.min()), "bot": int(rows.max()), "cols": (int(cols.min()) + 6, int(cols.max()) - 6)}


def amj_box_blur(a, r):
    s = np.cumsum(np.cumsum(np.pad(a, ((r + 1, r), (r + 1, r)), mode="edge"), axis=0), axis=1)
    k = 2 * r + 1
    return (s[k:, k:] - s[:-k, k:] - s[k:, :-k] + s[:-k, :-k]) / (k * k)


def amj_label_runs(mask):
    """Componentes conexas por tramos de fila; devuelve cajas (x0, y0, x1, y1, área)."""
    parent = []

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i

    prev, boxes = [], {}
    for y, row in enumerate(mask):
        d = np.diff(np.concatenate(([0], row.astype(np.int8), [0])))
        cur = []
        for x0, x1 in zip(np.where(d == 1)[0], np.where(d == -1)[0]):
            lab = None
            for px0, px1, pl in prev:
                if px0 < x1 and x0 < px1:
                    r = find(pl)
                    if lab is None:
                        lab = r
                    elif r != lab:
                        parent[r] = lab
            if lab is None:
                lab = len(parent)
                parent.append(lab)
            cur.append((x0, x1, lab))
            boxes.setdefault(lab, []).append((x0, y, x1, y + 1, x1 - x0))
        prev = cur
    merged = {}
    for lab, parts in boxes.items():
        r = find(lab)
        merged.setdefault(r, []).extend(parts)
    out = []
    for parts in merged.values():
        a = np.array(parts)
        out.append((a[:, 0].min(), a[:, 1].min(), a[:, 2].max(), a[:, 3].max(), a[:, 4].sum()))
    return out


def amj_detect_keys(photo, width_mm):
    """Busca las teclas en la foto de la cubierta. Devuelve filas (cx, cy, w, h) como las de keys.csv."""
    px = amj_load_rgba(photo)[::2, ::2]
    g = (px[..., :3] * [0.299, 0.587, 0.114]).sum(axis=2).astype(np.float32)
    h, w = g.shape
    pitch = 19.05 * w / width_mm
    keys = []
    for gain in (0.92, 0.97, 1.0):
        m = g > amj_box_blur(g, int(pitch * 0.42)) * gain
        m[:, 1:-1] &= m[:, :-2] & m[:, 2:]
        m[1:-1] &= m[:-2] & m[2:]
        found = []
        for x0, y0, x1, y1, area in amj_label_runs(m):
            bw, bh = x1 - x0, y1 - y0
            if 0.52 * pitch <= bh <= 1.08 * pitch and 0.6 * pitch <= bw <= 7.2 * pitch and area / (bw * bh) > 0.7:
                found.append((x0, y0, x1, y1))
        rows = {}
        for b in found:
            rows.setdefault(round((b[1] + b[3]) / 2 / (pitch * 0.5)), []).append(b)
        good = []
        for k, v in rows.items():
            near = v + rows.get(k - 1, []) + rows.get(k + 1, [])
            if len(near) >= 5:
                good += v
        if len(good) > len(keys):
            keys = good
    return [((a + c) / 2 / w - 0.5 + 0.0, 1 - (b + d) / 2 / h, (c - a + 2) / w, (d - b + 2) / h) for a, b, c, d in keys]


# ------------------------------------------------------------------ piezas declaradas

def amj_apply_features(v):
    """Piezas propias. Todas las posiciones son fracciones (0..1) de la foto correspondiente."""
    dw, dh = v.deckP.w, v.deckP.h
    lp = v.ph.get("lid", v.deckP)
    D = lambda u, w: (u * dw, w * dh)
    L = lambda u, w: (u * lp.w, w * lp.h)
    for f in v.c.get("features", []):
        t = f["t"]
        h = f.get("h", 0.12)
        if t == "deckRect":
            a, b, c, d = f["r"]
            v.amj_deck_rect(f.get("part", "deck"), a * dw, b * dh, c * dw, d * dh, h, f.get("radius", 1.5), f.get("inset", 0.08))
        elif t == "deckEllipse":
            c, r, a, b = f["e"]
            v.amj_on_deck(f.get("part", "deck"), [v.amj_px(x, y) for x, y in reversed(pd.amj_ellipse_px(c * dw, r * dh, a * dw, b * dh, egg=f.get("egg", 0.0)))], h, f.get("inset", 0.08))
        elif t == "deckPoly":
            v.amj_on_deck(f.get("part", "deck"), [v.amj_px(*D(x, y)) for x, y in f["p"]], h, f.get("inset", 0.08))
        elif t == "lidRect":
            a, b, c, d = f["r"]
            v.amj_lid_prism([L(a, d), L(c, d), L(c, b), L(a, b)], h, inset=f.get("inset", 0.05))
        elif t == "lidEllipse":
            c, r, a, b = f["e"]
            v.amj_lid_prism(pd.amj_ellipse_px(c * lp.w, r * lp.h, a * lp.w, b * lp.h, egg=f.get("egg", 0.0)), h)
        elif t == "lidPoly":
            v.amj_lid_prism([L(x, y) for x, y in f["p"]], h, inset=f.get("inset", 0.05))
        elif t == "hinge":
            for c0, c1 in f["cols"]:
                x0, x1 = (c0 - 0.5) * v.W, (c1 - 0.5) * v.W
                v.amj_cylinder(v.body, "hinge", v.m["hinge"], (x0 + x1) / 2, v.amj_hinge_y(), v.hR - f.get("drop", 0.15), f.get("radius", 2.4), abs(x1 - x0), 16)
        elif t == "rearPanel":
            amj_rear_panel(v, f)
        elif t == "foot":
            amj_foot(v, f)
        elif t == "keyGrid":
            amj_key_grid(v, f)
        elif t == "rearBlock":
            amj_rear_block(v, f)
        else:
            raise ValueError(t)
    missing = {k: n for k, n in v.c.get("entryPorts", {}).items() if n and not (k in v.ph and k in v.m)}
    if missing:
        amj_draw_ports(v, missing)


def amj_draw_ports(v, ports):
    """Puertos dibujados cuando no hay foto de ese lado: posición aproximada, lado y orden según la ficha."""
    port, tongue = v.k.amj_solid("Puerto", "#050506", 0.8, 0.05), v.k.amj_solid("Lengueta", "#3a3d42", 0.4, 0.3)
    M = v.body
    for side, names in ports.items():
        names = [n for n in names if n in core.PORT_SIZE]
        gap = 5.0
        total = sum(core.PORT_SIZE[n][0] for n in names) + gap * (len(names) - 1)
        if side == "rear":
            at = total / 2
            for n in names:
                w, h = core.PORT_SIZE[n]
                cx = at - w / 2
                y = v.D / 2 + 0.05
                zc, hz = v.hR * 0.5, min(h / 2, v.hR * 0.36)
                M.amj_poly("base", port, [(cx + w / 2, y, zc - hz), (cx - w / 2, y, zc - hz), (cx - w / 2, y, zc + hz), (cx + w / 2, y, zc + hz)])
                at -= w + gap
            continue
        sign = -1 if side == "left" else 1
        order = names if side == "left" else names[::-1]
        y = min(v.D / 2 - 18, total / 2 + v.D * 0.08)
        x = sign * (v.W / 2 + 0.05)
        for n in order:
            w, h = core.PORT_SIZE[n]
            cy = y - w / 2
            zc, hz = v.amj_h(cy) * 0.5, min(h / 2, v.amj_h(cy) * 0.36)
            q = [(x, cy - w / 2, zc - hz), (x, cy + w / 2, zc - hz), (x, cy + w / 2, zc + hz), (x, cy - w / 2, zc + hz)]
            M.amj_poly("base", port, q if sign > 0 else q[::-1])
            y -= w + gap


def amj_key_grid(v, f):
    """Teclado construido cuando la foto no muestra las teclas: distribución estandar dentro del rectángulo medido."""
    a, b, c, d = f["r"]
    x0, y1 = v.amj_px(a * v.deckP.w, b * v.deckP.h)
    x1, y0 = v.amj_px(c * v.deckP.w, d * v.deckP.h)
    numpad = f.get("numpad", False)
    units = 15.0 + (4.35 if numpad else 0.0)
    pw, ph = (x1 - x0) / units, (y1 - y0) / 5.62
    key = v.k.amj_solid("Tecla", f.get("color", "#17181b"), 0.55, 0.15)
    leg = v.k.amj_solid("Rotulos", f.get("legend", "#e8eaed"), 0.5, 0.1)
    b_txt = core.AMJBuilder("t")

    def place(cx, cy, w, h, label):
        v.amj_prism(v.body, "keys", key, v.m["keySide"], pd.amj_rounded_rect(cx, cy, w - 1.0, h - 1.0, 1.0), v.amj_deck_z, 0.8, lambda *_: (0, 0), 0.2)
        if label:
            ob = b_txt.amj_text("legend", leg, label, cx, cy, v.amj_deck_z(cx, cy) + 0.83, min(pw, ph) * (0.36 if len(label) == 1 else 0.2))
            for poly in ob.data.polygons:
                v.body.amj_poly("legend", leg, [tuple(ob.data.vertices[i].co) for i in poly.vertices])
            me = ob.data
            bpy.data.objects.remove(ob)
            bpy.data.meshes.remove(me)

    for r, row in enumerate(core.ROWS):
        height = ph * (0.62 if r == 0 else 1)
        y = y1 - (0 if r == 0 else ph * 0.62 + (r - 1) * ph) - height / 2
        x = x0
        scale = 15.0 / sum(w for _, w in row)
        for label, w in row:
            kw = w * scale * pw
            place(x + kw / 2, y, kw, height, label)
            x += kw
    if numpad:
        for r, row in enumerate(core.NUMPAD):
            y = y1 - ph * 0.62 - r * ph - ph / 2
            for i, label in enumerate(row):
                place(x0 + 15.35 * pw + i * pw + pw / 2, y, pw, ph, label)


def amj_rear_panel(v, f):
    p, s = v.ph["rear"], v.c["rearMap"]
    c0, c1 = s.get("cols", (0, p.w))
    x_of = lambda c: (0.5 - (c - c0) / (c1 - c0)) * v.W
    f0 = s.get("fromFrac", 0.0)
    z_of = lambda r: v.hR * (f0 + (1 - f0) * (s["bot"] - r) / (s["bot"] - s["top"]))
    a, b = [q * p.w for q in f["cols"]]
    r0, r1 = [q * p.h for q in f.get("rows", (s["top"] / p.h, s["bot"] / p.h))]
    x0, x1, z0, z1 = x_of(a), x_of(b), z_of(r1), z_of(r0)
    y, y2 = v.D / 2, v.D / 2 + f.get("d", 1.0)
    uv = lambda x, z: ((c0 + (0.5 - x / v.W) * (c1 - c0)) / p.w, 1 - (s["bot"] - (z / v.hR - f0) / (1 - f0) * (s["bot"] - s["top"])) / p.h)
    q = [(x0, y2, z0), (x1, y2, z0), (x1, y2, z1), (x0, y2, z1)]
    M = v.body
    M.amj_poly("base", v.m["rear"], q[::-1], [uv(q_[0], q_[2]) for q_ in q[::-1]])
    for i in range(4):
        A, B = q[i], q[(i + 1) % 4]
        M.amj_poly("base", v.m["wall"], [A, B, (B[0], y, B[2]), (A[0], y, A[2])])


def amj_rear_block(v, f):
    """Módulo que sobresale por detrás de la bisagra."""
    hw, y0, y1, z0, z1 = v.W * f.get("w", 0.96) / 2, v.D / 2 - 0.2, v.D / 2 + f["d"], f["z0"], f["z1"]
    mat = v.k.amj_solid("Modulo_trasero", f.get("color", "#141518"), 0.6, 0.15)
    box = [(-hw, y0), (hw, y0), (hw, y1), (-hw, y1)]
    M = v.body
    M.amj_poly("base", mat, [(x, y, z1) for x, y in box])
    M.amj_poly("base", mat, [(x, y, z0) for x, y in reversed(box)])
    for i in range(4):
        a, b = box[i], box[(i + 1) % 4]
        M.amj_poly("base", mat, [(*a, z0), (*b, z0), (*b, z1), (*a, z1)])


def amj_foot(v, f):
    """Pie de goma medido en la foto de la base (rectángulo en píxeles)."""
    c0, r0, c1, r1 = f["r"]
    xa, xb = (0.5 - c0) * v.W, (0.5 - c1) * v.W
    ya, yb = (0.5 - r0) * v.D, (0.5 - r1) * v.D
    poly = pd.amj_rounded_rect((xa + xb) / 2, (ya + yb) / 2, abs(xb - xa), abs(yb - ya), min(abs(xb - xa), abs(yb - ya)) * 0.45, 4)
    M, drop = v.body, f.get("h", 0.8)
    M.amj_poly("base", v.m["foot"], [(x, y, -drop) for x, y in reversed(poly)])
    for i in range(len(poly)):
        j = (i + 1) % len(poly)
        M.amj_poly("base", v.m["foot"], [(poly[i][0], poly[i][1], -drop), (poly[j][0], poly[j][1], -drop), (poly[j][0], poly[j][1], 0.02), (poly[i][0], poly[i][1], 0.02)])


# ------------------------------------------------------------------ configuración de un chasis

GENERIC_RINGS = [(1.3, 1.3, 1.3, 0.0), (0.35, 0.35, 0.35, 0.09), (0.0, 0.0, 0.0, 0.2), (0.0, 0.0, 0.0, 0.93), (0.1, 0.1, 0.1, 0.975), (0.35, 0.35, 0.35, 1.0)]


def amj_specs_of(i):
    import glob
    for path in glob.glob(os.path.join(core.SPECS, "*.json")):
        with open(path, encoding="utf-8") as f:
            for e in json.load(f):
                if e["id"] == i:
                    return e
    raise KeyError(i)


def amj_band(m, photo, frac):
    t, b = frac
    return {**m, "topRear": t * photo.h, "topFront": t * photo.h, "botRear": b * photo.h, "botFront": b * photo.h}


def amj_chassis(folder, spec_id, **o):
    """Crea la configuración de un chasis a partir de sus fotos y su ficha; las opciones sustituyen lo calculado."""

    def make():
        e = amj_specs_of(spec_id)
        sp, de = e["specs"], e.get("design", {})
        W, D = o.get("W", sp["widthMm"]), o.get("D", sp["depthMm"])
        T = o.get("T", de.get("thicknessMaxMm") or sp["thicknessMm"])
        Tf = o.get("Tf", de.get("thicknessMinMm") or T)
        lid_t = o["lidT"]
        names = o.get("files", {})
        found = {}
        base = os.path.join(pd.PHOTOS, folder.replace("/", os.sep))
        for key in ("deck", "lid", "screen", "bottom", "left", "right", "rear"):
            stem = names.get(key, key)
            for ext in ("", ".jpg", ".png"):
                if os.path.isfile(os.path.join(base, stem + ext)):
                    found[key] = f"{folder}/{stem}{ext}"
                    break
        # algunas imágenes de trasera y base eran reconstrucciones sin valor de referencia: no se proyectan
        for key in o.get("skip", ()):
            found.pop(key, None)
        for key, rel in o.get("borrow", {}).items():
            found[key] = rel
        if o.get("swapSides") and "left" in found and "right" in found:
            found["left"], found["right"] = found["right"], found["left"]
        c = {"W": W, "D": D, "hRear": T - lid_t, "hFront": max(Tf - lid_t, (T - lid_t) * 0.6), "lidT": lid_t,
             "rFront": o.get("rFront", 6.0), "rRear": o.get("rRear", 3.0), "rLidFree": o.get("rLidFree", 5.0), "rLidHinge": o.get("rLidHinge", 1.5),
             "rings": o.get("rings", GENERIC_RINGS), "photos": dict(found),
             "keySide": o.get("keySide", "#101114"), "wall": o["wall"], "under": o.get("under", o["wall"]), "hinge": o.get("hinge", "#17181b"),
             "lidEdge": o.get("lidEdge", o["wall"]), "extras": amj_apply_features, "features": o.get("features", [])}
        for k in ("deckSteps", "lidCols", "lidEdgePx", "lidBump", "photoGain", "photoGains", "sideMinFrac", "hingeInset"):
            if k in o:
                c[k] = o[k]
        for key, first in (("left", o.get("leftRearFirst", True)), ("right", o.get("rightRearFirst", False))):
            if key in found:
                ph = pd.AMJPhoto(found[key])
                m = amj_side_map(ph, first)
                if key + "Band" in o:
                    m = amj_band(m, ph, o[key + "Band"])
                c[key] = {**m, **o.get(key + "Map", {})}
        if "rear" in found:
            ph = pd.AMJPhoto(found["rear"])
            m = amj_rear_map(ph)
            if "rearBand" in o:
                m.update(top=o["rearBand"][0] * ph.h, bot=o["rearBand"][1] * ph.h)
            c["rearMap"] = {**m, **o.get("rearMap", {})}
        if "steps" in o:
            c["deckSteps"] = [(v * pd.AMJPhoto(found["deck"]).h, dz) for v, dz in o["steps"]]
        if "rearCols" in o and "rearMap" in c:
            w = pd.AMJPhoto(found["rear"]).w
            c["rearMap"]["cols"] = (o["rearCols"][0] * w, o["rearCols"][1] * w)
        keys = os.path.join(base, "keys.csv")
        if o.get("keys", True):
            if os.path.isfile(keys):
                c["keys"] = {"csv": f"{folder}/keys.csv", **o.get("keyOpts", {})}
            else:
                rows = amj_detect_keys(pd.AMJPhoto(found["deck"]), W)
                if rows:
                    c["keys"] = {"rows": rows, **o.get("keyOpts", {})}
        return c

    return make


# Los diseños se añaden aquí por marca a medida que se modelan.
CATALOG = {}


def amj_register(ids, cfg, features, ports=None):
    for i in ids:
        CATALOG[i] = {"cfg": cfg, "ports": ports, "features": features}
    pd.DESIGNS.update(CATALOG)

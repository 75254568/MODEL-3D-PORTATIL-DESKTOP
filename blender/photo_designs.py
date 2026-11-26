"""
Modelos 3D únicos construidos a partir de las fotos reales de cada laptop.

Cada diseño declara, medidos sobre sus fotos (en píxeles de la imagen): teclas, touchpad, relieves de la cubierta,
perfil lateral, panel trasero, tapa y piezas propias. La fotografía se proyecta sobre esa geometría, de modo que
puertos, rejillas, rótulos y logotipos quedan exactamente donde están en el equipo.

Convención: +Z arriba, -Y delante, milímetros. La tapa se modela cerrada con el origen en la bisagra (se extiende hacia -Y).
"""
import csv
import json
import math
import os
import re

import bmesh
import bpy
import numpy as np

import build_laptops as core

RES = os.path.join(core.ROOT, "src", "main", "resources")
PHOTOS = os.path.join(RES, "models")
BLENDS = os.path.join(core.ROOT, "blender", "modelos")


def amj_lerp(a, b, t):
    return a + (b - a) * t


class AMJPhoto:
    def __init__(self, rel, path=None, ref=None):
        self.rel = rel
        self.ref = ref or "@models/" + rel
        self.img = bpy.data.images.load(path or os.path.join(PHOTOS, rel.replace("/", os.sep)), check_existing=True)
        self.w, self.h = self.img.size
        self.file = os.path.basename(path or rel)

    def amj_uv(self, c, r):
        return c / self.w, 1 - r / self.h


class AMJKit:
    """Materiales del modelo y referencias a sus fotos para el MTL."""

    def __init__(self):
        self.mats = {}
        self.refs = {}

    def amj_solid(self, name, color, rough=0.55, spec=0.3):
        m = self.mats.get(name)
        if m is None:
            m = bpy.data.materials.new(name)
            m.use_nodes = True
            b = next(n for n in m.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
            rgb = core.amj_hex_rgb(color)
            b.inputs["Base Color"].default_value = (*rgb, 1)
            b.inputs["Roughness"].default_value = rough
            b.inputs["Metallic"].default_value = 0.0
            b.inputs["Specular IOR Level"].default_value = spec
            m.diffuse_color = (*rgb, 1)
            self.mats[name] = m
        return m

    def amj_photo(self, name, photo, rough=0.55, spec=0.18):
        m = self.mats.get(name)
        if m is None:
            m = bpy.data.materials.new(name)
            m.use_nodes = True
            t = m.node_tree
            b = next(n for n in t.nodes if n.type == "BSDF_PRINCIPLED")
            tex = t.nodes.new("ShaderNodeTexImage")
            tex.image = photo.img
            t.links.new(tex.outputs["Color"], b.inputs["Base Color"])
            b.inputs["Roughness"].default_value = rough
            b.inputs["Metallic"].default_value = 0.0
            b.inputs["Specular IOR Level"].default_value = spec
            self.mats[name] = m
            self.refs[photo.file] = photo.ref
        return m


class AMJMesher:
    """Acumula polígonos por pieza y material; al final crea un objeto por cada combinación."""

    def __init__(self):
        self.groups = {}

    def amj_poly(self, part, mat, pts, uvs=None):
        g = self.groups.setdefault((part, mat.name), (mat, [], [], []))
        i = len(g[1])
        g[1].extend(pts)
        g[2].append(list(range(i, i + len(pts))))
        g[3].extend(uvs or [(0.0, 0.0)] * len(pts))

    def amj_objects(self, collection):
        out = []
        for (part, _), (mat, verts, faces, uvs) in self.groups.items():
            me = bpy.data.meshes.new(part)
            me.from_pydata(verts, [], faces)
            layer = me.uv_layers.new(name="UVMap")
            layer.data.foreach_set("uv", [c for uv in uvs for c in uv])
            bm = bmesh.new()
            bm.from_mesh(me)
            bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=0.0005)
            bm.to_mesh(me)
            bm.free()
            me.materials.append(mat)
            me.polygons.foreach_set("use_smooth", [True] * len(me.polygons))
            me.set_sharp_from_angle(angle=math.radians(32))
            me.update()
            ob = bpy.data.objects.new(part, me)
            collection.objects.link(ob)
            out.append(ob)
        return out


def amj_rounded_rect(cx, cy, w, h, r, seg=3):
    r = min(r, w / 2, h / 2)
    pts = []
    for sx, sy, start in ((1, -1, -90), (1, 1, 0), (-1, 1, 90), (-1, -1, 180)):
        ox, oy = cx + sx * (w / 2 - r), cy + sy * (h / 2 - r)
        for i in range(seg + 1):
            a = math.radians(start + 90 * i / seg)
            pts.append((ox + r * math.cos(a), oy + r * math.sin(a)))
    return pts


def amj_ring_outline(c, s, f, r):
    w, d = c["W"] - 2 * s, c["D"] - f - r
    pts = core.amj_rounded_outline(w, d, max(0.4, c["rFront"] - min(s, f)), max(0.4, c["rRear"] - min(s, r)))
    return [(x, y + (f - r) / 2) for x, y in pts]


def amj_half_width(outline, y):
    best = 0.0
    n = len(outline)
    for i in range(n):
        (x0, y0), (x1, y1) = outline[i], outline[(i + 1) % n]
        lo, hi = min(y0, y1), max(y0, y1)
        if lo - 1e-6 <= y <= hi + 1e-6:
            if abs(y1 - y0) < 1e-9:
                best = max(best, abs(x0), abs(x1))
            else:
                best = max(best, abs(amj_lerp(x0, x1, (y - y0) / (y1 - y0))))
    return best


class AMJDesign:
    """Geometría comun a todos los diseños: cuerpo, cubierta, teclas y tapa proyectando sus fotos."""

    def __init__(self, cfg, kit):
        self.c = cfg
        self.k = kit
        self.W, self.D = cfg["W"], cfg["D"]
        self.hR, self.hF = cfg["hRear"], cfg["hFront"]
        self.body, self.lid = AMJMesher(), AMJMesher()
        self.ph = {k: AMJPhoto(*v) if isinstance(v, tuple) else AMJPhoto(v) for k, v in cfg["photos"].items()}
        self.deckP = self.ph["deck"]
        self.m = {
            "deck": kit.amj_photo("Foto_cubierta", self.deckP),
            "keySide": kit.amj_solid("Tecla_lado", cfg["keySide"], 0.6, 0.2),
            "wall": kit.amj_solid("Canto", cfg["wall"], 0.75, 0.08),
            "under": kit.amj_solid("Inferior", cfg["under"], 0.85, 0.04),
            "dark": kit.amj_solid("Hueco", "#07080a", 0.8, 0.1),
            "hinge": kit.amj_solid("Bisagra", cfg.get("hinge", "#17181b"), 0.4, 0.45),
            "lidEdge": kit.amj_solid("Canto_tapa", cfg.get("lidEdge", cfg["wall"]), 0.65, 0.12),
            "foot": kit.amj_solid("Goma", "#0d0e10", 0.9, 0.08),
        }
        for key, name in (("lid", "Foto_tapa"), ("screen", "Foto_pantalla"), ("bottom", "Foto_base"), ("left", "Foto_izquierda"), ("right", "Foto_derecha"), ("rear", "Foto_trasera")):
            if key in self.ph:
                self.m[key] = kit.amj_photo(name, self.ph[key], 0.3 if key == "screen" else 0.5, 0.12 if key == "screen" else 0.2)
        if "lid" not in self.m:
            self.m["lid"] = kit.amj_solid("Tapa", cfg.get("lidColor", cfg["wall"]), 0.45, 0.25)
        if "screen" not in self.m:
            self.m["screen"] = kit.amj_solid("Pantalla_generada", "#ffffff", 0.3, 0.1)
        self.profile = sorted(((0.5 - r / self.deckP.h) * self.D, dz) for r, dz in cfg.get("deckSteps", [(0, 0), (self.deckP.h, 0)]))

    # ---- coordenadas
    def amj_h(self, y):
        return self.hF + (self.hR - self.hF) * (y + self.D / 2) / self.D

    def amj_dz(self, y):
        p = self.profile
        if y <= p[0][0]:
            return p[0][1]
        for (y0, a), (y1, b) in zip(p, p[1:]):
            if y <= y1:
                return amj_lerp(a, b, (y - y0) / max(1e-6, y1 - y0))
        return p[-1][1]

    def amj_deck_z(self, x, y):
        return self.amj_h(y) + self.amj_dz(y)

    def amj_px(self, c, r):
        return (c / self.deckP.w - 0.5) * self.W, (0.5 - r / self.deckP.h) * self.D

    def amj_deck_uv(self, x, y):
        # los bordes de la foto pueden traer fondo: nunca se muestrea el último pixel
        return min(0.992, max(0.008, (x + self.W / 2) / self.W)), min(0.992, max(0.008, (y + self.D / 2) / self.D))

    def amj_lid_xy(self, c, r):
        p, edge = self.ph.get("lid", self.deckP), self.c.get("lidEdgePx", 0)
        c0, c1 = self.c.get("lidCols", (0, p.w))
        return (0.5 - (c - c0) / (c1 - c0)) * self.W, -self.L * (p.h - r) / (p.h - edge)

    def amj_lid_uv(self, x, y):
        p, edge = self.ph.get("lid", self.deckP), self.c.get("lidEdgePx", 0)
        c0, c1 = self.c.get("lidCols", (0, p.w))
        u = (c0 + (0.5 - x / self.W) * (c1 - c0)) / p.w
        return min(0.997, max(0.003, u)), min(0.997, max(0.003, 1 - (p.h - (-y / self.L) * (p.h - edge)) / p.h))

    def amj_screen_uv(self, x, y):
        p, edge = self.ph.get("screen", self.deckP), self.c.get("lidEdgePx", 0) if "screen" in self.ph else 0
        return min(0.997, max(0.003, x / self.W + 0.5)), min(0.997, max(0.003, 1 - (p.h - (-y / self.L) * (p.h - edge)) / p.h))

    def amj_wall_uv(self, side, x, y, frac):
        s = self.c[side]
        p = self.ph[side]
        t = (self.D / 2 - y) / self.D
        col = amj_lerp(s["rearPx"], s["frontPx"], t)
        top, bot = amj_lerp(s["topRear"], s["topFront"], t), amj_lerp(s["botRear"], s["botFront"], t)
        return col / p.w, 1 - amj_lerp(bot, top, frac) / p.h

    def amj_rear_uv(self, x, frac):
        s = self.c["rearMap"]
        p = self.ph["rear"]
        f0 = s.get("fromFrac", 0.0)
        c0, c1 = s.get("cols", (0, p.w))
        return (c0 + (0.5 - x / self.W) * (c1 - c0)) / p.w, 1 - amj_lerp(s["bot"], s["top"], max(0.0, (frac - f0) / (1 - f0))) / p.h

    # ---- cuerpo
    def amj_build_body(self):
        rings = [(amj_ring_outline(self.c, s, f, r), frac) for s, f, r, frac in self.c["rings"]]
        M = self.body
        for (a, fa), (b, fb) in zip(rings, rings[1:]):
            n = len(a)
            for i in range(n):
                j = (i + 1) % n
                ks = [(a[i], fa), (a[j], fa), (b[j], fb), (b[i], fb)]
                pts = [(p[0], p[1], fr * self.amj_h(p[1])) for p, fr in ks]
                ux, uy, uz = [pts[1][k] - pts[0][k] for k in range(3)]
                vx, vy, vz = [pts[3][k] - pts[0][k] for k in range(3)]
                nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
                ln = math.sqrt(nx * nx + ny * ny + nz * nz)
                if ln < 1e-9:
                    continue
                nx, ny, nz = nx / ln, ny / ln, nz / ln
                if nz < -0.7:
                    # chaflan inferior: el borde de la foto de la base trae un brillo que aquí se veria como una línea
                    M.amj_poly("base", self.m["under"], pts)
                elif max(fa, fb) <= self.c.get("sideMinFrac", -1):
                    M.amj_poly("base", self.m["under"], pts)
                elif abs(nx) >= abs(ny):
                    side = "left" if nx < 0 else "right"
                    if side in self.m and side in self.c:
                        M.amj_poly("base", self.m[side], pts, [self.amj_wall_uv(side, p[0], p[1], fr) for p, (_, fr) in zip(pts, ks)])
                    else:
                        M.amj_poly("base", self.m["wall"], pts)
                elif ny > 0 and "rear" in self.m and "rearMap" in self.c:
                    M.amj_poly("base", self.m["rear"], pts, [self.amj_rear_uv(p[0], fr) for p, (_, fr) in zip(pts, ks)])
                else:
                    M.amj_poly("base", self.m["wall"], pts)
        self.amj_bottom_face(M, [(x, y, 0.0) for x, y in reversed(rings[0][0])])
        self.amj_build_deck(rings[-1][0])

    def amj_bottom_face(self, M, pts):
        if "bottom" in self.m:
            M.amj_poly("base", self.m["bottom"], pts, [(min(0.988, max(0.012, 0.5 - p[0] / self.W)), min(0.988, max(0.012, (p[1] + self.D / 2) / self.D))) for p in pts])
        else:
            M.amj_poly("base", self.m["under"], pts)

    def amj_build_deck(self, outline):
        ys = sorted({round(y, 4) for _, y in outline} | {round(y, 4) for y, _ in self.profile if outline and min(p[1] for p in outline) < y < max(p[1] for p in outline)})
        M, mat = self.body, self.m["deck"]
        for y0, y1 in zip(ys, ys[1:]):
            h0, h1 = amj_half_width(outline, y0 + 1e-5), amj_half_width(outline, y1 - 1e-5)
            z0, z1 = self.amj_deck_z(0, y0), self.amj_deck_z(0, y1)
            pts = [(-h0, y0, z0), (h0, y0, z0), (h1, y1, z1), (-h1, y1, z1)]
            M.amj_poly("deck", mat, pts, [self.amj_deck_uv(p[0], p[1]) for p in pts])
            for s in (-1, 1):
                w0, w1 = self.amj_h(y0), self.amj_h(y1)
                if abs(w0 - z0) > 1e-3 or abs(w1 - z1) > 1e-3:
                    q = [(s * h0, y0, min(z0, w0)), (s * h1, y1, min(z1, w1)), (s * h1, y1, max(z1, w1)), (s * h0, y0, max(z0, w0))]
                    M.amj_poly("deck", self.m["wall"], q)

    def amj_prism(self, M, part, top, side, poly, z0, height, uv, inset=0.1):
        cx = sum(p[0] for p in poly) / len(poly)
        cy = sum(p[1] for p in poly) / len(poly)
        low = [(x, y, z0(x, y)) for x, y in poly]
        tp = []
        for x, y in poly:
            dx, dy = cx - x, cy - y
            ln = math.hypot(dx, dy) or 1
            k = min(inset / ln, 0.3)
            nx, ny = x + dx * k, y + dy * k
            tp.append((nx, ny, z0(nx, ny) + height))
        n = len(poly)
        for i in range(n):
            j = (i + 1) % n
            M.amj_poly(part, side, [low[i], low[j], tp[j], tp[i]])
        M.amj_poly(part, top, tp, [uv(p[0], p[1]) for p in tp])

    def amj_on_deck(self, part, poly, height, inset=0.1, side=None):
        self.amj_prism(self.body, part, self.m["deck"], side or self.m["keySide"], poly, self.amj_deck_z, height, self.amj_deck_uv, inset)

    def amj_deck_rect(self, part, c0, r0, c1, r1, height, radius=1.0, inset=0.1):
        x0, y0 = self.amj_px(c0, r0)
        x1, y1 = self.amj_px(c1, r1)
        self.amj_on_deck(part, amj_rounded_rect((x0 + x1) / 2, (y0 + y1) / 2, abs(x1 - x0), abs(y1 - y0), radius), height, inset)

    def amj_build_keys(self):
        k = self.c["keys"]
        p = self.deckP
        src = k.get("sourceSize", (p.w, p.h))
        if "rows" in k:
            rows = k["rows"]
        else:
            with open(os.path.join(PHOTOS, k["csv"].replace("/", os.sep)), newline="") as f:
                rows = [[float(v) for v in row[:4]] for row in csv.reader(f) if row]
        ax, bx, ay, by = k.get("fit", (0.0, 1.0, 0.0, 1.0))
        for cx, cy, w, h in rows:
            fx, fy = cx + 0.5, 1 - cy
            c, r = (ax + bx * fx) * p.w, (ay + by * fy) * p.h
            wp, hp = w * bx * p.w, h * by * p.h
            x, y = self.amj_px(c, r)
            sw, sh = wp / p.w * self.W, hp / p.h * self.D
            self.amj_on_deck("keys", amj_rounded_rect(x, y, sw - 0.15, sh - 0.15, k.get("radius", 1.1)), k.get("height", 0.75), 0.25)

    def amj_cylinder(self, M, part, mat, cx, cy, cz, r, length, seg=14):
        for i in range(seg):
            a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
            q = [(cx - length / 2, cy + r * math.cos(a0), cz + r * math.sin(a0)), (cx + length / 2, cy + r * math.cos(a0), cz + r * math.sin(a0)),
                 (cx + length / 2, cy + r * math.cos(a1), cz + r * math.sin(a1)), (cx - length / 2, cy + r * math.cos(a1), cz + r * math.sin(a1))]
            M.amj_poly(part, mat, q)
        for s in (-1, 1):
            ring = [(cx + s * length / 2, cy + r * math.cos(2 * math.pi * i / seg), cz + r * math.sin(2 * math.pi * i / seg)) for i in range(seg)]
            M.amj_poly(part, mat, ring if s > 0 else ring[::-1])

    # ---- tapa
    def amj_build_lid(self):
        c = self.c
        self.L = self.D - self.amj_hinge_inset() - 1.5
        base = [(x, y - self.L / 2) for x, y in core.amj_rounded_outline(self.W, self.L, c["rLidFree"], c["rLidHinge"])]
        outline = base + [self.amj_lid_xy(cc, r) for cc, r in c.get("lidBump", [])]
        t = c["lidT"]
        M = self.lid
        rings = [(core.amj_inset(outline, 0.45), 0.0), (outline, 0.45), (outline, t - 0.45), (core.amj_inset(outline, 0.35), t)]
        for (a, za), (b, zb) in zip(rings, rings[1:]):
            n = len(a)
            for i in range(n):
                j = (i + 1) % n
                M.amj_poly("lid", self.m["lidEdge"], [(*a[i], za), (*a[j], za), (*b[j], zb), (*b[i], zb)])
        top = [(x, y, t) for x, y in rings[-1][0]]
        M.amj_poly("lid", self.m["lid"], top, [self.amj_lid_uv(p[0], p[1]) for p in top])
        inner = [(x, y, -0.02) for x, y in reversed(rings[0][0])]
        M.amj_poly("display", self.m["screen"], inner, [self.amj_screen_uv(p[0], p[1]) for p in inner])

    def amj_lid_prism(self, poly_px, height, side=None, inset=0.08):
        poly = [self.amj_lid_xy(c, r) for c, r in poly_px]
        area = sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))
        if area < 0:
            poly.reverse()
        self.amj_prism(self.lid, "lid", self.m["lid"], side or self.m["lidEdge"], poly, lambda x, y: self.c["lidT"], height, self.amj_lid_uv, inset)

    def amj_hinge_inset(self):
        return self.c.get("hingeInset", 0.0)

    def amj_hinge_y(self):
        return self.D / 2 - self.amj_hinge_inset() - 1.5

    def amj_build(self):
        self.amj_build_body()
        self.amj_build_lid()
        if "keys" in self.c:
            self.amj_build_keys()
        self.c["extras"](self)


def amj_ellipse_px(cx, cy, rx, ry, seg=20, egg=0.0):
    pts = []
    for i in range(seg):
        a = 2 * math.pi * i / seg
        k = 1 + egg * math.sin(a)
        pts.append((cx + rx * k * math.cos(a), cy + ry * math.sin(a)))
    return pts


# ------------------------------------------------------------------ ASUS ROG Zephyrus G16

def amj_zephyrus_extras(v):
    c = v.c["zep"]
    for rect in c["topKeys"]:
        v.amj_deck_rect("keys", *rect, 0.6, 0.9, 0.2)
    hx, hy, hr = c["power"]
    pts = [v.amj_px(hx + hr * math.cos(math.radians(a)), hy - hr * 0.8 * math.sin(math.radians(a))) for a in range(0, 360, 60)]
    v.amj_on_deck("keys", pts, 0.28, 0.12)
    v.amj_deck_rect("deck", *c["pad"], 0.12, 0.8, 0.08)
    v.amj_lid_prism(c["slash"], 0.14)
    a, b, e, d = c["plate"]
    v.amj_lid_prism([(a, b), (e, b), (e, d), (a, d)], 0.05, inset=0.03)
    for c0, c1 in c["hinges"]:
        x0, _ = v.amj_px(c0, 0)
        x1, _ = v.amj_px(c1, 0)
        v.amj_cylinder(v.body, "hinge", v.m["hinge"], (x0 + x1) / 2, v.amj_hinge_y(), v.hR - 0.1, 1.7, abs(x1 - x0))
    # dos largas barras de goma, medidas en la foto de la base
    for y, w in ((-v.D * 0.439, 0.872), (v.D * 0.417, 0.894)):
        poly = amj_rounded_rect(0, y, v.W * w, 5.6, 1.6)
        M = v.body
        M.amj_poly("base", v.m["foot"], [(x, yy, -0.7) for x, yy in reversed(poly)])
        for i in range(len(poly)):
            j = (i + 1) % len(poly)
            M.amj_poly("base", v.m["foot"], [(poly[i][0], poly[i][1], -0.7), (poly[j][0], poly[j][1], -0.7), (poly[j][0], poly[j][1], 0.02), (poly[i][0], poly[i][1], 0.02)])


def amj_zephyrus(photo_dir, white):
    rel = lambda n: f"{photo_dir}/{n}"
    cfg = {
        "W": 354.0, "D": 246.0, "hRear": 12.9, "hFront": 10.4, "lidT": 4.5,
        "rFront": 7.0, "rRear": 5.0, "rLidFree": 7.0, "rLidHinge": 5.0,
        "rings": [(1.4, 1.4, 1.4, 0.0), (0.35, 0.35, 0.35, 0.09), (0.0, 0.0, 0.0, 0.22), (0.0, 0.0, 0.0, 0.93), (0.1, 0.1, 0.1, 0.975), (0.35, 0.35, 0.35, 1.0)],
        "extras": amj_zephyrus_extras,
    }
    if white:
        cfg.update({
            "photos": {"deck": "zep/deck.jpg", "lid": "zep/lid.jpg", "screen": "zep/screen.jpg"},
            "keySide": "#b9bbc2", "wall": "#d6d8de", "under": "#c8cad0", "hinge": "#b9bbc2", "photoGain": 0.76, "photoGains": {"Foto_cubierta": 0.56},
            "deckSteps": [(0, 0), (195, 0), (200, -0.45), (803, -0.45), (809, 0), (1396, 0)],
            "keys": {"csv": "gen/zep24/keys.csv", "fit": (0.0078, 0.9878, -0.0151, 1.0088)},
            "lightSides": ("#d6d8de", ["zep/left.png", "zep/right.png", "zep/bottom.png"]),
            "left": {"rearPx": 0, "frontPx": 2048, "topRear": 0, "topFront": 34, "botRear": 100, "botFront": 122},
            "right": {"rearPx": 2048, "frontPx": 0, "topRear": 0, "topFront": 34, "botRear": 100, "botFront": 122},
            "zep": {"topKeys": [(395, 128, 492, 180), (505, 128, 603, 180), (615, 128, 712, 180), (723, 128, 822, 180)], "power": (1646, 143, 48),
                    "pad": (576, 811, 1445, 1386), "slash": [(258, 190), (310, 190), (1792, 1228), (1740, 1232)], "plate": (90, 1284, 452, 1347),
                    "hinges": [(95, 185), (880, 935), (1115, 1165), (1865, 1955)]},
        })
    else:
        cfg.update({
            "photos": {"deck": rel("deck.jpg"), "lid": rel("lid.jpg"), "screen": rel("screen.jpg"), "bottom": rel("bottom.jpg"),
                       "left": rel("left.png"), "right": rel("right.png")},
            "keySide": "#17181b", "wall": "#3b3c40", "under": "#2c2d31",
            "deckSteps": [(0, 0), (215, 0), (220, -0.45), (833, -0.45), (839, 0), (1423, 0)],
            "keys": {"csv": "gen/zep24/keys.csv"},
            "left": {"rearPx": 0, "frontPx": 2048, "topRear": 0, "topFront": 0, "botRear": 89, "botFront": 89},
            "right": {"rearPx": 2048, "frontPx": 0, "topRear": 0, "topFront": 0, "botRear": 89, "botFront": 89},
            "zep": {"topKeys": [(390, 150, 487, 200), (500, 150, 598, 200), (610, 150, 710, 200), (721, 150, 820, 200)], "power": (1657, 165, 48),
                    "pad": (572, 841, 1452, 1422), "slash": [(246, 177), (298, 177), (1802, 1232), (1748, 1235)], "plate": (75, 1286, 444, 1350),
                    "hinges": [(95, 185), (880, 935), (1115, 1165), (1865, 1955)]},
        })
    return cfg


# ------------------------------------------------------------------ Alienware m16 R2

def amj_m16_extras(v):
    v.amj_on_deck("deck", [v.amj_px(c, r) for c, r in reversed(amj_ellipse_px(1025, 131, 30, 41, egg=0.18))], 0.25, 0.1)
    v.amj_deck_rect("deck", 618, 928, 1290, 1338, 0.12, 4.0, 0.08)
    v.amj_lid_prism(amj_ellipse_px(1020, 372, 52, 73, egg=0.18), 0.12)
    # lomo de bisagra de lado a lado y sus dos soportes, como en la foto de la tapa
    v.amj_cylinder(v.body, "hinge", v.m["hinge"], 0, v.amj_hinge_y(), v.hR - 0.2, 3.0, v.W * 0.9)
    for c0, c1 in ((170, 345), (1695, 1865)):
        x0, _ = v.amj_px(c0, 0)
        x1, _ = v.amj_px(c1, 0)
        v.amj_cylinder(v.body, "hinge", v.m["hinge"], (x0 + x1) / 2, v.amj_hinge_y(), v.hR - 0.2, 3.5, abs(x1 - x0), 18)
    # panel central de la trasera con los puertos, algo saliente respecto a las rejillas hexagonales
    p, s = v.ph["rear"], v.c["rearMap"]
    x0, x1 = (0.5 - (555 - s["cols"][0]) / (s["cols"][1] - s["cols"][0])) * v.W, (0.5 - (1490 - s["cols"][0]) / (s["cols"][1] - s["cols"][0])) * v.W
    f0 = s["fromFrac"]
    y, y2 = v.D / 2, v.D / 2 + 1.1
    z0, z1 = v.hR * (f0 + (1 - f0) * (1 - 50 / s["bot"])), v.hR * 0.995
    z2 = v.hR * f0
    M = v.body
    c0, c1 = s["cols"]
    uv = lambda x, z: ((c0 + (0.5 - x / v.W) * (c1 - c0)) / p.w, 1 - amj_lerp(s["bot"], s["top"], (z / v.hR - f0) / (1 - f0)) / p.h)
    q = [(x0, y2, z2), (x1, y2, z2), (x1, y2, z1), (x0, y2, z1)]
    M.amj_poly("base", v.m["rear"], q[::-1], [uv(a[0], a[2]) for a in q[::-1]])
    for a, b in ((q[0], q[3]), (q[1], q[2])):
        M.amj_poly("base", v.m["wall"], [a, b, (b[0], y, b[2]), (a[0], y, a[2])])
    M.amj_poly("base", v.m["wall"], [q[3], q[2], (q[2][0], y, q[2][2]), (q[3][0], y, q[3][2])])
    M.amj_poly("base", v.m["wall"], [q[0], q[1], (q[1][0], y, q[1][2]), (q[0][0], y, q[0][2])])


def amj_m16r2():
    d = "gen/m16r2"
    return {
        "W": 363.9, "D": 249.9, "hRear": 17.0, "hFront": 13.49, "lidT": 6.5,
        "rFront": 8.0, "rRear": 3.0, "rLidFree": 4.0, "rLidHinge": 1.2, "lidCols": (9, 2031),
        "photos": {"deck": d + "/deck.jpg", "lid": d + "/lid.jpg", "screen": d + "/screen.jpg", "left": d + "/left.png", "right": d + "/right.png", "rear": d + "/rear.png"},
        "keySide": "#101114", "wall": "#34373c", "under": "#191a1d", "hinge": "#141518", "lidEdge": "#2e3136",
        # la zona trasera y el reposamanos quedan por encima del teclado; debajo de la trasera hay un pie retranqueado
        "rings": [(3.0, 1.8, 15.0, 0.0), (0.6, 0.5, 15.0, 0.10), (0.0, 0.0, 15.0, 0.30), (0.0, 0.0, 0.0, 0.30), (0.0, 0.0, 0.0, 0.93), (0.12, 0.12, 0.12, 0.975), (0.4, 0.4, 0.4, 1.0)],
        "deckSteps": [(0, 0), (205, 0), (214, -0.7), (832, -0.7), (874, 0), (1403, 0)],
        "keys": {"csv": d + "/keys.csv", "height": 0.95, "radius": 1.3},
        "left": {"rearPx": 0, "frontPx": 2048, "topRear": 0, "topFront": 18, "botRear": 121, "botFront": 121},
        "right": {"rearPx": 2048, "frontPx": 0, "topRear": 0, "topFront": 18, "botRear": 121, "botFront": 121},
        "rearMap": {"top": 0, "bot": 57, "fromFrac": 0.30, "cols": (70, 1978)},
        "extras": amj_m16_extras,
    }


# ------------------------------------------------------------------ Lenovo ThinkPad X1 Carbon Gen 12

def amj_x1_extras(v):
    # bandeja del teclado: marco en relieve que sigue su contorno real, con el escalon de las flechas
    tray = [(92, 870), (1575, 870), (1575, 915), (1958, 915), (1958, 115), (92, 115)]
    outer = [v.amj_px(c, r) for c, r in tray]
    inner = [v.amj_px(c + dc * 9, r + dr * 9) for (c, r), (dc, dr) in zip(tray, [(1, -1), (1, -1), (1, -1), (-1, -1), (-1, 1), (1, 1)])]
    M = v.body
    for i in range(len(outer)):
        j = (i + 1) % len(outer)
        for a, b, ha, hb in ((outer, inner, 0.0, 0.32), (inner, inner, None, None)):
            if ha is None:
                continue
            q = [(*a[i], v.amj_deck_z(*a[i]) + ha), (*a[j], v.amj_deck_z(*a[j]) + ha), (*b[j], v.amj_deck_z(*b[j]) + hb), (*b[i], v.amj_deck_z(*b[i]) + hb)]
            M.amj_poly("deck", v.m["deck"], q, [v.amj_deck_uv(p[0], p[1]) for p in q])
    # botones fisicos y touchpad
    v.amj_deck_rect("deck", 562, 1000, 1352, 1366, 0.1, 1.6, 0.08)
    for poly in ([(565, 905), (880, 905), (893, 992), (565, 992)], [(1036, 905), (1350, 905), (1350, 992), (1022, 992)], [(880, 905), (1036, 905), (1022, 992), (893, 992)]):
        v.amj_on_deck("keys", [v.amj_px(c, r) for c, r in reversed(poly)], 0.22, 0.12)
    # TrackPoint rojo
    tx, ty = v.amj_px(956, 590)
    circle = [(tx + 3.9 * math.cos(2 * math.pi * i / 22), ty + 3.9 * math.sin(2 * math.pi * i / 22)) for i in range(22)]
    v.amj_on_deck("keys", circle, 1.25, 0.5, v.k.amj_solid("TrackPoint_lado", "#8f1a1e", 0.7, 0.1))
    # junta de la bisagra: en la foto se ve el fondo blanco a través de ella
    x0, y0 = v.amj_px(262, 46)
    x1, y1 = v.amj_px(1788, 61)
    z = v.amj_deck_z(0, (y0 + y1) / 2) + 0.03
    M.amj_poly("deck", v.m["dark"], [(x0, y1, z), (x1, y1, z), (x1, y0, z), (x0, y0, z)])
    v.amj_cylinder(M, "hinge", v.m["hinge"], 0, v.amj_hinge_y(), v.hR - 0.1, 2.0, abs(x1 - x0))
    # barra de la cámara: sobresale de la tapa y va en relieve
    v.amj_lid_prism([(c, r) for c, r in reversed(amj_rounded_rect(1025, 36, 620, 70, 30, 5))], 1.05, v.k.amj_solid("Barra_camara", "#3b3c3f", 0.25, 0.6), 0.22)
    # dos pies de goma de la foto de la base
    for c in (195, 1852):
        x = (0.5 - c / v.ph["bottom"].w) * v.W
        y = (0.5 - 1222 / v.ph["bottom"].h) * v.D
        poly = amj_rounded_rect(x, y, 18.5, 6.2, 3.0, 5)
        M.amj_poly("base", v.m["foot"], [(a, b, -0.65) for a, b in reversed(poly)])
        for i in range(len(poly)):
            j = (i + 1) % len(poly)
            M.amj_poly("base", v.m["foot"], [(poly[i][0], poly[i][1], -0.65), (poly[j][0], poly[j][1], -0.65), (poly[j][0], poly[j][1], 0.02), (poly[i][0], poly[i][1], 0.02)])


def amj_x1c12():
    d = "gen/x1c"
    return {
        "W": 312.8, "D": 214.75, "hRear": 10.56, "hFront": 10.56, "lidT": 4.4,
        "rFront": 4.5, "rRear": 2.0, "rLidFree": 2.5, "rLidHinge": 1.0, "lidEdgePx": 19, "sideMinFrac": 0.2,
        "lidBump": [(1369, 20), (1352, 16), (1335, 6), (1320, 0), (730, 0), (715, 6), (698, 16), (681, 20)],
        "photos": {"deck": d + "/deck.jpg", "lid": d + "/lid.jpg", "screen": d + "/screen.jpg", "bottom": d + "/bottom.jpg", "left": d + "/right.png", "right": d + "/left.png"},
        "keySide": "#0c0d0f", "wall": "#1d1e20", "under": "#17181a", "hinge": "#101113", "lidEdge": "#161719",
        # la base se estrecha con un chaflan ancho en la parte inferior, visible en su foto
        "rings": [(13.5, 12.5, 3.0, 0.0), (6.0, 5.5, 1.2, 0.20), (0.8, 0.8, 0.3, 0.46), (0.0, 0.0, 0.0, 0.62), (0.0, 0.0, 0.0, 0.95), (0.12, 0.12, 0.12, 0.985), (0.3, 0.3, 0.3, 1.0)],
        "keys": {"csv": d + "/keys.csv", "height": 0.8, "radius": 1.5},
        "left": {"rearPx": 0, "frontPx": 2048, "topRear": 0, "topFront": 0, "botRear": 85, "botFront": 85},
        "right": {"rearPx": 2048, "frontPx": 0, "topRear": 0, "topFront": 0, "botRear": 88, "botFront": 88},
        "extras": amj_x1_extras,
    }


DESIGNS = {
    32: {"cfg": lambda: amj_zephyrus("zep", True), "ports": {"left": ["power", "hdmi", "usbc", "usba", "audio"], "right": ["sd", "usba", "usbc"]},
         "features": ["cubierta con zona de teclado rebajada", "Slash en relieve", "placa ROG de la tapa", "teclas superiores y encendido hexagonal", "cuatro bisagras", "barras de goma inferiores"]},
    17: {"cfg": lambda: amj_zephyrus("gen/zep24", False), "ports": {"left": ["power", "hdmi", "usbc", "usba", "audio"], "right": ["sd", "usba", "usbc"]},
         "features": ["cubierta con zona de teclado rebajada", "Slash en relieve", "placa ROG de la tapa", "teclas superiores y encendido hexagonal", "cuatro bisagras", "barras de goma inferiores"]},
    31: {"cfg": amj_m16r2, "ports": {"left": ["rj45", "audio"], "right": ["microsd", "usba", "usba"], "rear": ["usbc", "usbc", "hdmi", "power"]},
         "features": ["zona térmica trasera elevada", "pie trasero retranqueado", "panel trasero de puertos saliente", "logotipos Alienware en relieve", "touchpad con su marco", "lomo de bisagra con dos soportes"]},
    24: {"cfg": amj_x1c12, "ports": {"left": ["usba", "usbc", "usbc"], "right": ["audio", "usba", "hdmi", "kensington"]},
         "features": ["barra de la cámara saliente", "bandeja del teclado con escalón de flechas", "TrackPoint", "tres botones físicos", "chaflán inferior ancho", "pies de goma"]},
}


def amj_lighten(rel, color, folder):
    """Versiones claras de las fotos laterales: la única foto disponible es de un equipo gris."""
    src = AMJPhoto(rel)
    w, h = src.w, src.h
    px = np.array(src.img.pixels[:], dtype=np.float32).reshape(h, w, 4)
    rgb, a = px[..., :3], px[..., 3:]
    lum = (rgb * [0.299, 0.587, 0.114]).sum(axis=2, keepdims=True)
    t = np.clip((lum - 0.035) / 0.07, 0, 1)
    t = t * t * (3 - 2 * t)
    base = np.array(core.amj_hex_rgb(color), dtype=np.float32)
    body = np.clip(base * (0.84 + 0.75 * (lum - 0.16)), 0, 1)
    out = rgb * (1 - t) + body * t
    out = out * a + base * (1 - a)
    name = os.path.splitext(os.path.basename(rel))[0] + "_claro.jpg"
    dest = os.path.join(folder, name)
    img = bpy.data.images.new(name, w, h)
    img.pixels.foreach_set(np.concatenate([out, np.ones((h, w, 1))], axis=2).astype(np.float32).ravel())
    img.filepath_raw = dest
    img.file_format = "JPEG"
    img.save(quality=90)
    bpy.data.images.remove(img)
    return rel, dest, name


def amj_assemble(entry, folder):
    spec = DESIGNS[entry["id"]]
    cfg = spec["cfg"]()
    cfg["entryPorts"] = {k: [n for n in (v or []) if n != "gap"] for k, v in (entry.get("design", {}).get("ports") or {}).items() if k in ("left", "right", "rear")}
    if "lightSides" in cfg:
        color, rels = cfg["lightSides"]
        for rel in rels:
            _, dest, name = amj_lighten(rel, color, folder)
            key = {"left.png": "left", "right.png": "right", "rear.jpg": "rear", "bottom.png": "bottom"}[os.path.basename(rel)]
            cfg["photos"][key] = (rel, dest, name)
    kit = AMJKit()
    design = cfg.get("builder", AMJDesign)(cfg, kit)
    design.amj_build()
    return design, kit, spec


def amj_build(entry):
    """Crea el modelo, lo exporta para la aplicación y guarda su archivo .blend. Devuelve el nombre de su carpeta."""
    folder = os.path.join(core.OUT, core.amj_slug(entry["brand"] + "-" + entry["model"]))
    os.makedirs(folder, exist_ok=True)
    design, kit, spec = amj_assemble(entry, folder)
    import spec_render
    if spec.get("basis") == "descripciones":
        spec_render.amj_bake_design(design, kit, folder)
    if "screen" not in design.ph:
        spec_render.amj_add_screen(design, kit, entry, folder)
    c = design.c
    scene = bpy.context.scene
    for name, mesher in (("body", design.body), ("lid", design.lid)):
        coll = bpy.data.collections.new(name)
        scene.collection.children.link(coll)
        obs = mesher.amj_objects(coll)
        bpy.ops.object.select_all(action="DESELECT")
        for ob in obs:
            ob.select_set(True)
        bpy.context.view_layer.objects.active = obs[0]
        bpy.ops.wm.obj_export(filepath=os.path.join(folder, name + ".obj"), export_selected_objects=True, forward_axis="Y", up_axis="Z",
                              export_uv=True, export_normals=True, export_materials=True, export_triangulated_mesh=True, path_mode="STRIP")
        mtl = os.path.join(folder, name + ".mtl")
        with open(mtl, encoding="utf-8") as f:
            text = f.read()
        text = re.sub(r"^map_Kd\s+(.*)$", lambda m: "map_Kd " + kit.refs.get(os.path.basename(m.group(1).strip()), os.path.basename(m.group(1).strip())), text, flags=re.M)
        with open(mtl, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    de = entry.get("design", {})
    ports = spec.get("ports") or c["entryPorts"]
    described = spec.get("basis") == "descripciones"
    if described:
        features = ["diseño propio modelado a partir de su ficha, reseñas y galerías consultadas, sin fotos proyectadas"] + spec["features"]
        assumed = ["posición y tamaño de teclado, rejillas y rótulos, estimados a partir de esas fuentes", "radios de las esquinas y grosor de la tapa, estimados", "la pantalla muestra un fondo generado, no el del fabricante"] + spec.get("approx", []) + design.notes
        pending = ["el proyecto no contiene fotos de este equipo: el modelo no reproduce texturas reales"] + [n for n in de.get("notVerified", []) if not any(w in n for w in spec.get("resolved", ()))]
    else:
        features = ["diseño propio modelado sobre fotos reales del equipo"] + (["teclas colocadas según su foto"] if "keys" in c else []) \
            + (["puertos y rejillas laterales proyectados desde sus fotos"] if "left" in design.ph or "right" in design.ph else []) + spec["features"]
        assumed = ["altura exacta de los relieves, estimada", "radios de las esquinas medidos en foto", "grosor de la tapa"]
        pending = [f"sin foto fiable de: {n}" for k, n in (("left", "lado izquierdo"), ("right", "lado derecho"), ("rear", "trasera"), ("bottom", "base"), ("lid", "tapa"), ("screen", "pantalla")) if k not in design.ph or (k == "screen" and getattr(design, "syntheticScreen", False))] \
            + [n for n in de.get("notVerified", []) if not any(w in n for w in ("logotipo", "rejillas", "touchpad"))]
    s0, f0, r0, _ = c["rings"][0]
    meta = {
        "brand": entry["brand"], "model": entry["model"], "widthMm": c["W"], "depthMm": c["D"], "thicknessMm": round(c["hRear"] + c["lidT"], 3),
        "thicknessFrontMm": round(c["hFront"] + c["lidT"], 3), "bodyRearMm": c["hRear"], "bodyFrontMm": c["hFront"], "lidThicknessMm": c["lidT"],
        "lidLengthMm": round(design.L, 3), "hingeYMm": round(design.amj_hinge_y(), 3), "hingeZMm": c["hRear"], "maxLidDeg": de.get("hingeMaxDeg"),
        "innerFit": {"scaleX": round((c["W"] - 2 * s0 - 2) / c["W"], 4), "scaleZ": round((c["D"] - f0 - r0 - 2) / c["D"], 4), "offsetZ": round((f0 - r0) / 2, 3)},
        "outlineFromFrac": next((fr for s, f, r, fr in c["rings"] if s == f == r == 0.0), 0.0),
        "cornerFrontMm": c["rFront"], "cornerRearMm": c["rRear"], "cornerLidFreeMm": c["rLidFree"], "cornerLidHingeMm": c["rLidHinge"],
        "photoTextured": True, "photoGain": c.get("photoGain", 1.0), "photoGains": c.get("photoGains", {}), "panel": {},
        "portsPlaced": [{"side": s, "type": p} for s, names in ports.items() for p in names], "portsNotPlaced": de.get("portsUnplaced", []) if not spec.get("ports") else [],
        "generatedScreen": "screen" not in design.ph,
        "modelBasis": "descripciones" if described else "fotos",
        "verifiedFeatures": features, "assumed": assumed, "notVerified": pending, "sources": entry.get("sources", []) + [u for u in spec.get("sources", []) if u not in entry.get("sources", [])],
    }
    with open(os.path.join(folder, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=1)
    os.makedirs(BLENDS, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(BLENDS, os.path.basename(folder) + ".blend"), copy=True, relative_remap=True)
    return os.path.basename(folder)


def amj_entries():
    import glob
    found = {}
    for path in glob.glob(os.path.join(core.SPECS, "*.json")):
        with open(path, encoding="utf-8") as f:
            for e in json.load(f):
                if e["id"] in DESIGNS:
                    found[e["id"]] = e
    return found


def amj_preview(ids, spacing=430.0, open_deg=112.0):
    """Muestra los modelos, con la tapa abierta, en la ventana de Blender."""
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob)
    for coll in list(bpy.data.collections):
        bpy.data.collections.remove(coll)
    all_e = amj_entries()
    tmp = bpy.app.tempdir or os.environ.get("TEMP", ".")
    for n, i in enumerate(ids):
        e = all_e[i]
        coll = bpy.data.collections.new(e["model"][:40])
        bpy.context.scene.collection.children.link(coll)
        design, _, _ = amj_assemble(e, tmp)
        root = bpy.data.objects.new(e["model"][:30], None)
        coll.objects.link(root)
        root.location = ((n - (len(ids) - 1) / 2) * spacing, 0, 0)
        for ob in design.body.amj_objects(coll):
            ob.parent = root
        pivot = bpy.data.objects.new("bisagra", None)
        coll.objects.link(pivot)
        pivot.parent = root
        pivot.location = (0, design.amj_hinge_y(), design.c["hRear"])
        pivot.rotation_euler = (-math.radians(open_deg), 0, 0)
        for ob in design.lid.amj_objects(coll):
            ob.parent = pivot
    bpy.context.scene.render.engine = "BLENDER_EEVEE"
    wm = bpy.context.window_manager
    for win in wm.windows:
        for area in win.screen.areas:
            if area.type == "VIEW_3D":
                space = area.spaces.active
                space.shading.type = "MATERIAL"
                space.clip_end = 20000
                space.overlay.show_floor = False
                region = next(r for r in area.regions if r.type == "WINDOW")
                with bpy.context.temp_override(window=win, area=area, region=region):
                    bpy.ops.view3d.view_all()

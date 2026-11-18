"""
Disenos propios de los laptops que no tienen fotos dentro del proyecto.

No se proyecta ninguna imagen: cada equipo se construye con geometria y colores a partir de su ficha verificada y de lo
que describen fabricantes y resenas (medidas, teclado, touchpad, puertos, bisagra, rejillas, logotipos). Lo que esas
fuentes no fijan con exactitud (posicion milimetrica de piezas, forma exacta de un logotipo) se estima y queda anotado
en el meta.json de cada modelo.

Convencion igual que photo_designs: +Z arriba, -Y delante, milimetros; tapa cerrada con origen en la bisagra.
"""
import math

import bpy

import build_laptops as core
import photo_catalog as pc
import photo_designs as pd

UV0 = lambda *_: (0.0, 0.0)

# perfiles del canto: (retranqueo lateral, frontal, trasero, fraccion de altura)
R_SLAB = [(1.1, 1.1, 1.1, 0.0), (0.25, 0.25, 0.25, 0.1), (0.0, 0.0, 0.0, 0.2), (0.0, 0.0, 0.0, 0.93), (0.08, 0.08, 0.08, 0.975), (0.25, 0.25, 0.25, 1.0)]
R_SHARP = [(0.7, 0.7, 0.7, 0.0), (0.1, 0.1, 0.1, 0.08), (0.0, 0.0, 0.0, 0.14), (0.0, 0.0, 0.0, 0.96), (0.18, 0.18, 0.18, 1.0)]
R_SOFT = [(2.6, 2.6, 2.2, 0.0), (1.2, 1.2, 1.0, 0.1), (0.35, 0.35, 0.3, 0.26), (0.0, 0.0, 0.0, 0.46), (0.0, 0.0, 0.0, 0.86), (0.18, 0.18, 0.18, 0.95), (0.55, 0.55, 0.55, 1.0)]
R_GAMER = [(4.5, 3.5, 2.0, 0.0), (2.2, 1.6, 1.0, 0.12), (0.6, 0.5, 0.3, 0.3), (0.0, 0.0, 0.0, 0.42), (0.0, 0.0, 0.0, 0.94), (0.12, 0.12, 0.12, 0.98), (0.35, 0.35, 0.35, 1.0)]
R_WEDGE = [(9.0, 8.0, 2.0, 0.0), (4.0, 3.5, 1.0, 0.22), (0.6, 0.6, 0.3, 0.5), (0.0, 0.0, 0.0, 0.64), (0.0, 0.0, 0.0, 0.95), (0.1, 0.1, 0.1, 0.985), (0.3, 0.3, 0.3, 1.0)]

RAINBOW = ["#ff4d5e", "#ff9a3d", "#ffe14d", "#58e36b", "#3fd8e8", "#4f8dff", "#b16bff"]


def ccw(poly):
    n = len(poly)
    area = sum(poly[i][0] * poly[(i + 1) % n][1] - poly[(i + 1) % n][0] * poly[i][1] for i in range(n))
    return list(poly) if area > 0 else list(poly)[::-1]


def circle(x, y, r, n=16):
    return [(x + r * math.cos(2 * math.pi * i / n), y + r * math.sin(2 * math.pi * i / n)) for i in range(n)]


def tone(color, k):
    r, g, b = [min(1.0, c * k) for c in core.hex_rgb(color)]
    return "#%02x%02x%02x" % (round(r * 255), round(g * 255), round(b * 255))


class Blank:
    """Ocupa el lugar de la foto de la cubierta: un pixel equivale a un milimetro."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.file, self.ref = "", ""


class KB:
    pass


class SpecDesign(pd.Design):
    def __init__(self, cfg, kit):
        self.c, self.k = cfg, kit
        self.W, self.D = cfg["W"], cfg["D"]
        self.hR, self.hF = cfg["hRear"], cfg["hFront"]
        self.body, self.lid = pd.Mesher(), pd.Mesher()
        self.ph = {}
        self.deckP = Blank(self.W, self.D)
        self.notes = []
        self.tb = core.Builder("t")
        finish = cfg.get("finish", {})
        rough, spec = finish.get("rough", 0.46), finish.get("spec", 0.3)
        self.m = {
            "deck": kit.solid("Cubierta", cfg["deck"], rough, spec),
            "keySide": kit.solid("Tecla_lado", cfg["keySide"], 0.6, 0.15),
            "wall": kit.solid("Canto_liso", cfg["wall"], rough * 0.9, spec * 1.2),
            "under": kit.solid("Base", cfg["under"], 0.62, 0.15),
            "dark": kit.solid("Hueco", "#07080a", 0.8, 0.08),
            "hinge": kit.solid("Bisagra", cfg["hinge"], 0.38, 0.45),
            "lidEdge": kit.solid("Canto_tapa_liso", cfg["lidEdge"], rough * 0.8, spec * 1.3),
            "foot": kit.solid("Goma", "#0d0e10", 0.9, 0.06),
            "lid": kit.solid("Tapa_lisa", cfg["lid"], finish.get("lidRough", rough), finish.get("lidSpec", spec)),
            "screen": kit.solid("Pantalla_generada", "#ffffff", 0.3, 0.1),
        }
        self.profile = sorted((self.D / 2 - r, dz) for r, dz in cfg.get("deckSteps", [(0.0, 0.0), (self.D, 0.0)]))
        zero = [fr for s, f, r, fr in cfg["rings"] if s == f == r == 0.0]
        self.band = (zero[0], zero[-1])

    # ---- referencias
    def Y(self, r):
        """Y a r mm del borde trasero."""
        return self.D / 2 - r

    def F(self, f):
        """Y a f mm del borde delantero."""
        return -self.D / 2 + f

    def mat(self, name, color, rough=0.5, spec=0.2):
        return self.k.solid(name, color, rough, spec)

    # ---- superficies
    def flat(self, poly, mat, lift=0.03, part="deck"):
        self.body.poly(part, mat, [(x, y, self.deck_z(x, y) + lift) for x, y in ccw(poly)])

    def raised(self, poly, top, height=0.2, side=None, part="deck", inset=0.08):
        self.prism(self.body, part, top, side or self.m["keySide"], ccw(poly), self.deck_z, height, UV0, inset)

    def under(self, poly, mat, z=-0.03):
        self.body.poly("base", mat, [(x, y, z) for x, y in reversed(ccw(poly))])

    def ring(self, poly, width, mat, lift=0.04, put=None):
        put = put or (lambda q, m: self.flat(q, m, lift))
        outer = ccw(poly)
        inner = core.inset(outer, width)
        n = len(outer)
        for i in range(n):
            j = (i + 1) % n
            put([outer[i], outer[j], inner[j], inner[i]], mat)

    def dots(self, x0, y0, x1, y1, pitch, r, mat, put=None, n=6, keep=None):
        put = put or (lambda q, m: self.flat(q, m, 0.04))
        row = 0
        y = y0 + pitch / 2
        while y < y1:
            x = x0 + pitch / 2 + (pitch / 2 if row % 2 else 0)
            while x < x1 - pitch / 4:
                if keep is None or keep(x, y):
                    put(circle(x, y, r, n), mat)
                x += pitch
            y += pitch * 0.866
            row += 1

    def slots(self, x0, y0, x1, y1, count, mat, across=True, fill=0.55, radius=0.3, put=None):
        """Ranuras paralelas; across=True: ranuras a lo largo de X, apiladas en Y."""
        put = put or (lambda q, m: self.flat(q, m, 0.04))
        for i in range(count):
            t0 = i / count
            step = 1 / count
            if across:
                h = (y1 - y0) * step
                put(pd.rounded_rect((x0 + x1) / 2, y0 + (t0 + step / 2) * (y1 - y0), x1 - x0, h * fill, radius, 2), mat)
            else:
                w = (x1 - x0) * step
                put(pd.rounded_rect(x0 + (t0 + step / 2) * (x1 - x0), (y0 + y1) / 2, w * fill, y1 - y0, radius, 2), mat)

    def stamp(self, M, part, mat, s, x, y, size, zf, rot=0.0, align="CENTER"):
        if not s.strip():
            return
        ob = self.tb.text(part, mat, s, x, y, 0.0, size, rot, align)
        me = ob.data
        for p in me.polygons:
            M.poly(part, mat, [(a, b, zf(a, b)) for a, b in (tuple(me.vertices[i].co)[:2] for i in p.vertices)])
        self.tb.objects.clear()
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)

    def deck_text(self, s, x, y, size, mat, rot=0.0, lift=0.05, align="CENTER"):
        self.stamp(self.body, "deck", mat, s, x, y, size, lambda a, b: self.deck_z(a, b) + lift, rot, align)

    # ---- tapa: u a la derecha de quien la mira por fuera con el equipo abierto, w desde la bisagra hasta el borde libre
    def lp(self, u, w):
        return -u * self.W, -w * self.L

    def lmap(self, u, w, scale=1.0, rot=0.0):
        cx, cy = self.lp(u, w)
        c, s = math.cos(rot + math.pi), math.sin(rot + math.pi)
        return lambda px, py: (cx + (px * c - py * s) * scale, cy + (px * s + py * c) * scale)

    def lid_flat(self, poly, mat, lift=0.03):
        z = self.c["lidT"] + lift
        self.lid.poly("lid", mat, [(x, y, z) for x, y in ccw(poly)])

    def lid_raised(self, poly, top, height=0.12, side=None, inset=0.06):
        z = self.c["lidT"]
        self.prism(self.lid, "lid", top, side or self.m["lidEdge"], ccw(poly), lambda a, b: z, height, UV0, inset)

    def lid_text(self, s, u, w, size, mat, rot=0.0, lift=0.04, align="CENTER"):
        x, y = self.lp(u, w)
        z = self.c["lidT"] + lift
        self.stamp(self.lid, "lid", mat, s, x, y, size, lambda a, b: z, rot + math.pi, align)

    # ---- teclado
    def keyboard(self, top, pitch=None, numpad=0, np_scale=1.0, layout="std", fn=0.62, gap=1.4, radius=1.0, height=0.7, key="#17181b",
                 legend="#e8eaed", tray=None, tray_pad=2.0, rim=None, rgb=None, x=0.0, trackpoint=False, copilot=False, margin=None,
                 legend_scale=1.0, np_top=True):
        extra = 0.35 + numpad * np_scale if numpad else 0.0
        units = 15.0 + extra
        if pitch is None:
            pitch = min(19.05, (self.W - (margin if margin is not None else (18 if numpad else 38))) / units)
        left = x - units * pitch / 2
        y_top = self.Y(top)
        M = self.body
        mk = self.mat("Teclas", key, 0.55, 0.14)
        side = self.m["keySide"]
        mats = [self.mat(f"Luz_tecla_{i}", c, 0.4, 0.1) for i, c in enumerate(rgb)] if rgb else None
        mleg = self.mat("Rotulos", legend, 0.5, 0.08) if legend else None
        total = units * pitch

        def zk(a, b):
            return self.deck_z(a, b) + height + 0.025

        def place(cx, cy, w, h, label):
            self.prism(M, "keys", mk, side, pd.rounded_rect(cx, cy, max(0.8, w - gap), max(0.8, h - gap), radius, 2), self.deck_z, height, UV0, min(0.2, gap * 0.25 + 0.05))
            if label and (mleg or mats):
                m = mats[min(len(mats) - 1, int((cx - left) / total * len(mats)))] if mats else mleg
                size = pitch * legend_scale * (0.34 if len(label) == 1 else 0.19)
                size = min(size, (w - gap) * 1.45 / max(1, len(label)))
                self.stamp(M, "legend", m, label, cx, cy, size, zk)

        rows = [list(r) for r in (core.ROWS_APPLE if layout == "apple" else core.ROWS)]
        if layout in ("half", "thinkpad"):
            rows[4] = [("Shift", 2.25)] + [(c, 1) for c in "ZXCVBNM,./"] + [("Shift", 2.75)]
            rows[5] = [("Ctrl", 1.25), ("Fn", 1), ("Win", 1.25), ("Alt", 1.25), ("", 5.25), ("Alt", 1), ("Ctrl", 1), ("@", 3)]
        if layout == "thinkpad":
            rows[0] = [("Esc", 1)] + [(f"F{i}", 1) for i in range(1, 13)] + [("Home", 1), ("End", 1)]
            rows[5] = [("Fn", 1), ("Ctrl", 1.25), ("Win", 1), ("Alt", 1.25), ("", 4.75), ("Alt", 1), ("PrtSc", 1), ("Ctrl", 0.75), ("@", 3)]
        if copilot:
            last = rows[5]
            idx = max(i for i, (lab, _) in enumerate(last) if lab == "Ctrl")
            last[idx] = ("Cop", last[idx][1])
        for r, row in enumerate(rows):
            height_r = pitch * (fn if r == 0 else 1.0)
            cy = y_top - (0 if r == 0 else pitch * fn + (r - 1) * pitch) - height_r / 2
            cur = left
            scale = 15.0 / sum(w for _, w in row)
            for label, w in row:
                kw = w * scale * pitch
                if label == "@":
                    # flechas: izquierda y derecha completas, arriba y abajo de media altura
                    unit = kw / 3
                    place(cur + unit / 2, cy, unit, height_r, "<")
                    place(cur + unit * 1.5, cy + height_r / 4, unit, height_r / 2, "^")
                    place(cur + unit * 1.5, cy - height_r / 4, unit, height_r / 2, "v")
                    place(cur + unit * 2.5, cy, unit, height_r, ">")
                else:
                    place(cur + kw / 2, cy, kw, height_r, label)
                cur += kw
        kb = KB()
        kb.p, kb.left, kb.right, kb.top = pitch, left, left + 15 * pitch, y_top
        kb.bot = y_top - pitch * (fn + 5)
        kb.cx = left + 7.5 * pitch
        kb.end = left + total
        kb.fn = fn
        if numpad:
            nw = pitch * np_scale
            nx = left + 15.35 * pitch
            y0 = y_top - pitch * fn
            layout4 = [(0, 0, 1, 1, "Num"), (1, 0, 1, 1, "/"), (2, 0, 1, 1, "*"), (3, 0, 1, 1, "-"), (0, 1, 1, 1, "7"), (1, 1, 1, 1, "8"), (2, 1, 1, 1, "9"), (3, 1, 1, 2, "+"),
                       (0, 2, 1, 1, "4"), (1, 2, 1, 1, "5"), (2, 2, 1, 1, "6"), (0, 3, 1, 1, "1"), (1, 3, 1, 1, "2"), (2, 3, 1, 1, "3"), (3, 3, 1, 2, "Ent"), (0, 4, 2, 1, "0"), (2, 4, 1, 1, ".")]
            layout3 = [(0, 0, 1, 1, "/"), (1, 0, 1, 1, "*"), (2, 0, 1, 1, "-"), (0, 1, 1, 1, "7"), (1, 1, 1, 1, "8"), (2, 1, 1, 1, "9"), (0, 2, 1, 1, "4"), (1, 2, 1, 1, "5"), (2, 2, 1, 1, "6"),
                       (0, 3, 1, 1, "1"), (1, 3, 1, 1, "2"), (2, 3, 1, 1, "3"), (0, 4, 1, 1, "0"), (1, 4, 1, 1, "."), (2, 4, 1, 1, "Ent")]
            for c, r, w, h, label in (layout4 if numpad == 4 else layout3):
                place(nx + (c + w / 2) * nw, y0 - (r + h / 2) * pitch, w * nw, h * pitch, label)
            if np_top:
                for c in range(numpad):
                    place(nx + (c + 0.5) * nw, y_top - pitch * fn / 2, nw, pitch * fn, "")
        if tray or rim:
            cx, w = (left + kb.end) / 2, total + 2 * tray_pad
            cy, h = (kb.top + kb.bot) / 2, kb.top - kb.bot + 2 * tray_pad
            poly = pd.rounded_rect(cx, cy, w, h, 1.6, 3)
            if tray:
                self.flat(poly, self.mat("Bandeja", tray, 0.7, 0.06), 0.02)
            if rim:
                self.ring(poly, 0.7, self.mat("Marco_teclado", rim, 0.4, 0.3), 0.05)
        if trackpoint:
            tx, ty = left + 6.75 * pitch, y_top - pitch * fn - 3 * pitch
            red = self.mat("TrackPoint", "#c3161c", 0.72, 0.05)
            self.prism(M, "keys", red, self.mat("TrackPoint_lado", "#7f1216", 0.7, 0.05), circle(tx, ty, 2.5, 18), self.deck_z, height + 0.65, UV0, 0.25)
        return kb

    def power(self, x, y, w, h, color, radius=1.0, height=0.45, legend=None):
        top = self.mat("Tecla_encendido", color, 0.42, 0.22)
        self.prism(self.body, "keys", top, self.m["keySide"], pd.rounded_rect(x, y, w, h, radius, 3), self.deck_z, height, UV0, 0.12)
        if legend:
            self.flat(circle(x, y, min(w, h) * 0.2, 12), self.mat("Luz_encendido", legend, 0.4, 0.1), height + 0.04, "keys")

    # ---- touchpad
    def touchpad(self, w, d, kb, x=0.0, front=8.0, color=None, edge=None, style="inset", buttons=0, stripes=None, lit=None, radius=2.0):
        btn = 8.5 if buttons else 0.0
        room = kb.bot - self.F(front) - 2.6 - btn
        if d > room:
            self.notes.append(f"el touchpad publicado mide {d:g} mm de fondo; se dibuja de {room:.0f} mm para que quepa bajo el teclado de este modelo")
            d = room
        cy = self.F(front) + d / 2
        poly = pd.rounded_rect(x, cy, w, d, radius, 4)
        light = max(core.hex_rgb(self.c["deck"])) > 0.42
        color = color or tone(self.c["deck"], 0.9 if light else 1.22)
        edge = edge or tone(self.c["deck"], 0.62)
        if style == "etched":
            self.ring(poly, 0.55, self.mat("Grabado_touchpad", edge, 0.5, 0.2), 0.03)
        else:
            self.flat(poly, self.mat("Touchpad", color, 0.32, 0.3), 0.04)
            self.ring(poly, 0.7 if style == "inset" else 0.5, self.mat("Borde_touchpad", edge, 0.5, 0.2), 0.06)
        if lit:
            self.ring(pd.rounded_rect(x, cy, w + 3.4, d + 3.4, radius + 1.4, 4), 1.0, self.mat("Luz_touchpad", lit, 0.4, 0.1), 0.05)
        if buttons:
            y = cy + d / 2 + btn / 2 + 0.4
            mk = self.mat("Teclas", self.c.get("keyColor", "#17181b"), 0.55, 0.14)
            for a, b in ((x - w / 2, x - 13), (x - 13, x + 13), (x + 13, x + w / 2)):
                self.prism(self.body, "keys", mk, self.m["keySide"], pd.rounded_rect((a + b) / 2, y, b - a - 0.7, btn - 1.0, 0.9, 2), self.deck_z, 0.35, UV0, 0.1)
            if stripes:
                red = self.mat("Linea_botones", stripes, 0.5, 0.1)
                for sx in (x - 13, x + 13):
                    a, b = (sx - 0.5, sx - 0.15) if sx < x else (sx + 0.15, sx + 0.5)
                    self.flat([(a, y - 3.4), (b, y - 3.4), (b, y + 3.4), (a, y + 3.4)], red, 0.4, "keys")
        return cy, d

    def side_grilles(self, kb, inset=5.5, pitch=2.1, r=0.42, color=None, y0=None, y1=None, gap=4.0):
        mat = self.mat("Rejilla", color or "#090a0c", 0.8, 0.05)
        y0 = kb.bot + 1 if y0 is None else y0
        y1 = kb.top - 1 if y1 is None else y1
        hw = self.W / 2 - inset
        for a, b in ((-hw, kb.left - gap), (kb.end + gap, hw)):
            if b - a > 4:
                self.dots(a, y0, b, y1, pitch, r, mat)

    # ---- trasera y bisagras
    def shelf(self, color=None, top=None, gap=0.5):
        """Bloque trasero por detras de la bisagra, hasta la altura indicada (por defecto, el grosor total)."""
        depth = self.hinge_inset() + 1.5 - self.c["lidT"] - gap
        z_top = top if top is not None else self.hR + self.c["lidT"]
        y0, hw, hd, r = self.Y(depth), self.W / 2 - 0.05, self.D / 2 - 0.05, self.c["rRear"]
        pts = [(-hw, y0), (hw, y0)]
        for cx, cy, a0 in ((hw - r, hd - r, 0), (-hw + r, hd - r, 90)):
            pts += [(cx + r * math.cos(math.radians(a0 + 90 * i / 6)), cy + r * math.sin(math.radians(a0 + 90 * i / 6))) for i in range(7)]
        M, mat = self.body, self.mat("Bloque_trasero", color or self.c["wall"], 0.5, 0.2)
        top_pts = core.inset(pts, 0.5)
        n = len(pts)
        for i in range(n):
            j = (i + 1) % n
            M.poly("deck", mat, [(*pts[i], self.deck_z(*pts[i]) - 0.02), (*pts[j], self.deck_z(*pts[j]) - 0.02), (*pts[j], z_top - 0.5), (*pts[i], z_top - 0.5)])
            M.poly("deck", mat, [(*pts[i], z_top - 0.5), (*pts[j], z_top - 0.5), (*top_pts[j], z_top), (*top_pts[i], z_top)])
        M.poly("deck", mat, [(x, y, z_top) for x, y in top_pts])
        self.shelf_top = z_top
        self.shelf_y = y0
        return y0, z_top

    def hinge_gap(self, y_front=None):
        y0 = self.hinge_y() - 1.2 if y_front is None else y_front
        y1 = getattr(self, "shelf_y", self.D / 2 - 0.2)
        hw = self.W / 2 - max(self.c["rRear"], 1.0)
        self.flat([(-hw, y0), (hw, y0), (hw, y1), (-hw, y1)], self.m["dark"], 0.03)

    def hinges(self, spans, r=2.1, drop=0.1):
        for a, b in spans:
            self.cylinder(self.body, "hinge", self.m["hinge"], (a + b) / 2 * self.W, self.hinge_y(), self.hR - drop, r, abs(b - a) * self.W, 16)

    # ---- paredes
    def wall_poly(self, side, pts, mat, lift=0.04, part="base"):
        """pts: (a, z); a es Y en los lados y X en la trasera."""
        if side == "rear":
            P = [(a, self.D / 2 + lift, z) for a, z in pts]
            out = (0, 1, 0)
        else:
            s = -1 if side == "left" else 1
            P = [(s * (self.W / 2 + lift), a, z) for a, z in pts]
            out = (s, 0, 0)
        nx = ny = nz = 0.0
        for i in range(len(P)):
            x0, y0, z0 = P[i]
            x1, y1, z1 = P[(i + 1) % len(P)]
            nx += (y0 - y1) * (z0 + z1)
            ny += (z0 - z1) * (x0 + x1)
            nz += (x0 - x1) * (y0 + y1)
        if nx * out[0] + ny * out[1] + nz * out[2] < 0:
            P.reverse()
        self.body.poly(part, mat, P)

    def wall_slats(self, side, a0, a1, z0, z1, count, mat=None, fill=0.58):
        mat = mat or self.m["dark"]
        step = (a1 - a0) / count
        for i in range(count):
            c = a0 + (i + 0.5) * step
            w = abs(step) * fill / 2
            self.wall_poly(side, [(c - w, z0), (c + w, z0), (c + w, z1), (c - w, z1)], mat)

    def port_shape(self, name, a, z, w, h):
        if name == "audio":
            return circle(a, z, min(w, h) / 2, 14)
        if name == "usbc":
            return pd.rounded_rect(a, z, w, h, h / 2, 4)
        if name == "hdmi":
            return [(a - w / 2, z + h / 2), (a + w / 2, z + h / 2), (a + w / 2, z - h * 0.08), (a + w / 2 - 1.3, z - h / 2), (a - w / 2 + 1.3, z - h / 2), (a - w / 2, z - h * 0.08)]
        if name == "rj45":
            n = min(3.0, w * 0.2)
            return [(a - w / 2, z + h / 2), (a + w / 2, z + h / 2), (a + w / 2, z - h * 0.2), (a + n, z - h * 0.2), (a + n, z - h / 2), (a - n, z - h / 2), (a - n, z - h * 0.2), (a - w / 2, z - h * 0.2)]
        return pd.rounded_rect(a, z, w, h, 0.5, 2)

    def place_ports(self, start=24.0, gap=4.6, rear_x=0.0, rear_z=None):
        ports = self.c.get("rawPorts") or {}
        hole = self.mat("Puerto", "#050506", 0.8, 0.05)
        tongue = self.mat("Lengueta", "#3d4046", 0.4, 0.3)
        lo, hi = self.band
        for side in ("left", "right", "rear"):
            names = list(ports.get(side) or [])
            if not names:
                continue
            if side == "right":
                names.reverse()
            sized = [n for n in names if n == "gap" or n in core.PORT_SIZE]
            if side == "rear":
                total = sum(26.0 if n == "gap" else core.PORT_SIZE[n][0] for n in sized) + gap * (len(sized) - 1)
                at = rear_x + total / 2
                d = -1
            else:
                at = self.D / 2 - start
                d = -1
            for n in sized:
                if n == "gap":
                    at += d * (26.0 + gap)
                    continue
                w, h = core.PORT_SIZE[n]
                c = at + d * w / 2
                if side == "rear":
                    z0, z1 = rear_z if rear_z else (self.hR * lo, self.hR * hi)
                else:
                    z0, z1 = self.h(c) * lo, self.h(c) * hi
                zc, room = (z0 + z1) / 2, (z1 - z0) * 0.9
                hh = min(h, room)
                self.wall_poly(side, self.port_shape(n, c, zc, w, hh), hole)
                if n == "usba":
                    self.wall_poly(side, pd.rounded_rect(c, zc + hh * 0.14, w * 0.76, hh * 0.24, 0.1, 1), tongue, 0.07)
                elif n in ("usbc", "hdmi"):
                    self.wall_poly(side, pd.rounded_rect(c, zc, w * 0.62, hh * 0.2, 0.08, 1), tongue, 0.07)
                at += d * (w + gap)

    # ---- base
    def foot_round(self, x, y, r, h=0.8, ring=None):
        poly = circle(x, y, r, 20)
        M, m = self.body, self.m["foot"]
        M.poly("base", m, [(a, b, -h) for a, b in reversed(poly)])
        for i in range(len(poly)):
            j = (i + 1) % len(poly)
            M.poly("base", m, [(*poly[i], -h), (*poly[j], -h), (*poly[j], 0.02), (*poly[i], 0.02)])
        if ring:
            self.ring(circle(x, y, r + 1.4, 20), 1.0, ring, put=lambda q, c: self.under(q, c))

    def foot_bar(self, x, y, w, d, h=0.8):
        poly = pd.rounded_rect(x, y, w, d, min(w, d) * 0.45, 4)
        M, m = self.body, self.m["foot"]
        M.poly("base", m, [(a, b, -h) for a, b in reversed(poly)])
        for i in range(len(poly)):
            j = (i + 1) % len(poly)
            M.poly("base", m, [(*poly[i], -h), (*poly[j], -h), (*poly[j], 0.02), (*poly[i], 0.02)])

    def under_dots(self, x0, y0, x1, y1, pitch=2.6, r=0.6, keep=None, color="#08090b"):
        self.dots(x0, y0, x1, y1, pitch, r, self.mat("Rejilla_base", color, 0.8, 0.05), put=lambda q, m: self.under(q, m), keep=keep)

    def under_slots(self, x0, y0, x1, y1, count, across=True, fill=0.55, color="#08090b"):
        self.slots(x0, y0, x1, y1, count, self.mat("Rejilla_base", color, 0.8, 0.05), across, fill, put=lambda q, m: self.under(q, m))

    def under_text(self, s, x, y, size, color):
        self.tb_under = True
        mat = self.mat("Rotulo_base", color, 0.5, 0.1)
        ob = self.tb.text("base", mat, s, 0.0, 0.0, 0.0, size, 0.0, "CENTER")
        me = ob.data
        for p in me.polygons:
            # girado para que se lea al dar la vuelta al equipo
            self.body.poly("base", mat, [(x + me.vertices[i].co.x, y - me.vertices[i].co.y, -0.04) for i in p.vertices])
        self.tb.objects.clear()
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)

    def build(self):
        self.build_body()
        self.build_lid()
        self.c["extras"](self)


# ------------------------------------------------------------------ logotipos simplificados (coordenadas locales: +y hacia el borde libre)

def alien_head(v, u, w, size, color, eye):
    f = v.lmap(u, w, size)
    pts = []
    for i in range(30):
        a = 2 * math.pi * i / 30
        k = 1 + 0.2 * math.sin(a)
        pts.append(f(0.74 * k * math.cos(a), math.sin(a)))
    v.lid_raised(pts, v.mat("Luz_logo", color, 0.35, 0.3), 0.08)
    z = v.mat("Ojos_logo", eye, 0.5, 0.2)
    for s in (-1, 1):
        q = [f(s * px, py) for px, py in ((0.1, 0.1), (0.56, 0.4), (0.52, 0.08), (0.22, -0.1))]
        v.lid_flat(q, z, 0.13)


def razer_mark(v, u, w, size, color):
    f = v.lmap(u, w, size)
    mat = v.mat("Luz_logo", color, 0.35, 0.2)
    for k in range(3):
        base = math.radians(90 + 120 * k)
        pts = []
        for i in range(11):
            t = i / 10
            a = base - math.radians(125) * t
            r = 1.0 - 0.12 * t
            pts.append((r * math.cos(a), r * math.sin(a)))
        for i in range(10, -1, -1):
            t = i / 10
            a = base - math.radians(125) * t + math.radians(26) * (1 - t)
            r = 0.34 + 0.46 * t + 0.12 * math.sin(math.pi * t)
            pts.append((r * math.cos(a), r * math.sin(a)))
        v.lid_flat([f(*p) for p in pts], mat, 0.05)


def apple_mark(v, u, w, size, color):
    f = v.lmap(u, w, size)
    pts = []
    for i in range(64):
        a = 2 * math.pi * i / 64
        r = 0.86 + 0.14 * math.cos(2 * a) - 0.13 * max(0.0, math.cos(a - math.pi / 2)) ** 10 - 0.11 * max(0.0, -math.sin(a)) ** 14
        x, y = r * math.cos(a) * 0.94, r * math.sin(a)
        dx, dy = x - 0.98, y - 0.2
        d = math.hypot(dx, dy)
        if d < 0.44:
            x, y = 0.98 + dx / (d or 1) * 0.44, 0.2 + dy / (d or 1) * 0.44
        pts.append((x, y))
    mat = v.mat("Logo", color, 0.2, 0.55)
    v.lid_flat([f(*p) for p in pts], mat, 0.04)
    v.lid_flat([f(*p) for p in ((0.02, 0.98), (0.1, 1.2), (0.34, 1.38), (0.32, 1.14), (0.2, 1.0))], mat, 0.04)


def hp_mark(v, u, w, size, color):
    x, y = v.lp(u, w)
    mat = v.mat("Logo", color, 0.3, 0.5)
    v.ring(circle(x, y, size, 40), size * 0.09, mat, put=lambda q, m: v.lid_flat(q, m, 0.05))
    v.lid_text("hp", u, w, size * 1.15, mat)


def thinkpad_mark(v, u, w, size, rot=0.0):
    white, red = v.mat("Logo", "#d8dadd", 0.45, 0.2), v.mat("Punto_rojo", "#d3171e", 0.5, 0.15)
    v.lid_text("ThinkPad", u, w, size, white, rot)
    f = v.lmap(u, w, 1.0, rot)
    v.lid_flat([f(*p) for p in circle(size * 2.32, size * 0.42, size * 0.13, 12)], red, 0.05)


# ------------------------------------------------------------------ un diseno por equipo

def legion(v):
    kb = v.keyboard(46, margin=24, numpad=4, np_scale=0.85, key="#101114", legend=None, rgb=RAINBOW, tray="#0b0c0e", gap=1.5, height=0.85, radius=0.9, tray_pad=2.4)
    v.touchpad(120, 75, kb, x=kb.cx, front=8, color="#26272b", style="inset")
    v.shelf("#1f2024")
    v.hinge_gap()
    v.hinges([(-0.36, -0.2), (0.2, 0.36)], 2.6)
    v.slots(kb.left, kb.top + 3.0, kb.end, kb.top + 6.0, 1, v.mat("Rejilla", "#090a0c", 0.8, 0.05), fill=0.5)
    v.wall_slats("rear", v.W * 0.3, v.W * 0.47, v.hR * 0.45, v.shelf_top - 2.4, 22)
    v.wall_slats("rear", -v.W * 0.47, -v.W * 0.3, v.hR * 0.45, v.shelf_top - 2.4, 22)
    for s in ("left", "right"):
        v.wall_slats(s, v.D / 2 - 54, v.D / 2 - 26, v.hR * 0.5, v.hR * 0.86, 12)
    v.under_slots(-v.W * 0.4, v.D * 0.02, v.W * 0.4, v.D * 0.42, 15, across=False, fill=0.6)
    v.foot_bar(0, v.F(10), v.W * 0.72, 5.5)
    v.foot_bar(0, v.Y(8), v.W * 0.8, 5.5)
    v.place_ports(start=30, rear_z=(v.hR * 0.45, v.shelf_top - 2.2))


def loq(v):
    kb = v.keyboard(38, margin=22, numpad=4, np_scale=0.86, key="#16171a", legend="#dfe5ec", tray="#121316", gap=1.45, height=0.8, radius=1.1)
    v.touchpad(120, 75, kb, x=kb.cx, front=9, style="inset")
    v.shelf(tone(v.c["wall"], 0.9))
    v.hinge_gap()
    v.hinges([(-0.38, -0.27), (0.27, 0.38)], 2.5)
    v.lid_text("LOQ", 0, 0.6, 20, v.mat("Logo", tone(v.c["lid"], 2.1), 0.35, 0.3))
    v.wall_slats("rear", v.W * 0.3, v.W * 0.47, v.hR * 0.4, v.shelf_top - 2, 16)
    v.wall_slats("rear", -v.W * 0.47, -v.W * 0.3, v.hR * 0.4, v.shelf_top - 2, 16)
    for s in ("left", "right"):
        v.wall_slats(s, v.D / 2 - 62, v.D / 2 - 26, v.hR * 0.46, v.hR * 0.72, 14)
    for s in (-1, 1):
        a, b = sorted((s * v.W * 0.08, s * v.W * 0.4))
        v.under_slots(a, v.D * 0.1, b, v.D * 0.42, 12, across=True, fill=0.55)
    v.foot_bar(0, v.F(9), v.W * 0.5, 5)
    for s in (-1, 1):
        v.foot_round(s * v.W * 0.42, v.F(12), 5)
        v.foot_bar(s * v.W * 0.3, v.Y(8), 60, 5)


def x1g14(v):
    kb = v.keyboard(16, pitch=18.45, layout="thinkpad", fn=0.6, key="#131416", legend="#e4e6e9", tray="#0e0f11", rim="#2b2c2f", gap=1.45, height=0.75, radius=1.5, trackpoint=True, copilot=True)
    v.c["keyColor"] = "#131416"
    v.touchpad(120, 80, kb, x=0, front=3.5, style="inset", color="#26272a", buttons=3, stripes="#a31419", radius=1.5)
    v.hinge_gap()
    v.hinges([(-0.26, 0.26)], 1.9)
    v.power(kb.end - 7, kb.top + 5, 11, 5.5, "#0f1012", 1.6, 0.25)
    bar = v.mat("Barra_camara", "#35363a", 0.26, 0.6)
    x, y = v.lp(0, 1)
    v.lid_raised(pd.rounded_rect(x, y + 6.0, 96, 12.0, 5.8, 6), bar, 2.55, v.mat("Barra_lado", "#26272a", 0.4, 0.3), 0.3)
    thinkpad_mark(v, -0.33, 0.9, 9.0)
    v.lid_text("X1", -0.4, 0.8, 11.5, v.mat("Logo", "#d8dadd", 0.45, 0.2))
    v.lid_text("Lenovo", 0.44, 0.12, 7.0, v.mat("Logo_tenue", "#6a6c70", 0.4, 0.25), math.pi / 2)
    for s in (-1, 1):
        v.under_dots(s * v.W * 0.27 - 23, v.D * 0.06, s * v.W * 0.27 + 23, v.D * 0.06 + 46, 2.3, 0.55, keep=lambda a, b, c=s: math.hypot(a - c * v.W * 0.27, b - (v.D * 0.06 + 23)) < 22)
    v.under_slots(-v.W * 0.1, v.D * 0.2, v.W * 0.1, v.D * 0.36, 8, across=True)
    v.foot_bar(0, v.F(12), v.W * 0.62, 4.2, 0.7)
    v.foot_bar(0, v.Y(8), v.W * 0.72, 4.2, 0.7)
    v.place_ports(start=22)


def p16(v):
    kb = v.keyboard(36, margin=24, numpad=4, np_scale=0.85, layout="thinkpad", key="#141518", legend="#e4e6e9", tray="#0f1012", rim="#2b2d31", gap=1.5, height=0.85, radius=1.5, trackpoint=True, copilot=True)
    v.c["keyColor"] = "#141518"
    v.touchpad(115, 67, kb, x=kb.cx, front=7, style="inset", color="#2b2d30", buttons=3, stripes="#a31419", radius=1.5)
    v.shelf("#191a1d")
    v.hinge_gap()
    v.hinges([(-0.3, 0.3)], 2.8)
    v.power(kb.end - 6, kb.top + 4.6, 13, 5.4, "#0f1012", 1.6, 0.25)
    thinkpad_mark(v, -0.32, 0.92, 11.0)
    v.lid_text("Lenovo", 0.45, 0.15, 9.0, v.mat("Logo_tenue", "#6f7175", 0.4, 0.25), math.pi / 2)
    v.wall_slats("rear", v.W * 0.2, v.W * 0.47, v.hR * 0.42, v.shelf_top - 2.2, 30)
    v.wall_slats("rear", -v.W * 0.47, -v.W * 0.2, v.hR * 0.42, v.shelf_top - 2.2, 30)
    v.under_slots(-v.W * 0.42, v.D * 0.06, v.W * 0.42, v.D * 0.42, 34, across=False, fill=0.55)
    v.foot_bar(0, v.F(10), v.W * 0.7, 5.5)
    v.foot_bar(0, v.Y(7), v.W * 0.8, 5.5)
    v.place_ports(start=34, rear_z=(v.hR * 0.42, v.shelf_top - 2.2))


def slim5(v):
    kb = v.keyboard(23, margin=26, numpad=4, np_scale=0.85, key="#26282c", legend="#f0f2f5", tray=tone(v.c["deck"], 0.72), gap=1.45, height=0.6, radius=1.1, copilot=True)
    v.touchpad(120, 75, kb, x=kb.cx, front=10, style="flat", radius=2.2)
    v.hinge_gap()
    v.hinges([(-0.3, 0.3)], 1.9)
    v.power(kb.end - 8, kb.top + 5, 12, 5.0, tone(v.c["deck"], 0.8), 1.2, 0.2)
    v.lid_text("Lenovo", 0.34, 0.08, 9.0, v.mat("Logo", tone(v.c["lid"], 0.62), 0.3, 0.45))
    v.under_slots(-v.W * 0.36, v.D * 0.2, v.W * 0.36, v.D * 0.42, 9, across=True, fill=0.42)
    v.foot_bar(0, v.F(11), v.W * 0.72, 4.2, 0.7)
    v.foot_bar(0, v.Y(9), v.W * 0.8, 4.2, 0.7)
    v.place_ports(start=26)


def blade24(v):
    kb = v.keyboard(25, pitch=18.75, layout="half", fn=0.62, key="#101012", legend="#bff5b0", gap=1.4, height=0.65, radius=0.9)
    v.touchpad(170, 97, kb, front=4, style="flat", color="#242427", radius=2.0)
    v.side_grilles(kb, inset=5.2, pitch=1.9, r=0.42)
    v.hinge_gap()
    v.hinges([(-0.34, 0.34)], 2.3)
    razer_mark(v, 0, 0.56, 19, "#3fd02a")
    v.wall_slats("rear", -v.W * 0.4, v.W * 0.4, v.hR * 0.34, v.hR * 0.72, 62, fill=0.5)
    v.under_slots(-v.W * 0.36, -v.D * 0.02, v.W * 0.36, v.D * 0.41, 40, across=False, fill=0.48)
    for s in (-1, 1):
        v.foot_bar(s * v.W * 0.2, v.F(10), v.W * 0.3, 4.6)
    v.foot_bar(0, v.Y(9), v.W * 0.84, 4.6)
    v.place_ports(start=26)


def blade26(v):
    kb = v.keyboard(24, pitch=18.75, layout="half", fn=0.62, key="#101012", legend="#bff5b0", gap=1.3, height=0.55, radius=0.9)
    v.touchpad(150, 97, kb, front=7, style="flat", color="#242427", radius=2.0)
    v.side_grilles(kb, inset=5.2, pitch=1.75, r=0.38)
    v.hinge_gap()
    v.hinges([(-0.36, 0.36)], 1.9)
    razer_mark(v, 0, 0.55, 18, "#4bea33")
    v.wall_slats("rear", -v.W * 0.42, v.W * 0.42, v.hR * 0.3, v.hR * 0.76, 70, fill=0.5)
    for s in (-1, 1):
        cx = s * v.W * 0.33
        v.under_dots(cx - 26, v.D * 0.1, cx + 26, v.D * 0.1 + 52, 2.3, 0.6, keep=lambda a, b, c=cx: math.hypot(a - c, b - (v.D * 0.1 + 26)) < 25)
    v.under_slots(-v.W * 0.2, v.D * 0.02, v.W * 0.2, v.D * 0.42, 24, across=False, fill=0.48)
    v.foot_bar(0, v.F(10), v.W * 0.62, 4.4)
    v.foot_bar(0, v.Y(9), v.W * 0.84, 4.4)
    v.place_ports(start=24)


def xps14(v):
    kb = v.keyboard(11, pitch=(v.W - 7.0) / 15, layout="half", fn=0.72, key="#1a1b1e", legend="#d7dade", gap=0.32, height=0.34, radius=0.25, legend_scale=0.8)
    v.touchpad(152, 75, kb, front=6, style="etched", edge=tone(v.c["deck"], 0.7), radius=1.6)
    v.hinge_gap()
    v.hinges([(-0.46, -0.37), (0.37, 0.46)], 1.7)
    v.lid_text("XPS", 0, 0.52, 17, v.mat("Logo", tone(v.c["lid"], 2.2), 0.25, 0.55))
    dark = v.m["dark"]
    v.under(pd.rounded_rect(0, v.D / 2 - 5.6, v.W * 0.64, 3.0, 1.4, 3), dark)
    v.wall_slats("rear", -v.W * 0.32, v.W * 0.32, v.hR * 0.2, v.hR * 0.6, 54, fill=0.52)
    v.under_text("DELL", 0, v.D * 0.02, 10, tone(v.c["under"], 1.9))
    for s in (-1, 1):
        for y in (v.F(10), v.Y(16)):
            v.foot_round(s * (v.W / 2 - 13), y, 3.8, 0.6)
    v.place_ports(start=19, gap=5.0)


def area51(v):
    kb = v.keyboard(53, pitch=18.75, layout="std", key="#0d0e11", legend=None, rgb=["#23d5e8", "#2fb6ff", "#3f8cff", "#6a6dff", "#2fb6ff", "#23d5e8"], gap=1.5, height=0.95, radius=0.8, tray="#0a0b0d", tray_pad=2.5)
    teal = "#31d8e6"
    v.touchpad(112, 65, kb, front=34, style="inset", color="#1e2226", edge="#0d0e11", lit=teal, radius=2.2)
    v.shelf("#20262a")
    v.hinge_gap()
    v.hinges([(-0.42, -0.26), (0.26, 0.42)], 3.0, 0.3)
    v.slots(kb.left + 8, kb.top + 2.9, kb.end - 8, kb.top + 5.0, 1, v.mat("Rejilla", "#07080a", 0.8, 0.05), fill=0.7)
    v.deck_text("A L I E N W A R E", 0, kb.top + 7.3, 2.9, v.mat("Rotulo_cubierta", "#7f8a92", 0.4, 0.3))
    alien_head(v, 0, 0.64, 15, "#8cd0db", "#10232a")
    top = v.shelf_top
    for s, side in ((-1, "left"), (1, "right")):
        v.wall_poly(side, pd.rounded_rect(v.D / 2 - 27, top * 0.48, 38, top * 0.46, top * 0.2, 6), v.m["dark"])
        v.wall_slats(side, v.D / 2 - 43, v.D / 2 - 11, top * 0.33, top * 0.63, 11, v.mat("Aleta", "#2c343a", 0.5, 0.2))
    for s in (-1, 1):
        a, b = sorted((s * v.W * 0.27, s * v.W * 0.47))
        v.wall_slats("rear", a, b, top * 0.28, top * 0.84, 18, v.m["dark"], 0.62)
    v.wall_poly("rear", pd.rounded_rect(0, top * 0.1, v.W * 0.86, 1.3, 0.6, 2), v.mat("Luz_trasera", teal, 0.4, 0.1), 0.05)
    glass = v.mat("Ventana_inferior", "#142a30", 0.12, 0.8)
    v.under(pd.rounded_rect(0, v.D * 0.12, v.W * 0.3, v.D * 0.22, 6, 4), glass)
    v.under_slots(-v.W * 0.44, v.D * 0.04, -v.W * 0.18, v.D * 0.44, 18, across=True)
    v.under_slots(v.W * 0.18, v.D * 0.04, v.W * 0.44, v.D * 0.44, 18, across=True)
    v.foot_bar(0, v.F(13), v.W * 0.72, 6)
    v.foot_bar(0, v.Y(9), v.W * 0.78, 6)
    v.place_ports(start=64, rear_z=(top * 0.28, top * 0.78))


def g14(v):
    kb = v.keyboard(25, pitch=18.2, key="#2c2e33", legend="#efe9dc", gap=1.5, height=0.72, radius=1.0, fn=0.62)
    v.touchpad(130, 85, kb, front=4.5, style="flat", radius=2.0, edge=tone(v.c["deck"], 0.68))
    mk = "#2c2e33"
    for i in range(4):
        v.power(-46 + i * 16.5, kb.top + 5.3, 14.8, 5.3, mk, 1.0, 0.45)
    v.power(kb.right - 7.5, kb.top + 5.3, 13, 6.3, "#3a3c42", 3.0, 0.3, "#f2f2ef")
    v.side_grilles(kb, inset=4.8, pitch=1.75, r=0.4, y1=kb.top + 8)
    v.hinge_gap()
    v.hinges([(-0.42, -0.34), (0.34, 0.42)], 2.0)
    led = v.mat("Luz_slash", "#e9edf2", 0.3, 0.3)
    n = 42
    for i in range(n):
        t = (i + 0.5) / n
        x, y = v.lp(pd.lerp(-0.37, 0.37, t), pd.lerp(0.87, 0.13, t))
        ang = math.atan2(0.74 * v.L, 0.74 * v.W)
        c, s = math.cos(ang), math.sin(ang)
        q = [(a * c - b * s + x, a * s + b * c + y) for a, b in ((-2.3, -0.7), (2.3, -0.7), (2.3, 0.7), (-2.3, 0.7))]
        v.lid_flat(q, led, 0.05)
    v.wall_slats("rear", -v.W * 0.36, v.W * 0.36, v.hR * 0.26, v.hR * 0.76, 40, fill=0.5)
    v.under_dots(-v.W * 0.42, v.D * 0.0, v.W * 0.42, v.D * 0.43, 2.5, 0.62)
    v.foot_bar(0, v.F(10), v.W * 0.8, 4.2)
    v.foot_bar(0, v.Y(6), v.W * 0.84, 3.8)
    v.place_ports(start=26)


def zenbook(v):
    kb = v.keyboard(14, pitch=18.45, fn=0.56, key=tone(v.c["deck"], 0.62), legend="#f1f2f4", gap=1.45, height=0.5, radius=1.2)
    v.touchpad(150.3, 99.5, kb, front=2.8, style="flat", radius=2.0, color=tone(v.c["deck"], 1.04), edge=tone(v.c["deck"], 0.8))
    v.hinge_gap()
    v.hinges([(-0.29, 0.29)], 1.7)
    v.flat(pd.rounded_rect(0, v.F(1.2), 34, 2.2, 1.0, 3), v.mat("Muesca", tone(v.c["deck"], 0.8), 0.5, 0.2), 0.03)
    v.lid_text("ASUS Zenbook", 0.0, 0.5, 7.4, v.mat("Logo", tone(v.c["lid"], 1.6), 0.3, 0.4))
    v.slots(-v.W * 0.3, v.Y(10), v.W * 0.3, v.Y(6.2), 1, v.mat("Rejilla", tone(v.c["deck"], 0.55), 0.8, 0.05), fill=0.42)
    v.under_slots(-v.W * 0.3, v.D * 0.26, v.W * 0.3, v.D * 0.43, 5, across=True, fill=0.34)
    v.foot_bar(0, v.F(9), v.W * 0.66, 3.6, 0.6)
    v.foot_bar(0, v.Y(9), v.W * 0.78, 3.6, 0.6)
    v.place_ports(start=24)


def aspire(v):
    kb = v.keyboard(24, margin=26, numpad=4, np_scale=0.8, key="#26282c", legend="#f4f5f7", gap=1.5, height=0.62, radius=1.0, copilot=True, tray=tone(v.c["deck"], 0.76))
    v.touchpad(150, 92, kb, x=kb.cx, front=4, style="flat", radius=1.8)
    v.hinge_gap()
    v.hinges([(-0.3, 0.3)], 1.9)
    v.lid_text("acer", 0, 0.54, 17, v.mat("Logo", tone(v.c["lid"], 0.66), 0.26, 0.5))
    v.under_slots(-v.W * 0.34, v.D * 0.14, v.W * 0.34, v.D * 0.42, 10, across=True, fill=0.42)
    v.foot_bar(0, v.F(9), v.W * 0.72, 4.2, 0.7)
    v.foot_bar(0, v.Y(9), v.W * 0.8, 4.2, 0.7)
    v.place_ports(start=24, gap=4.2)


def swift16(v):
    kb = v.keyboard(17, margin=30, numpad=4, np_scale=0.84, key="#1f2125", legend="#eef0f3", gap=1.35, height=0.5, radius=1.0, fn=0.6, copilot=True)
    v.touchpad(175.5, 109.7, kb, x=0, front=2.6, style="etched", edge=tone(v.c["deck"], 0.68), radius=2.2)
    v.hinge_gap()
    v.hinges([(-0.34, 0.34)], 1.8)
    v.lid_text("acer", 0, 0.52, 16, v.mat("Logo", tone(v.c["lid"], 1.7), 0.25, 0.5))
    v.under_slots(-v.W * 0.36, v.D * 0.18, v.W * 0.36, v.D * 0.43, 12, across=True, fill=0.42)
    v.foot_bar(0, v.F(9), v.W * 0.74, 3.8, 0.6)
    v.foot_bar(0, v.Y(9), v.W * 0.82, 3.8, 0.6)
    v.place_ports(start=24)


def prestige(v):
    kb = v.keyboard(21, pitch=18.2, key="#23252a", legend="#eef0f3", gap=1.5, height=0.55, radius=1.3, fn=0.6, tray=tone(v.c["deck"], 0.8), copilot=True)
    v.touchpad(138, 84, kb, front=3.4, style="flat", radius=2.4, edge=tone(v.c["deck"], 0.74))
    v.power(kb.right - 6.5, kb.top + 4.6, 10.5, 5.0, tone(v.c["deck"], 0.72), 1.2, 0.2)
    v.hinge_gap()
    # convertible: dos bisagras de 360 grados bien visibles
    v.hinges([(-0.455, -0.385), (0.385, 0.455)], 2.5, -0.1)
    v.lid_text("msi", 0.34, 0.86, 12, v.mat("Logo", tone(v.c["lid"], 0.66), 0.25, 0.55))
    v.under_dots(-v.W * 0.4, v.D * 0.2, v.W * 0.4, v.D * 0.41, 2.4, 0.62)
    v.foot_bar(0, v.F(9), v.W * 0.8, 3.8, 0.6)
    v.foot_bar(0, v.Y(9), v.W * 0.84, 3.8, 0.6)
    v.place_ports(start=25)


def omnibook(v):
    kb = v.keyboard(21, pitch=18.2, key="#2a2c31", legend="#f0f1f3", gap=1.45, height=0.42, radius=1.1, fn=0.6, copilot=True, tray=tone(v.c["deck"], 0.84))
    v.touchpad(132, 86, kb, front=4, style="etched", edge=tone(v.c["deck"], 0.74), radius=2.2)
    v.hinge_gap()
    v.hinges([(-0.3, 0.3)], 1.5)
    hp_mark(v, 0, 0.52, 13, tone(v.c["lid"], 0.62))
    v.under_slots(-v.W * 0.3, v.D * 0.22, v.W * 0.3, v.D * 0.42, 6, across=True, fill=0.34)
    v.foot_bar(0, v.F(9), v.W * 0.7, 3.2, 0.5)
    v.foot_bar(0, v.Y(9), v.W * 0.8, 3.2, 0.5)
    v.place_ports(start=24, gap=5.4)


def gram(v):
    kb = v.keyboard(19, margin=26, numpad=4, np_scale=0.86, key="#2a2c30", legend="#f2f3f5", gap=1.45, height=0.42, radius=1.1, fn=0.58, copilot=True)
    v.touchpad(128, 80, kb, x=kb.cx, front=8, style="flat", radius=2.0, edge=tone(v.c["deck"], 0.74))
    v.hinge_gap()
    v.hinges([(-0.33, 0.33)], 1.6)
    v.lid_text("LG gram", 0.0, 0.5, 11.5, v.mat("Logo", tone(v.c["lid"], 0.6), 0.3, 0.4))
    v.under_slots(-v.W * 0.34, v.D * 0.24, v.W * 0.34, v.D * 0.42, 6, across=True, fill=0.34)
    v.foot_bar(0, v.F(10), v.W * 0.72, 3.4, 0.5)
    v.foot_bar(0, v.Y(10), v.W * 0.8, 3.4, 0.5)


def book6(v):
    kb = v.keyboard(24, pitch=18.75, key="#17181c", legend="#e9ebee", gap=1.5, height=0.5, radius=1.1, tray=tone(v.c["deck"], 0.82), fn=0.62, copilot=True)
    v.touchpad(164, 100, kb, front=5, style="etched", edge=tone(v.c["deck"], 0.7), radius=2.4)
    v.power(kb.right + 11, kb.top - 4.6, 11, 7.2, tone(v.c["deck"], 0.8), 1.2, 0.2)
    v.hinge_gap()
    v.hinges([(-0.4, 0.4)], 1.5)
    v.lid_text("SAMSUNG", 0, 0.93, 6.6, v.mat("Logo", tone(v.c["lid"], 2.0), 0.3, 0.4))
    v.under_dots(-v.W * 0.24, v.D * 0.1, v.W * 0.24, v.D * 0.4, 2.3, 0.72)
    edge = v.mat("Aro_pie", tone(v.c["under"], 1.5), 0.4, 0.3)
    for s in (-1, 1):
        v.foot_round(s * (v.W / 2 - 18), v.F(18), 6.5, 0.9, edge)
        v.foot_round(s * (v.W / 2 - 18), v.Y(18), 6.5, 0.9, edge)
    v.place_ports(start=27)


def neo(v):
    kb = v.keyboard(14, pitch=17.9, layout="apple", fn=0.82, key="#e4e5e7", legend="#2c2e33", gap=1.55, height=0.5, radius=1.5, legend_scale=0.88)
    v.touchpad(115, 72, kb, front=7, style="flat", color=tone(v.c["deck"], 1.03), edge=tone(v.c["deck"], 0.72), radius=3.4)
    # Touch ID en la esquina superior derecha del teclado
    v.flat(circle(kb.end - kb.p * 0.75, kb.top - kb.p * kb.fn / 2, 3.4, 22), v.mat("Touch_ID", "#cfd1d5", 0.3, 0.45), 0.53, "keys")
    v.hinge_gap()
    v.hinges([(-0.25, 0.25)], 1.6)
    apple_mark(v, 0, 0.5, 15, tone(v.c["lid"], 0.68))
    for s in (-1, 1):
        for y in (v.F(14), v.Y(14)):
            v.foot_round(s * (v.W / 2 - 14), y, 4.4, 0.7)
    v.under_text("MacBook", 0, 0, 6, tone(v.c["under"], 0.6))
    v.place_ports(start=22, gap=5.0)


# ------------------------------------------------------------------ configuracion

def base(i, dress, lidT, shell=None, deck=None, lid=None, wall=None, under=None, keySide="#0e0f11", hinge="#17181b", lidEdge=None, rings=R_SLAB,
         rFront=6.0, rRear=3.0, rLidFree=5.0, rLidHinge=1.5, T=None, Tf=None, inset=0.0, finish=None, bump=None):
    def make():
        e = pc.specs_of(i)
        sp, de, lk = e["specs"], e.get("design", {}), e.get("look") or {}
        s = shell or lk.get("shell", "#55585e")
        Tmax = T or de.get("thicknessMaxMm") or sp["thicknessMm"]
        Tmin = Tf or de.get("thicknessMinMm") or Tmax
        hR = Tmax - lidT
        c = {"builder": SpecDesign, "screen": SCREENS.get(i), "W": sp["widthMm"], "D": sp["depthMm"], "hRear": hR, "hFront": max(Tmin - lidT, hR * 0.6), "lidT": lidT,
             "rFront": rFront, "rRear": rRear, "rLidFree": rLidFree, "rLidHinge": rLidHinge, "rings": rings, "photos": {},
             "deck": deck or lk.get("deck", s), "lid": lid or s, "wall": wall or s, "under": under or tone(s, 0.86), "keySide": keySide, "hinge": hinge,
             "lidEdge": lidEdge or wall or s, "hingeInset": inset, "extras": dress, "finish": finish or {}, "rawPorts": de.get("ports") or {}}
        if bump:
            # saliente del borde libre de la tapa (camara): ancho y alto en mm, centrado
            w, h = bump
            c["lidEdgePx"] = 3.0
            scale = (c["D"] - 3.0) / (c["D"] - inset - 1.5)
            pts = []
            for k in range(11):
                t = k / 10
                x = w / 2 - w * t
                y = h * math.sin(math.pi * min(1.0, min(t, 1 - t) * 5.0) / 2)
                pts.append((x, y))
            c["lidBump"] = [(c["W"] / 2 + x, 3.0 - y * scale) for x, y in pts]
        return c

    return make


# fondos originales generados por procedimiento, con la paleta de cada familia; no reproducen los del fabricante
SCREENS = {
    19: {"style": "slash", "colors": ["#07090f", "#101a2c", "#2f7bff", "#7a5cff", "#16d3e6", "#e0e7ff"]},
    48: {"style": "slash", "colors": ["#080c10", "#14222a", "#7cf03a", "#22c7a9", "#d8ff5a"]},
    36: {"style": "ribbons", "colors": ["#090406", "#1a0a0f", "#3a0d16", "#e0232e", "#ff6a4d", "#8a1220", "#f3f0ee"], "chin": "ThinkPad"},
    44: {"style": "ribbons", "colors": ["#07080c", "#11141c", "#20242f", "#e0232e", "#4a78ff", "#b8c4d8"], "chin": "ThinkPad"},
    54: {"style": "folds", "colors": ["#0f1b36", "#234f86", "#5f9fd0", "#c9e0ee"]},
    20: {"style": "rings", "colors": ["#040806", "#0a140d", "#44d62c", "#a6ff8f"]},
    39: {"style": "rings", "colors": ["#030504", "#07120a", "#2fe36a", "#d2ff4a"]},
    40: {"style": "rings", "colors": ["#040507", "#08130f", "#1fd8a0", "#7dff6a"]},
    33: {"style": "bloom", "colors": ["#0a0f1d", "#141c34", "#090b14", "#3a8dff", "#8b6bff", "#e6eeff"]},
    34: {"style": "bloom", "colors": ["#10121a", "#1e2231", "#0a0b12", "#6f9bd8", "#b0bdd6", "#e8edf6"]},
    50: {"style": "nebula", "colors": ["#02070a", "#07242c", "#0e5a66", "#35e3ee"]},
    35: {"style": "slash", "colors": ["#0a060c", "#190a14", "#ff2d55", "#ff7a2f", "#ff3fb4", "#ffe2d0"]},
    47: {"style": "ribbons", "colors": ["#0a1020", "#16264a", "#2a4a86", "#68a8ff", "#b58cff", "#f2c9e6"]},
    46: {"style": "aurora", "colors": ["#04100f", "#0a2424", "#31e3a2", "#2fb7e8", "#b6ff7a"]},
    51: {"style": "aurora", "colors": ["#060b16", "#101d33", "#5a8dff", "#35d9c7", "#c58bff"]},
    45: {"style": "bloom", "colors": ["#0b0a18", "#1b1636", "#08070f", "#ff8a2b", "#3b6bff", "#ffe0b8"], "chin": "msi"},
    38: {"style": "folds", "colors": ["#0b1430", "#1e3f78", "#4f8fcb", "#b7d6ec"]},
    43: {"style": "folds", "colors": ["#1c0a2a", "#6a1f60", "#c74a8a", "#f3b7a8"]},
    37: {"style": "ribbons", "colors": ["#070a1c", "#111a3f", "#1f2f6b", "#3f7bff", "#b45cff", "#62e0ff"]},
    55: {"style": "hills", "colors": ["#f6d58c", "#f3a76b", "#ffe9b0", "#d9c24e", "#a8a93a", "#6f8434", "#3f5a2e"], "os": "mac", "corner": 2.4},
}


SOURCES = {
    "legion": ["https://www.notebookcheck.net/Lenovo-Legion-Pro-7-16-2024-review-Cream-of-the-crop-amongst-RTX-4080-laptops.819046.0.html", "https://gamesradar.com/hardware/laptops/lenovo-legion-pro-7i-gen-9-review"],
    "loq": ["https://www.notebookcheck.net/Eyesore-or-eye-catcher-Lenovo-LOQ-15-gaming-laptop-review.1338691.0.html", "https://psref.lenovo.com/syspool/Sys/PDF/LOQ/LOQ_15AHP11/LOQ_15AHP11_Spec.pdf"],
    "x1": ["https://www.notebookcheck.net/Flagship-business-laptop-with-almost-24-hours-of-battery-life-Lenovo-ThinkPad-X1-Carbon-Gen-14-Review.1332558.0.html"],
    "p16": ["https://www.notebookcheck.net/This-laptop-is-a-local-AI-monster-Lenovo-ThinkPad-P16-Gen-3-review.1221962.0.html"],
    "slim": ["https://www.notebookcheck.net/Dual-debut-in-the-Lenovo-IdeaPad-Slim-5-16-Ryzen-AI-5-330-and-Radeon-820M-in-review.1166676.0.html"],
    "blade24": ["https://www.notebookcheck.net/Razer-Blade-16-2024-review.804677.0.html"],
    "blade26": ["https://www.notebookcheck.net/Razer-Blade-16-2025-review-Gaming-laptop-now-with-GeForce-RTX-5090-Laptop-and-an-even-slimmer-case.988251.0.html"],
    "xps": ["https://www.notebookcheck.net/Dell-XPS-14-2026-review-Fully-reborn-with-Intel-Panther-Lake-X7.1218670.0.html", "https://tomshardware.com/laptops/dell-xps-14-2026-da14260-review"],
    "aw": ["https://www.notebookcheck.net/Alienware-16-Area-51-laptop-review-Dell-s-best-flagship-gaming-laptop-yet.1027688.0.html"],
    "g14": ["https://www.notebookcheck.net/One-of-the-best-compact-gaming-laptops-gets-a-brighter-OLED-Asus-ROG-Zephyrus-G14-2026-Review.1321952.0.html"],
    "zen": ["https://www.notebookcheck.net/Asus-ZenBook-S14-2026-Laptop-Review-Brighter-OLED-but-a-slow-Panther-Lake-iGPU.1342874.0.html", "https://press.asus.com/news/press-releases/zenbook-s14-2026/"],
    "aspire": ["https://www.techradar.com/computing/windows-laptops/acer-aspire-16-ai-review"],
    "swift": ["https://www.notebookcheck.net/Acer-Swift-16-AI-review-High-performance-meets-impressive-cooling.1289101.0.html"],
    "msi": ["https://www.notebookcheck.net/Nearly-20-hours-of-battery-life-with-Panther-Lake-MSI-Prestige-14-Flip-AI-Convertible-review.1243401.0.html"],
    "hp": ["https://www.notebookcheck.net/HP-OmniBook-Ultra-14-Core-Ultra-X9-review-Fully-redesigned-and-so-much-better-than-before.1317970.0.html"],
    "lg": ["https://www.notebookcheck.net/LG-gram-Pro-AI-2026-16Z90U.1240959.0.html", "https://www.techradar.com/computing/gaming-laptops/lg-gram-pro-16-2026"],
    "book": ["https://www.notebookcheck.net/Samsung-Galaxy-Book6-Ultra-Review-Impressive-multimedia-laptop-with-gorgeous-OLED-RTX-5070.1242686.0.html"],
    "neo": ["https://www.notebookcheck.net/Apple-MacBook-Neo-Review-Surprisingly-good-and-capable-laptop-for-599-with-one-big-flaw.1247679.0.html"],
}


def add(ids, dress, features, approx, src, resolved=(), **body):
    for i in ids:
        pd.DESIGNS[i] = {"cfg": base(i, dress, **body), "ports": None, "features": features, "basis": "descripciones", "approx": approx,
                         "sources": SOURCES[src], "resolved": resolved}


METAL = {"rough": 0.4, "spec": 0.38}
SOFT = {"rough": 0.5, "spec": 0.24}
PLASTIC = {"rough": 0.68, "spec": 0.1}

add([19], legion, ["teclado completo con bloque numérico y RGB por tecla", "touchpad de 120 × 75 mm bajo las teclas principales", "bloque trasero con los puertos y salidas de aire", "saliente de la cámara en la tapa", "base con gran rejilla central y dos barras de goma"],
    ["sin logotipo en la tapa: ninguna fuente consultada lo describe", "posición de las rejillas laterales y traseras"], "legion", ("bloque numérico",),
    lidT=6.4, deck="#26272b", lid="#2c2d31", wall="#2a2b2f", under="#1f2023", hinge="#141518", rings=R_GAMER, rFront=9, rRear=4, rLidFree=5, rLidHinge=1.5, inset=13, finish=METAL, bump=(58, 2.6))
add([48], loq, ["chasis de plástico con bloque trasero", "rótulo LOQ centrado en la tapa", "teclado con bloque numérico y flechas de buen tamaño", "aletas de ventilación laterales", "dos campos de rejilla en la base"],
    ["color Eclipse Black supuesto: la ficha no dice el de esta unidad", "tipografía del rótulo", "puertos sin colocar: ninguna fuente dice su lado exacto en este modelo"], "loq", ("logotipo", "rejillas"),
    lidT=6.0, rings=R_GAMER, rFront=8, rRear=4, rLidFree=5, rLidHinge=1.5, inset=11.5, finish=PLASTIC, hinge="#15161a")
add([36], x1g14, ["barra de la cámara en relieve sobre el borde de la tapa", "rótulos ThinkPad X1 con punto rojo y Lenovo", "TrackPoint rojo y tres botones físicos", "bandeja del teclado con marco", "chaflán inferior ancho", "dos ventiladores redondos en la base"],
    ["medidas de la barra de la cámara", "tipografía de los rótulos", "tramado de fibra de carbono: se representa con color mate"], "x1", ("logotipo", "rejillas"),
    lidT=3.8, T=14.3, deck="#1e1f21", lid="#1b1c1e", wall="#1d1e20", under="#17181a", lidEdge="#161719", hinge="#101113", rings=R_WEDGE, rFront=4.5, rRear=2, rLidFree=2.5, rLidHinge=1, finish={"rough": 0.62, "spec": 0.12, "weave": 1.6}, bump=(90, 2.4))
add([44], p16, ["bloque trasero con rejillas y puertos", "bisagra ancha de una pieza", "teclado con bloque numérico y TrackPoint", "touchpad de 115 × 67 mm con tres botones", "rótulos ThinkPad y Lenovo", "base casi entera de rejilla"],
    ["altura de la zona trasera de 29,8 mm: su forma no se publica y no se modela", "tipografía de los rótulos"], "p16", ("logotipo", "rejillas"),
    lidT=5.6, deck="#1f2023", lid="#202124", wall="#1d1e21", under="#17181a", hinge="#111214", rings=R_SLAB, rFront=5, rRear=2.5, rLidFree=3, rLidHinge=1, inset=14, finish={"rough": 0.58, "spec": 0.16})
add([54], slim5, ["aluminio anodizado Luna Grey", "teclado con bloque numérico y bandeja", "touchpad de 120 × 75 mm desplazado bajo las letras", "bisagra de 165 grados", "rejilla horizontal en la base"],
    ["el color real depende de la unidad", "posición del rótulo Lenovo"], "slim", ("logotipo",), lidT=4.2, deck="#8d9196", lid="#8a8e93", wall="#8f9398", under="#7d8186", rings=R_SLAB, rFront=5.5, rRear=2.5, rLidFree=4, rLidHinge=1.2, finish=METAL)
add([20], blade24, ["unibody de aluminio negro", "logotipo verde centrado en la tapa", "altavoces perforados a ambos lados del teclado", "teclado sin bloque numérico con flechas de media altura", "touchpad grande", "base con gran rejilla de ventilación"],
    ["forma del logotipo simplificada", "touchpad con las medidas del Blade 16 de 2023, que comparte chasis", "tamaño de la perforación"], "blade24", ("logotipo", "medidas del touchpad", "altavoces", "rejillas"),
    lidT=6.4, deck="#1d1d1f", lid="#1c1c1e", wall="#202022", under="#161618", hinge="#0f0f11", rings=R_SHARP, rFront=4.5, rRear=2.5, rLidFree=4, rLidHinge=1.2, finish=METAL)
add([39, 40], blade26, ["unibody de aluminio negro más fino", "logotipo verde iluminado en la tapa", "altavoces perforados a ambos lados del teclado", "touchpad de 150 × 97 mm", "base con dos ventiladores y rejilla central"],
    ["forma del logotipo simplificada", "tamaño de la perforación", "zona elevada de la base: sin medidas"], "blade26", ("forma gráfica del logotipo", "altavoces"),
    lidT=5.0, deck="#1c1c1e", lid="#1b1b1d", wall="#1f1f21", under="#151517", hinge="#0f0f11", rings=R_SHARP, rFront=4.5, rRear=2.5, rLidFree=4, rLidHinge=1.2, finish=METAL)
add([33, 34], xps14, ["teclado de lado a lado sin separación", "touchpad sin marco, señalado por un grabado sutil", "fila de función física", "bisagras cortas junto a los extremos", "rótulo XPS en la tapa y DELL en la base", "ranura de ventilación trasera"],
    ["altavoces ocultos: no se dibujan", "tipografía de los rótulos"], "xps", ("forma gráfica del logotipo", "posición de altavoces"),
    lidT=4.1, deck="#303134", lid="#2f3033", wall="#34353a", under="#2a2b2e", hinge="#1b1c1f", lidEdge="#393a3f", rings=R_SHARP, rFront=5.5, rRear=2.5, rLidFree=5, rLidHinge=1.5, finish=METAL)
add([50], area51, ["tapa Liquid Teal con la cabeza de Alienware", "bloque térmico trasero muy profundo", "barra de luz trasera y touchpad iluminado", "rejillas ovaladas en las esquinas traseras", "rótulo ALIENWARE sobre el teclado", "ventana inferior de vidrio"],
    ["forma de la cabeza simplificada", "profundidad del bloque trasero, estimada", "forma exacta de la barra de luz"], "aw", ("logotipo de la tapa", "forma de la barra RGB", "forma de las rejillas"),
    lidT=7.0, deck="#15181b", lid="#1f444e", wall="#262c31", under="#191c20", lidEdge="#2a525d", hinge="#101316", rings=R_GAMER, rFront=13, rRear=5, rLidFree=7, rLidHinge=2, inset=38, finish=METAL)
add([35], g14, ["tapa con Slash como fila diagonal de LED", "altavoces perforados a los lados del teclado", "cuatro teclas de función sobre el teclado", "encendido arriba a la derecha", "base casi entera perforada"],
    ["color Eclipse Gray elegido entre los dos que se venden", "medidas del touchpad, estimadas", "recorrido exacto de Slash"], "g14", ("posición de Slash", "rejillas", "teclado numérico", "medidas del touchpad", "bisagra"),
    lidT=4.2, shell="#5c5f65", deck="#5a5d63", lid="#5c5f65", wall="#62656b", under="#4e5156", hinge="#27292d", rings=R_SHARP, rFront=5.5, rRear=2.5, rLidFree=5, rLidHinge=1.5, finish=METAL)
add([47], zenbook, ["cuerpo muy fino de Ceraluminum mate", "teclado del mismo tono que la cubierta", "touchpad grande, casi hasta el borde", "muesca delantera para abrir", "rótulo en el centro de la tapa"],
    ["tono gris de esta unidad, estimado", "patrón grabado exacto de la tapa", "lado del conector de audio: sin confirmar, no se dibuja"], "zen", ("medidas del touchpad", "bisagra"),
    lidT=3.5, shell="#5f6266", deck="#5f6266", lid="#5c5f63", wall="#65686c", under="#55585c", hinge="#34363a", rings=R_SOFT, rFront=6.5, rRear=3, rLidFree=6, rLidHinge=2, finish={"rough": 0.72, "spec": 0.1})
add([46], aspire, ["tapa metálica de poco brillo", "teclado con bloque numérico de teclas estrechas", "sin Ctrl derecho: en su lugar, tecla Copilot", "touchpad grande", "bisagra de 180 grados"],
    ["medidas del touchpad, estimadas", "tipografía del rótulo", "color gris, estimado"], "aspire", ("lado de cada puerto", "color y material", "bloque numérico y touchpad", "bisagra", "logotipo"),
    lidT=4.0, deck="#8e9196", lid="#8e9196", wall="#94979c", under="#7f8287", rings=R_SLAB, rFront=5.5, rRear=2.5, rLidFree=4, rLidHinge=1.5, finish=SOFT)
add([51], swift16, ["touchpad háptico enorme de 175,5 × 109,7 mm, solo con grabado", "teclado con bloque numérico", "bisagra de 180 grados", "chasis gris metálico con cuña"],
    ["tono exacto del gris", "tipografía del rótulo"], "swift", ("logotipo", "rejillas"),
    lidT=3.9, deck="#5c6068", lid="#5a5e66", wall="#62666e", under="#50545b", rings=R_SOFT, rFront=6, rRear=3, rLidFree=5, rLidHinge=1.5, finish=METAL)
add([45], prestige, ["convertible con dos bisagras de 360 grados a la vista", "cantos claros con acabado diamantado", "teclas oscuras y redondeadas", "banda perforada ancha en la base"],
    ["medidas del touchpad, estimadas", "posición del rótulo msi", "el giro completo de la pantalla no se simula"], "msi", ("medidas del touchpad", "logotipo de la tapa", "rejillas"),
    lidT=3.8, deck="#8b8882", lid="#8a8781", wall="#a9a69f", under="#7d7a74", lidEdge="#b5b2ab", hinge="#6f6c66", rings=R_SOFT, rFront=7, rRear=4, rLidFree=6.5, rLidHinge=3, finish=METAL)
add([38], omnibook, ["cuerpo de 10,8 mm en Eclipse Gray", "bordes cromados", "logotipo hp en bajorrelieve", "teclado gris oscuro", "touchpad háptico marcado solo por un grabado", "tres USB-C: dos a la derecha y uno a la izquierda, con audio"],
    ["medidas del touchpad, estimadas", "orden de los puertos dentro de cada lado"], "hp", ("lado de los tres USB-C", "color exacto", "teclado y touchpad", "bisagra", "logotipo"),
    lidT=3.0, deck="#6d7076", lid="#6d7076", wall="#c3c6cb", lidEdge="#c9ccd1", under="#62656a", hinge="#3d3f43", rings=R_SHARP, rFront=5.5, rRear=2.5, rLidFree=5.5, rLidHinge=1.5, finish=METAL)
add([43], gram, ["aleación Aerominum muy ligera", "teclado con bloque numérico, según la variante europea", "touchpad bajo las letras", "rótulo LG gram en la tapa"],
    ["bloque numérico: consta solo en la variante del Reino Unido", "medidas del touchpad, estimadas", "lados de los puertos sin confirmar: no se dibujan"], "lg", ("logotipo",),
    lidT=3.4, deck="#a9acb1", lid="#a9acb1", wall="#b1b4b9", under="#9a9da2", rings=R_SOFT, rFront=5.5, rRear=2.5, rLidFree=5, rLidHinge=1.5, finish=SOFT)
add([37], book6, ["teclado y touchpad centrados", "touchpad háptico grande sin botones", "rótulo SAMSUNG junto al borde libre de la tapa", "bisagra larga y fina", "cuatro pies redondos grandes con aro", "campo rectangular de perforaciones en la base"],
    ["medidas del touchpad, estimadas", "seis altavoces: sin rejillas visibles en la cubierta"], "book", ("logotipo de la tapa", "rejillas", "medidas del touchpad háptico"),
    lidT=4.2, deck="#3b3e45", lid="#3a3d44", wall="#454850", under="#33363c", hinge="#22242a", rings=R_SHARP, rFront=7, rRear=3.5, rLidFree=6, rLidHinge=2, finish=METAL)
add([55], neo, ["esquinas suaves de una pieza de aluminio", "teclado Magic Keyboard de 78 teclas en tono claro", "Touch ID arriba a la derecha", "touchpad de 115 × 72 mm", "logotipo centrado en la tapa", "cuatro pies redondos"],
    ["color Silver supuesto: se vende en cuatro", "forma del logotipo simplificada", "altavoces laterales: no se dibujan"], "neo", ("color de las teclas",),
    lidT=3.4, deck="#c9ccd0", lid="#c9ccd0", wall="#d0d3d7", under="#bfc2c6", hinge="#9b9ea3", rings=R_SOFT, rFront=8, rRear=4.5, rLidFree=8, rLidHinge=3, finish=METAL, keySide="#a9acb1")

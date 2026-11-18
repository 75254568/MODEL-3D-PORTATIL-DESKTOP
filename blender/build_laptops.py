"""
Genera en Blender el modelo 3D de cada laptop a partir de sus datos verificados
(src/main/resources/specs/verified/*.json) y lo exporta para el visor de la aplicacion.

Uso:
  blender --background --factory-startup --python blender/build_laptops.py -- [--only texto] [--blend]

Por laptop escribe en src/main/resources/laptops3d/<slug>/:
  body.obj / body.mtl   base, cubierta, teclas, touchpad, puertos y bisagras (+Z arriba, -Y delante, en mm)
  lid.obj  / lid.mtl    tapa cerrada, con origen en el eje de la bisagra (se extiende hacia -Y)
  meta.json             medidas, eje de la bisagra y que rasgos son verificados o supuestos

Regla: lo que no consta en las fuentes no se dibuja como si fuera cierto. Un puerto sin lado confirmado no se
coloca; un rasgo sin datos (logotipo, iluminacion, rejilla) no se inventa. Lo que hace falta para que exista el
modelo y no publica nadie (radios, grosor de tapa, marcos) se calcula con una regla comun y se anota en
meta.json como "assumed".
"""
import glob
import json
import math
import os
import re
import sys

import bmesh
import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPECS = os.path.join(ROOT, "src", "main", "resources", "specs", "verified")
OUT = os.path.join(ROOT, "src", "main", "resources", "laptops3d")

# Medidas de los conectores segun sus normas (ancho x alto de la abertura, mm).
PORT_SIZE = {
    "usba": (12.5, 4.6), "usbc": (8.9, 3.0), "hdmi": (14.0, 4.6), "minidp": (7.5, 4.6), "rj45": (16.0, 11.5),
    "audio": (3.6, 3.6), "sd": (24.5, 2.4), "microsd": (11.5, 1.2), "power": (11.0, 4.6), "magsafe": (16.5, 3.2),
    "kensington": (7.0, 3.2), "surface": (16.5, 2.6),
}
ROUND_PORTS = {"audio"}


def hex_rgb(h):
    """Color tal cual: el visor (JavaFX) trabaja directamente con estos valores, sin gestion de color lineal."""
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))


# ------------------------------------------------------------------ materiales

class Materials:
    def __init__(self):
        self.cache = {}

    def get(self, name, color, rough=0.5, metal=0.0, emit=False):
        m = self.cache.get(name)
        if m:
            return m
        m = bpy.data.materials.new(name)
        m.use_nodes = True
        bsdf = next(n for n in m.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
        rgb = hex_rgb(color)
        bsdf.inputs["Base Color"].default_value = (*rgb, 1)
        bsdf.inputs["Roughness"].default_value = rough
        # El MTL oscurece el color difuso de los materiales metalicos: el brillo del metal se da con el especular.
        bsdf.inputs["Metallic"].default_value = 0.0
        bsdf.inputs["Specular IOR Level"].default_value = 0.35 + 0.5 * metal
        if emit:
            bsdf.inputs["Emission Color"].default_value = (*rgb, 1)
            bsdf.inputs["Emission Strength"].default_value = 1.0
        m.diffuse_color = (*rgb, 1)
        self.cache[name] = m
        return m


# ------------------------------------------------------------------ geometria

def rounded_outline(w, d, r_front, r_rear, seg=8):
    """Contorno en planta, antihorario visto desde arriba, centrado en el origen (-Y es el frente)."""
    hw, hd = w / 2, d / 2
    pts = []
    corners = [(hw - r_front, -hd + r_front, r_front, -90), (hw - r_rear, hd - r_rear, r_rear, 0),
               (-hw + r_rear, hd - r_rear, r_rear, 90), (-hw + r_front, -hd + r_front, r_front, 180)]
    for cx, cy, r, start in corners:
        for i in range(seg + 1):
            a = math.radians(start + 90 * i / seg)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def inset(pts, amount):
    """Contorno desplazado hacia dentro (valido para contornos convexos como estos)."""
    n = len(pts)
    out = []
    for i, (x, y) in enumerate(pts):
        ax, ay = pts[i - 1]
        bx, by = pts[(i + 1) % n]
        tx, ty = bx - ax, by - ay
        ln = math.hypot(tx, ty) or 1
        out.append((x - ty / ln * amount, y + tx / ln * amount))
    return out


class Builder:
    """Acumula piezas (objetos con nombre de pieza del visor) y las coloca en una coleccion."""

    def __init__(self, label):
        self.label = label
        self.objects = []

    def mesh(self, part, material, verts, faces, smooth=False):
        me = bpy.data.meshes.new(f"{self.label}_{part}")
        me.from_pydata(verts, [], faces)
        me.update()
        me.materials.append(material)
        if smooth:
            me.polygons.foreach_set("use_smooth", [True] * len(me.polygons))
        ob = bpy.data.objects.new(part, me)
        bpy.context.scene.collection.objects.link(ob)
        self.objects.append(ob)
        return ob

    def slab(self, part, material, outline, z_bottom, z_top, bevel=0.5):
        """Solido de contorno dado; z_bottom y z_top son funciones de y (permiten la cuna). Cantos achaflanados."""
        rings = []
        inner = inset(outline, bevel)
        rings.append([(x, y, z_bottom(y)) for x, y in inner])
        rings.append([(x, y, z_bottom(y) + bevel) for x, y in outline])
        rings.append([(x, y, z_top(y) - bevel) for x, y in outline])
        rings.append([(x, y, z_top(y)) for x, y in inner])
        n = len(outline)
        verts = [v for ring in rings for v in ring]
        faces = [list(range(n - 1, -1, -1))]
        for r in range(len(rings) - 1):
            a, b = r * n, (r + 1) * n
            faces += [[a + i, a + (i + 1) % n, b + (i + 1) % n, b + i] for i in range(n)]
        faces.append([3 * n + i for i in range(n)])
        return self.mesh(part, material, verts, faces)

    def box(self, part, material, cx, cy, cz, sx, sy, sz, top_inset=0.0, tilt=0.0):
        """Caja centrada; top_inset estrecha la cara superior (teclas); tilt inclina en X siguiendo la cuna."""
        hx, hy, hz = sx / 2, sy / 2, sz / 2
        i = top_inset
        local = [(-hx, -hy, -hz), (hx, -hy, -hz), (hx, hy, -hz), (-hx, hy, -hz),
                 (-hx + i, -hy + i, hz), (hx - i, -hy + i, hz), (hx - i, hy - i, hz), (-hx + i, hy - i, hz)]
        verts = [(cx + x, cy + y, cz + z + y * tilt) for x, y, z in local]
        faces = [[3, 2, 1, 0], [4, 5, 6, 7], [0, 1, 5, 4], [1, 2, 6, 5], [2, 3, 7, 6], [3, 0, 4, 7]]
        return self.mesh(part, material, verts, faces)

    def quad(self, part, material, corners, uv=True):
        """Cuadrilatero con coordenadas de textura 0..1: esquinas en orden arriba-izq, arriba-der, abajo-der, abajo-izq."""
        ob = self.mesh(part, material, corners, [[3, 2, 1, 0]])
        if uv:
            layer = ob.data.uv_layers.new(name="UVMap")
            for loop, co in zip(ob.data.polygons[0].loop_indices, [(0, 0), (1, 0), (1, 1), (0, 1)]):
                layer.data[loop].uv = co
        return ob

    def cylinder_x(self, part, material, cx, cy, cz, radius, length, seg=16):
        verts, faces = [], []
        for side in (-1, 1):
            for i in range(seg):
                a = 2 * math.pi * i / seg
                verts.append((cx + side * length / 2, cy + radius * math.cos(a), cz + radius * math.sin(a)))
        faces.append(list(range(seg - 1, -1, -1)))
        faces.append([seg + i for i in range(seg)])
        faces += [[i, (i + 1) % seg, seg + (i + 1) % seg, seg + i] for i in range(seg)]
        return self.mesh(part, material, verts, faces, smooth=True)

    def text(self, part, material, string, x, y, z, size, rot_z=0.0, align="CENTER", flip=False):
        curve = bpy.data.curves.new(f"t_{part}", "FONT")
        curve.body = string
        curve.size = size
        curve.align_x = align
        curve.align_y = "CENTER"
        curve.resolution_u = 2
        temp = bpy.data.objects.new("t", curve)
        bpy.context.scene.collection.objects.link(temp)
        bpy.context.view_layer.update()
        me = bpy.data.meshes.new_from_object(temp)
        bpy.data.objects.remove(temp)
        bpy.data.curves.remove(curve)
        c, s = math.cos(rot_z), math.sin(rot_z)
        for v in me.vertices:
            vx = -v.co.x if flip else v.co.x
            vy = v.co.y
            v.co = (x + vx * c - vy * s, y + vx * s + vy * c, z)
        me.materials.clear()
        me.materials.append(material)
        ob = bpy.data.objects.new(part, me)
        bpy.context.scene.collection.objects.link(ob)
        self.objects.append(ob)
        return ob


# ------------------------------------------------------------------ teclado

ROWS = [
    [("Esc", 1)] + [(f"F{i}", 1) for i in range(1, 13)] + [("Del", 1), ("Pwr", 1)],
    [("`", 1)] + [(c, 1) for c in "1234567890-="] + [("Back", 2)],
    [("Tab", 1.5)] + [(c, 1) for c in "QWERTYUIOP[]"] + [("\\", 1.5)],
    [("Caps", 1.75)] + [(c, 1) for c in "ASDFGHJKL;'"] + [("Enter", 2.25)],
    [("Shift", 2.25)] + [(c, 1) for c in "ZXCVBNM,./"] + [("Shift", 1.75), ("^", 1)],
    [("Ctrl", 1.25), ("Fn", 1), ("Win", 1.25), ("Alt", 1.25), ("", 5.25), ("Alt", 1), ("Ctrl", 1), ("<", 1), ("v", 1), (">", 1)],
]
NUMPAD = [["Num", "/", "*", "-"], ["7", "8", "9", "+"], ["4", "5", "6", ""], ["1", "2", "3", "Ent"], ["0", "", ".", ""]]
# Magic Keyboard: 78 teclas, fila de funcion de altura completa y Touch ID en la esquina
ROWS_APPLE = [
    [("esc", 1.5)] + [(f"F{i}", 1) for i in range(1, 13)] + [("", 1.5)],
    [("`", 1)] + [(c, 1) for c in "1234567890-="] + [("delete", 1.5)],
    [("tab", 1.5)] + [(c, 1) for c in "QWERTYUIOP[]"] + [("\\", 1)],
    [("caps", 1.8)] + [(c, 1) for c in "ASDFGHJKL;'"] + [("return", 1.7)],
    [("shift", 2.3)] + [(c, 1) for c in "ZXCVBNM,./"] + [("shift", 2.2)],
    [("fn", 1), ("ctrl", 1), ("opt", 1), ("cmd", 1.25), ("", 5), ("cmd", 1.25), ("opt", 1), ("<", 1), ("^", 1), (">", 1)],
]


def build_keyboard(b, m, s, deck_z, slope):
    numpad = s["numpad"]
    cols = s["numpadCols"]
    apple = s["layout"] == "apple"
    rows = ROWS_APPLE if apple else ROWS
    fn_factor = 0.85 if apple else 0.62
    total_units = 15.0 + (0.35 + cols if numpad else 0.0)
    avail = s["W"] - (22 if numpad else 52)
    pitch = min(19.05, avail / total_units)
    # teclado sin separacion: las teclas casi se tocan y el hueco inferior no se ve
    gap = 0.35 if s["zeroLattice"] else max(1.2, pitch * 0.09)
    rows_h = pitch * (5 + fn_factor)
    x0 = -total_units * pitch / 2
    top_y = s["D"] / 2 - s["keyboardRearMm"]
    plate_w = total_units * pitch + 3
    b.box("deck", m["well"], 0, top_y - rows_h / 2, deck_z(top_y - rows_h / 2) + 0.05, plate_w, rows_h + 3, 0.1, tilt=slope)
    keys_top = 0.9
    for r, row in enumerate(rows):
        height = pitch * (fn_factor if r == 0 else 1.0)
        y = top_y - (0 if r == 0 else pitch * fn_factor + (r - 1) * pitch) - height / 2
        x = x0
        scale = 15.0 / sum(w for _, w in row)
        touch_row = r == 0 and s["fnTouch"]
        if touch_row:
            # fila de funcion tactil: una banda plana, sin teclas fisicas
            b.box("keys", m["touchRow"], 0, y, deck_z(y) + 0.14, 15.0 * pitch - gap, height - gap, 0.16, tilt=slope)
        for label, width in row:
            kw = width * scale * pitch
            cx, sx, sy = x + kw / 2, kw - gap, height - gap
            if touch_row:
                if label:
                    b.text("legend", m["legend"], label, cx, y, deck_z(y) + 0.25, pitch * 0.2)
                x += kw
                continue
            z = deck_z(y) + keys_top / 2 + 0.06
            b.box("keys", m["key"], cx, y, z, sx, sy, keys_top, top_inset=min(0.35, sy * 0.05), tilt=slope)
            if label:
                size = pitch * (0.40 if len(label) == 1 else 0.23)
                b.text("legend", m["legend"], label, cx, y, z + keys_top / 2 + y * slope * 0 + 0.03, size)
            x += kw
    if numpad:
        nx = x0 + 15.35 * pitch
        for r, row in enumerate(NUMPAD):
            y = top_y - pitch * 0.62 - r * pitch - pitch / 2
            for c, label in enumerate(row[:cols]):
                cx = nx + c * pitch + pitch / 2
                z = deck_z(y) + keys_top / 2 + 0.06
                b.box("keys", m["key"], cx, y, z, pitch - gap, pitch - gap, keys_top, top_inset=0.35, tilt=slope)
                if label:
                    b.text("legend", m["legend"], label, cx, y, z + keys_top / 2 + 0.03, pitch * (0.40 if len(label) == 1 else 0.23))
    if s["trackpoint"]:
        # entre G, H y B: frontera de las filas ASDF y ZXCV
        ty = top_y - pitch * fn_factor - 3 * pitch
        b.box("keys", m["trackpoint"], x0 + 6.75 * pitch, ty, deck_z(ty) + keys_top / 2 + 0.2, 2.4, 2.4, keys_top + 0.3, top_inset=0.25, tilt=slope)
    margin = (s["W"] - plate_w) / 2
    if s["speakers"] and margin >= 14:
        # rejillas de altavoz a ambos lados del teclado
        nx_dots, ny_dots = min(7, int((margin - 7) / 3.0)), int((rows_h - 4) / 3.0)
        for side in (-1, 1):
            cx = side * (plate_w / 2 + margin / 2)
            for ix in range(nx_dots):
                for iy in range(ny_dots):
                    px = cx + (ix - (nx_dots - 1) / 2) * 3.0
                    py = top_y - 2 - iy * 3.0 - 1.0
                    zd = deck_z(py) + 0.08
                    b.mesh("deck", m["vent"], [(px - 0.7, py - 0.7, zd), (px + 0.7, py - 0.7, zd), (px + 0.7, py + 0.7, zd), (px - 0.7, py + 0.7, zd)], [[0, 1, 2, 3]])
    return top_y - rows_h


# ------------------------------------------------------------------ puertos

def build_ports(b, m, s, z_of):
    """
    Coloca solo los puertos con lado confirmado. Las fuentes los enumeran mirando cada lateral de frente: en el
    izquierdo la lista empieza por detras y en el derecho por delante, asi que este se invierte. "gap" indica una
    separacion grande citada por la fuente (por ejemplo, un conector aislado hacia el frente).
    """
    placed = []
    usable = s["D"] - s["rearExt"]
    for side, sign in (("left", -1), ("right", 1)):
        names = list(s["ports"].get(side) or [])
        if not names:
            continue
        if side == "right":
            names.reverse()
        spacing = 5.0
        long_gap = s["D"] * 0.30
        widths = sum(long_gap if n == "gap" else PORT_SIZE[n][0] for n in names)
        if widths + spacing * (len(names) - 1) > usable * 0.86:
            spacing = max(1.5, (usable * 0.86 - widths) / max(1, len(names) - 1))
        length = widths + spacing * (len(names) - 1)
        # los conectores se agrupan hacia la parte trasera, donde esta la placa base
        y = min(s["D"] / 2 - s["rearExt"] - 18, length / 2 + s["D"] * 0.08 - s["rearExt"] / 2)
        x = sign * (s["W"] / 2 + 0.03)
        for name in names:
            if name == "gap":
                y -= long_gap
                continue
            sw, sh = PORT_SIZE[name]
            w = sw
            cy = y - sw / 2
            zc = z_of(cy) * 0.48
            hy, hz = sw / 2, min(sh / 2, z_of(cy) * 0.40)
            if name in ROUND_PORTS:
                verts = [(x, cy + hz * math.cos(2 * math.pi * i / 16), zc + hz * math.sin(2 * math.pi * i / 16)) for i in range(16)]
                b.mesh("ports", m["port"], verts, [list(range(16)) if sign > 0 else list(range(15, -1, -1))])
            else:
                c = [(x, cy - hy, zc - hz), (x, cy + hy, zc - hz), (x, cy + hy, zc + hz), (x, cy - hy, zc + hz)]
                b.mesh("ports", m["port"], c, [[0, 1, 2, 3] if sign > 0 else [3, 2, 1, 0]])
                if name in ("usba", "usbc", "hdmi", "rj45", "power"):
                    t = 0.28 * hz
                    inner = [(x + sign * 0.02, cy - hy * 0.76, zc - t), (x + sign * 0.02, cy + hy * 0.76, zc - t),
                             (x + sign * 0.02, cy + hy * 0.76, zc + t), (x + sign * 0.02, cy - hy * 0.76, zc + t)]
                    b.mesh("ports", m["tongue"], inner, [[0, 1, 2, 3] if sign > 0 else [3, 2, 1, 0]])
            placed.append({"side": side, "type": name})
            y -= w + spacing
    # cara trasera: las fuentes la enumeran mirandola de frente, es decir, del lado derecho del equipo al izquierdo
    names = list(s["ports"].get("rear") or [])
    if names:
        spacing = 6.0
        widths = sum(PORT_SIZE[n][0] for n in names)
        if widths + spacing * (len(names) - 1) > s["W"] * 0.8:
            spacing = max(2.0, (s["W"] * 0.8 - widths) / max(1, len(names) - 1))
        x = (widths + spacing * (len(names) - 1)) / 2
        face = s["D"] / 2 + 0.06
        rear_h = z_of(s["D"] / 2)
        for name in names:
            sw, sh = PORT_SIZE[name]
            cx = x - sw / 2
            zc, hz, hx = rear_h * 0.48, min(sh / 2, rear_h * 0.40), sw / 2
            b.mesh("ports", m["port"], [(cx - hx, face, zc - hz), (cx + hx, face, zc - hz), (cx + hx, face, zc + hz), (cx - hx, face, zc + hz)], [[3, 2, 1, 0]])
            if name in ("usba", "usbc", "hdmi", "rj45", "power"):
                t = 0.28 * hz
                b.mesh("ports", m["tongue"], [(cx - hx * 0.76, face + 0.02, zc - t), (cx + hx * 0.76, face + 0.02, zc - t),
                                              (cx + hx * 0.76, face + 0.02, zc + t), (cx - hx * 0.76, face + 0.02, zc + t)], [[3, 2, 1, 0]])
            placed.append({"side": "rear", "type": name})
            x -= sw + spacing
    return placed


# ------------------------------------------------------------------ laptop

def slug(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")[:70]


def aspect_of(res):
    match = re.match(r"(\d+)x(\d+)", res or "")
    return int(match.group(1)) / int(match.group(2)) if match else 1.6


def bright(hex_color):
    r, g, b = [int(hex_color.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)]
    return (r * 0.299 + g * 0.587 + b * 0.114) / 255


def resolve(entry, look):
    """Reune medidas y rasgos. Devuelve el dibujo y la lista de valores supuestos."""
    sp, de = entry["specs"], entry.get("design", {})
    assumed = []
    W, D = sp["widthMm"], sp["depthMm"]
    T = de.get("thicknessMaxMm") or sp["thicknessMm"]
    T_front = de.get("thicknessMinMm") or T
    lid_t = max(3.2, min(7.0, T * 0.26))
    assumed.append("grosor de la tapa")
    assumed.append("radios de las esquinas")
    assumed.append("marcos de la pantalla")
    inches = sp.get("screenInches") or 15.6
    aspect = aspect_of(sp.get("resolution"))
    panel_h = inches * 25.4 / math.sqrt(aspect ** 2 + 1)
    panel_w = panel_h * aspect
    keyboard = de.get("keyboard", {})
    numpad = keyboard.get("numpad")
    if numpad is None:
        numpad = False
        assumed.append("teclado sin bloque numérico (no consta)")
    tp = de.get("touchpad", {})
    tp_w = tp.get("widthMm")
    tp_d = tp.get("depthMm")
    if not tp_w:
        tp_w, tp_d = round(W * 0.34), round(D * 0.31)
        if not tp.get("invisible"):
            assumed.append("medidas del touchpad")
    ports = de.get("ports", {})
    if not de.get("portsOrderConfirmed") and any(ports.get(k) for k in ("left", "right", "rear")):
        assumed.append("orden de los puertos dentro de cada lado")
    if tp.get("buttons"):
        assumed.append("altura de los botones sobre el touchpad")
    if de.get("cameraBump"):
        assumed.append("forma de la barra de la cámara en la tapa")
    if de.get("speakerGrilles"):
        assumed.append("forma de las rejillas de altavoz")
    ext = de.get("rearExtensionMm") or 0.0
    return {
        "W": W, "D": D, "T": T, "T_front": T_front, "lid_t": lid_t, "panel_w": panel_w, "panel_h": panel_h,
        "numpad": numpad, "numpadCols": keyboard.get("numpadCols", 4), "layout": keyboard.get("layout", ""), "notch": bool(de.get("notch")),
        "zeroLattice": keyboard.get("style") == "zero-lattice", "fnTouch": keyboard.get("fnRow") == "touch", "trackpoint": bool(keyboard.get("trackpoint")),
        "tp_w": tp_w, "tp_d": tp_d, "tp_left": "izquierda" in tp.get("placement", ""), "tp_invisible": bool(tp.get("invisible")),
        "tp_buttons": tp.get("buttons", 0), "tp_lit": bool(tp.get("lit")), "palmGlass": de.get("palmRest") == "vidrio",
        "speakers": bool(de.get("speakerGrilles")), "cameraBump": bool(de.get("cameraBump")), "bezels": de.get("bezelsMm"),
        "bumpH": max(1.2, (sp.get("thicknessMm") or T) - T), "rearExt": ext,
        "ports": ports, "keyboardRearMm": (D - ext) * 0.075 + ext, "look": look, "lighting": " ".join(de.get("lighting", [])),
        "logo": look.get("logo", ""), "vents": " ".join(de.get("vents", [])),
    }, assumed


# Aspecto por modelo: color solo cuando la fuente lo nombra; rotulo solo si la fuente cita un logotipo.
LOOKS = {
    3: {"shell": "#17181b", "deck": "#101114", "key": "#0c0d10", "legend": "#ff3b6b", "logo": "ROG", "metal": 0.6},
    35: {"shell": "#4a4c52", "deck": "#4a4c52", "key": "#17181b", "legend": "#f2f4f7", "logo": "", "metal": 0.9},
    32: {"shell": "#d9dbe0", "deck": "#d9dbe0", "key": "#c6c8ce", "legend": "#3a3d44", "logo": "ROG", "metal": 0.9},
    17: {"shell": "#3f4146", "deck": "#3f4146", "key": "#16171a", "legend": "#f2f4f7", "logo": "ROG", "metal": 0.9},
    2: {"shell": "#8d9096", "deck": "#1c1d20", "key": "#121316", "legend": "#f2f4f7", "logo": "", "metal": 0.1},
    27: {"shell": "#27364a", "deck": "#27364a", "key": "#1a2433", "legend": "#e8edf4", "logo": "Zenbook", "metal": 0.8},
    47: {"shell": "#dfe0dc", "deck": "#dfe0dc", "key": "#cfd0cc", "legend": "#3a3c40", "logo": "Zenbook", "metal": 0.3},
}


def build_laptop(entry):
    import photo_designs
    import photo_models  # registra los disenos del catalogo
    if entry["id"] in photo_designs.DESIGNS:
        return photo_designs.build(entry)
    look = entry.get("look") or LOOKS.get(entry["id"])
    if look is None:
        print("SIN ASPECTO DEFINIDO:", entry["model"])
        return None
    if not all(entry["specs"].get(k) for k in ("widthMm", "depthMm", "thicknessMm")):
        print("SIN MEDIDAS VERIFICADAS:", entry["model"])
        return None
    s, assumed = resolve(entry, look)
    W, D, T, lid_t = s["W"], s["D"], s["T"], s["lid_t"]
    mats = Materials()
    m = {
        "shell": mats.get("Chasis", look["shell"], 0.42, look["metal"]),
        "deck": mats.get("Cubierta", look["deck"], 0.48, look["metal"]),
        "bottom": mats.get("Base", look["shell"], 0.6, look["metal"] * 0.6),
        "well": mats.get("Hueco_teclado", "#0a0b0d", 0.7),
        "key": mats.get("Teclas", look["key"], 0.55),
        "legend": mats.get("Rotulos", look["legend"], 0.5, emit=True),
        "pad": mats.get("Touchpad", look["deck"], 0.28, look["metal"] * 0.5),
        "padEdge": mats.get("Borde_touchpad", "#6d7076" if bright(look["deck"]) > 0.55 else "#08090b", 0.5),
        "port": mats.get("Puerto", "#050506", 0.8),
        "tongue": mats.get("Lengueta", "#3a3d42", 0.4, 0.5),
        "hinge": mats.get("Bisagra", "#1a1b1e", 0.45, 0.7),
        "foot": mats.get("Goma", "#0c0c0d", 0.9),
        "vent": mats.get("Rejilla", "#050506", 0.85),
        "bezel": mats.get("Marco", "#050507", 0.25),
        "screen": mats.get("Pantalla", "#0b1018", 0.08),
        "cam": mats.get("Camara", "#10131a", 0.1),
        "logo": mats.get("Luz_logo" if look.get("logoLit") else "Logotipo", look.get("logoColor") or ("#f2f4f7" if bright(look["shell"]) < 0.55 else "#50535a"), 0.3, 0.8, emit=bool(look.get("logoLit"))),
        "light": mats.get("Luz", "#ffffff", 0.4, emit=True),
        "touchRow": mats.get("Fila_tactil", "#08090b", 0.2, 0.3),
        "trackpoint": mats.get("TrackPoint", "#c8102e", 0.7),
        "glass": mats.get("Reposamanos_vidrio", look["deck"], 0.12, 0.9),
    }

    # --- cuerpo: cuna entre el grosor delantero y el trasero, descontada la tapa
    body_rear = T - lid_t
    body_front = max(s["T_front"] - lid_t, body_rear * 0.55)
    deck_z = lambda y: body_front + (body_rear - body_front) * (y + D / 2) / D
    slope = (body_rear - body_front) / D
    r_front, r_rear = min(12.0, W * 0.03), min(7.0, W * 0.02)
    outline = rounded_outline(W, D, r_front, r_rear)
    body = Builder("body")
    body.slab("base", m["shell"], outline, lambda y: 0.0, lambda y: deck_z(y) * 0.45)
    body.slab("deck", m["deck"], outline, lambda y: deck_z(y) * 0.45, deck_z, bevel=0.4)

    tp_bottom = build_keyboard(body, m, s, deck_z, slope)
    verified = []
    if s["fnTouch"]:
        verified.append("fila de función táctil")
    if s["zeroLattice"]:
        verified.append("teclado sin separación entre teclas")
    if s["trackpoint"]:
        verified.append("TrackPoint")
    if s["speakers"]:
        verified.append("altavoces a los lados del teclado")
    margin_front = -D / 2 + 9
    if s["palmGlass"]:
        verified.append("reposamanos de vidrio")
        py = (tp_bottom - 2 + (-D / 2 + 4)) / 2
        body.box("deck", m["glass"], 0, py, deck_z(py) + 0.03, W - 10, tp_bottom - 2 - (-D / 2 + 4), 0.06, tilt=slope)
    btn_h = 9.0 if s["tp_buttons"] else 0.0
    pad_top = tp_bottom - (btn_h + 2.0 if btn_h else 0.0)
    tp_h = min(s["tp_d"], pad_top - margin_front - 5)
    tp_y = margin_front + (pad_top - margin_front) / 2
    tp_x = -W * 0.06 if s["tp_left"] else 0.0
    if s["tp_invisible"]:
        verified.append("touchpad integrado sin límites visibles")
    else:
        body.box("deck", m["padEdge"], tp_x, tp_y, deck_z(tp_y) + 0.07, s["tp_w"] + 1.0, tp_h + 1.0, 0.14, tilt=slope)
        body.box("deck", m["pad"], tp_x, tp_y, deck_z(tp_y) + 0.13, s["tp_w"], tp_h, 0.26, tilt=slope)
    if btn_h:
        verified.append(f"{s['tp_buttons']} botones físicos sobre el touchpad")
        by = tp_bottom - 1.0 - btn_h / 2
        widths = (0.38, 0.24, 0.38)
        bx = tp_x - s["tp_w"] / 2
        for f in widths:
            bw = s["tp_w"] * f
            body.box("keys", m["key"], bx + bw / 2, by, deck_z(by) + 0.3, bw - 0.8, btn_h - 0.8, 0.6, top_inset=0.2, tilt=slope)
            bx += bw
    if s["tp_lit"]:
        verified.append("iluminación RGB del touchpad")
        hw, hh = s["tp_w"] / 2 + 0.9, tp_h / 2 + 0.9
        zl = deck_z(tp_y) + 0.3
        for cx, cy, sx, sy in ((tp_x, tp_y + hh, hw * 2, 0.7), (tp_x, tp_y - hh, hw * 2, 0.7), (tp_x - hw, tp_y, 0.7, hh * 2), (tp_x + hw, tp_y, 0.7, hh * 2)):
            body.box("deck", m["light"], cx, cy, zl, sx, sy, 0.06, tilt=slope)

    placed = build_ports(body, m, s, deck_z)

    # patas de goma y rejilla inferior: las patas son comunes a todos los portatiles; su posicion exacta no consta
    for fx in (-W / 2 + 26, W / 2 - 26):
        for fy in (-D / 2 + 22, D / 2 - 22):
            body.box("base", m["foot"], fx, fy, -0.5, 30, 6, 1.0)

    # bisagra: eje en el borde trasero, a la altura de la cubierta
    hinge_y, hinge_z = D / 2 - s["rearExt"] - 1.5, body_rear
    for hx in (-W * 0.30, W * 0.30):
        body.cylinder_x("hinge", m["hinge"], hx, hinge_y, hinge_z - lid_t * 0.05, lid_t * 0.42, W * 0.16)
    if s["rearExt"]:
        verified.append("módulo trasero que sobresale de la bisagra")
    if "Barra RGB trasera" in s["lighting"]:
        verified.append("barra RGB trasera (forma aproximada)")
        body.box("base", m["light"], 0, D / 2 + 0.03, body_rear * 0.88, W * 0.46, 0.05, 1.2)
    if "Salida trasera" in s["vents"]:
        verified.append("salida de aire trasera")
        for i in range(36):
            vx = -W * 0.36 + i * W * 0.72 / 35
            body.mesh("base", m["vent"], [(vx - 1.8, D / 2 + 0.03, body_rear * 0.18), (vx + 1.8, D / 2 + 0.03, body_rear * 0.18),
                                          (vx + 1.8, D / 2 + 0.03, body_rear * 0.72), (vx - 1.8, D / 2 + 0.03, body_rear * 0.72)], [[3, 2, 1, 0]])
    if "Tira LED perimetral" in s["lighting"]:
        verified.append("tira LED inferior")
        band = inset(rounded_outline(W, D, r_front, r_rear), 0.05)
        body.slab("base", m["light"], band, lambda y: 0.45, lambda y: 1.25, bevel=0.0)

    # --- tapa cerrada, con el eje de la bisagra en el origen: se extiende hacia -Y
    L = D - s["rearExt"] - 1.5
    lid = Builder("lid")
    lid_outline = [(x, y - L / 2) for x, y in rounded_outline(W, L, r_front, 3.0)]
    lid.slab("lid", m["shell"], lid_outline, lambda y: 0.0, lambda y: lid_t, bevel=0.4)
    border_x = (W - s["panel_w"]) / 2
    top_border = s["bezels"]["top"] if s["bezels"] else max(3.5, border_x)
    if s["bezels"]:
        verified.append("marcos de pantalla medidos por el fabricante")
        assumed.remove("marcos de la pantalla")
    panel_top = -L + top_border
    panel_bottom = panel_top + s["panel_h"]
    if panel_bottom > -3:
        panel_bottom, panel_top = -3, -3 - s["panel_h"]
    z = -0.06
    lid.quad("bezel", m["bezel"], [(-W / 2 + 1.5, -L + 1.5, z), (W / 2 - 1.5, -L + 1.5, z), (W / 2 - 1.5, -1.5, z), (-W / 2 + 1.5, -1.5, z)], uv=False)
    z2 = -0.12
    hw = s["panel_w"] / 2
    lid.quad("display", m["screen"], [(-hw, panel_top, z2), (hw, panel_top, z2), (hw, panel_bottom, z2), (-hw, panel_bottom, z2)])
    cam_y = -L + top_border / 2
    if s["notch"]:
        assumed.append("tamaño de la muesca de la cámara")
        nw, nh = s["panel_w"] * 0.13, top_border * 0.2 + 5.0
        z3 = -0.16
        lid.quad("bezel", m["bezel"], [(-nw / 2, panel_top - 0.5, z3), (nw / 2, panel_top - 0.5, z3), (nw / 2, panel_top + nh, z3), (-nw / 2, panel_top + nh, z3)], uv=False)
        cam_y = panel_top + nh * 0.4
        lid.box("bezel", m["cam"], 0, cam_y, -0.2, 1.8, 1.8, 0.04)
    else:
        lid.box("bezel", m["cam"], 0, cam_y, -0.1, 1.8, 1.8, 0.04)

    outer = lid_t + 0.04
    if s["cameraBump"]:
        verified.append("realce de la cámara en la tapa")
        lid.box("lid", m["shell"], 0, -L + 6.5, lid_t + s["bumpH"] / 2 - 0.05, W * 0.17, 9.0, s["bumpH"] + 0.1, top_inset=0.4)
    if look["logo"]:
        verified.append("rótulo de la tapa (solo texto; el logotipo gráfico oficial no se reproduce)")
        # visto desde detras con la tapa abierta: el texto se lee con la parte alta hacia el borde libre
        lid.text("lid", m["logo"], look["logo"], 0, -L * 0.52, outer, W * (0.06 if len(look["logo"]) < 4 else 0.035), rot_z=math.pi, flip=False)
    if "Slash Lighting: tira diagonal" in s["lighting"]:
        verified.append("Slash: tira diagonal de 7 zonas (posición aproximada)")
        for i in range(7):
            t = (i + 0.5) / 7
            cx, cy = W * (0.30 - 0.60 * t), -L * (0.78 - 0.52 * t)
            ang = math.atan2(0.52 * L, 0.60 * W)
            lx, ly = W * 0.036, 1.3
            c, sn = math.cos(ang), math.sin(ang)
            pts = [(-lx, -ly), (lx, -ly), (lx, ly), (-lx, ly)]
            lid.mesh("lid", m["light"], [(cx + x * c + y * sn, cy - x * sn + y * c, outer) for x, y in pts], [[0, 1, 2, 3]])
    elif "Slash" in s["lighting"]:
        assumed.append("forma y posición de Slash: no se dibuja")
    if "AniMe Vision" in s["lighting"]:
        verified.append("AniMe Vision: matriz de puntos en la tapa (cantidad reducida para el visor)")
        for ix in range(36):
            for iy in range(20):
                px = -W * 0.30 + ix * W * 0.60 / 35
                py = -L * 0.20 - iy * L * 0.42 / 19
                lid.mesh("lid", m["light"], [(px - 0.7, py - 0.7, outer), (px + 0.7, py - 0.7, outer), (px + 0.7, py + 0.7, outer), (px - 0.7, py + 0.7, outer)], [[0, 1, 2, 3]])

    folder = os.path.join(OUT, slug(entry["brand"] + "-" + entry["model"]))
    os.makedirs(folder, exist_ok=True)
    for name, builder in (("body", body), ("lid", lid)):
        merged = merge_by_part(builder)
        bpy.ops.object.select_all(action="DESELECT")
        for ob in merged:
            ob.select_set(True)
        bpy.context.view_layer.objects.active = merged[0]
        bpy.ops.wm.obj_export(filepath=os.path.join(folder, name + ".obj"), export_selected_objects=True, forward_axis="Y", up_axis="Z",
                              export_uv=True, export_normals=True, export_materials=True, export_triangulated_mesh=True, path_mode="STRIP")
    meta = {
        "brand": entry["brand"], "model": entry["model"], "widthMm": W, "depthMm": D, "thicknessMm": T, "thicknessFrontMm": s["T_front"],
        "bodyRearMm": round(body_rear, 3), "bodyFrontMm": round(body_front, 3), "lidThicknessMm": round(lid_t, 3), "lidLengthMm": L,
        "hingeYMm": hinge_y, "hingeZMm": round(hinge_z, 3), "maxLidDeg": entry.get("design", {}).get("hingeMaxDeg"),
        "panel": {"widthMm": round(s["panel_w"], 2), "heightMm": round(s["panel_h"], 2)},
        "portsPlaced": placed, "portsNotPlaced": entry.get("design", {}).get("portsUnplaced", []),
        "verifiedFeatures": verified, "assumed": assumed, "notVerified": entry.get("design", {}).get("notVerified", []),
        "sources": entry.get("sources", []),
    }
    with open(os.path.join(folder, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=1)
    return os.path.basename(folder)


def merge_by_part(builder):
    """Une los objetos de una misma pieza y material: menos nodos en el visor y OBJ mas pequenos."""
    groups = {}
    for ob in builder.objects:
        groups.setdefault((ob.name.split(".")[0], ob.data.materials[0].name), []).append(ob)
    out = []
    for (part, _), obs in groups.items():
        bm = bmesh.new()
        for ob in obs:
            bm.from_mesh(ob.data)
        me = bpy.data.meshes.new(part)
        bm.to_mesh(me)
        bm.free()
        me.materials.append(obs[0].data.materials[0])
        for p in me.polygons:
            p.material_index = 0
        keep = bpy.data.objects.new(part, me)
        bpy.context.scene.collection.objects.link(keep)
        for ob in obs:
            bpy.data.objects.remove(ob)
        out.append(keep)
    return out


def clear_scene():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob)
    for coll in (bpy.data.meshes, bpy.data.materials, bpy.data.curves):
        for item in list(coll):
            coll.remove(item)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    only = argv[argv.index("--only") + 1].lower() if "--only" in argv else None
    index = []
    for path in sorted(glob.glob(os.path.join(SPECS, "*.json"))):
        with open(path, encoding="utf-8") as f:
            for entry in json.load(f):
                name = entry["brand"] + " " + entry["model"]
                if only and only not in name.lower():
                    continue
                clear_scene()
                folder = build_laptop(entry)
                if folder:
                    index.append({"brand": entry["brand"], "model": entry["model"], "folder": folder})
                    print("MODELO", folder)
                    if "--blend" in argv:
                        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(OUT, folder, "model.blend"))
    if not only:
        with open(os.path.join(OUT, "index.json"), "w", encoding="utf-8") as f:
            json.dump(index, f, ensure_ascii=False, indent=1)
    print("TOTAL", len(index))


if __name__ == "__main__":
    main()

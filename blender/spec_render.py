"""
Texturas de los modelos sin fotos, obtenidas del propio modelo.

JavaFX no calcula sombras ni oclusion, asi que un modelo de colores planos parece una maqueta. Aqui cada pieza se renderiza
con Cycles en vistas ortograficas (cubierta, tapa, base y cuatro lados) con oclusion ambiental y aristas resaltadas, y esas
imagenes se proyectan de vuelta sobre su geometria. Tambien se genera, por procedimiento, la pantalla de cada equipo: panel
con su tamano real, marcos y un fondo original. Ninguna imagen procede de fotografias.
"""
import math
import os
import zlib

import bpy
import numpy as np

import build_laptops as core
import photo_designs as pd

DROP = {"Rejilla", "Rejilla_base", "Rotulos", "Rotulo_cubierta", "Rotulo_base", "Grabado_touchpad", "Borde_touchpad", "Linea_botones", "Bandeja",
        "Marco_teclado", "Muesca", "Puerto", "Lengueta", "Aleta", "Ojos_logo", "Punto_rojo", "Touchpad", "Luz_touchpad", "Luz_trasera", "Ventana_inferior", "Touch_ID"}
DROP_PREFIX = ("Logo", "Luz_logo", "Luz_slash", "Luz_tecla", "Luz_encendido")
SOLID = {"Goma", "Aro_pie", "Bisagra", "Barra_camara", "Barra_lado", "TrackPoint", "TrackPoint_lado"}
BRUSHED = {"Cubierta", "Tapa_lisa", "Canto_liso", "Canto_tapa_liso", "Base", "Bloque_trasero"}


def linear(c):
    return tuple(max(0.0, v) ** 2.2 for v in c[:3])


# ------------------------------------------------------------------ dispositivo y escena

def setup_device(scene):
    scene.render.engine = "CYCLES"
    prefs = bpy.context.preferences.addons["cycles"].preferences
    for kind in ("OPTIX", "CUDA"):
        try:
            prefs.compute_device_type = kind
            prefs.get_devices()
            gpus = [d for d in prefs.devices if d.type == kind]
            if gpus:
                for d in prefs.devices:
                    d.use = d.type == kind
                scene.cycles.device = "GPU"
                return kind
        except Exception:
            pass
    scene.cycles.device = "CPU"
    return "CPU"


def make_scene():
    sc = bpy.data.scenes.new("hornear")
    kind = setup_device(sc)
    c = sc.cycles
    c.samples = 48
    c.use_adaptive_sampling = False
    c.use_denoising = True
    c.denoiser = "OPENIMAGEDENOISE"
    c.max_bounces = 0
    c.diffuse_bounces = c.glossy_bounces = c.transmission_bounces = c.volume_bounces = c.transparent_max_bounces = 0
    c.caustics_reflective = c.caustics_refractive = False
    c.filter_width = 1.1
    w = bpy.data.worlds.new("negro")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs[0].default_value = (0, 0, 0, 1)
    sc.world = w
    r = sc.render
    r.resolution_percentage = 100
    r.image_settings.file_format = "JPEG"
    r.image_settings.quality = 90
    r.image_settings.color_mode = "RGB"
    r.use_file_extension = False
    sc.view_settings.view_transform = "Standard"
    sc.view_settings.look = "None"
    sc.display_settings.display_device = "sRGB"
    return sc, kind


def bake_material(src, finish):
    """Emision con el color del material, oscurecida en los huecos y aclarada en las aristas."""
    name = src.name
    rgb = linear(src.diffuse_color)
    m = bpy.data.materials.new("R_" + name)
    m.use_nodes = True
    t = m.node_tree
    t.nodes.clear()
    out = t.nodes.new("ShaderNodeOutputMaterial")
    em = t.nodes.new("ShaderNodeEmission")
    t.links.new(em.outputs[0], out.inputs["Surface"])
    em.inputs["Color"].default_value = (*rgb, 1)
    if name.startswith("Luz"):
        em.inputs["Strength"].default_value = 1.15
        return m

    def math_node(op, a, b=None):
        n = t.nodes.new("ShaderNodeMath")
        n.operation = op
        for i, v in enumerate((a, b)):
            if v is None:
                continue
            if isinstance(v, (int, float)):
                n.inputs[i].default_value = v
            else:
                t.links.new(v, n.inputs[i])
        return n.outputs[0]

    ao = t.nodes.new("ShaderNodeAmbientOcclusion")
    ao.samples = 8
    ao.inputs["Distance"].default_value = 3.2
    bev = t.nodes.new("ShaderNodeBevel")
    bev.samples = 4
    bev.inputs["Radius"].default_value = 0.2
    geo = t.nodes.new("ShaderNodeNewGeometry")
    light = (-0.36, -0.42, 0.83)
    dots = []
    for src_normal in (bev.outputs[0], geo.outputs["True Normal"]):
        d = t.nodes.new("ShaderNodeVectorMath")
        d.operation = "DOT_PRODUCT"
        t.links.new(src_normal, d.inputs[0])
        d.inputs[1].default_value = light
        dots.append(d.outputs["Value"])
    same = t.nodes.new("ShaderNodeVectorMath")
    same.operation = "DOT_PRODUCT"
    t.links.new(bev.outputs[0], same.inputs[0])
    t.links.new(geo.outputs["True Normal"], same.inputs[1])
    # relieve direccional: la arista que mira a la luz se aclara y la opuesta se oscurece
    relief = math_node("ADD", 1.0, math_node("MULTIPLY", math_node("SUBTRACT", dots[0], dots[1]), 1.5))
    rim = math_node("MULTIPLY", math_node("SUBTRACT", 1.0, same.outputs["Value"]), 0.55)
    shade = math_node("MULTIPLY", math_node("ADD", 0.36, math_node("MULTIPLY", ao.outputs["AO"] if "AO" in ao.outputs else ao.outputs[1], 0.64)), relief)
    if name in BRUSHED:
        tc = t.nodes.new("ShaderNodeTexCoord")
        noise = t.nodes.new("ShaderNodeTexNoise")
        noise.inputs["Scale"].default_value = finish.get("grain", 7.0)
        noise.inputs["Detail"].default_value = 3.0
        t.links.new(tc.outputs["Object"], noise.inputs["Vector"])
        shade = math_node("MULTIPLY", shade, math_node("ADD", 0.975, math_node("MULTIPLY", noise.outputs[0], 0.05)))
        if finish.get("weave") and name == "Tapa_lisa":
            ch = t.nodes.new("ShaderNodeTexChecker")
            ch.inputs["Scale"].default_value = finish["weave"]
            ch.inputs["Color1"].default_value = (1, 1, 1, 1)
            ch.inputs["Color2"].default_value = (0.74, 0.74, 0.74, 1)
            t.links.new(tc.outputs["Object"], ch.inputs["Vector"])
            shade = math_node("MULTIPLY", shade, ch.outputs["Fac"] if False else ch.outputs[0])
    tint = t.nodes.new("ShaderNodeMix")
    tint.data_type = "RGBA"
    tint.blend_type = "MULTIPLY"
    tint.inputs["Factor"].default_value = 1.0
    tint.inputs["A"].default_value = (*rgb, 1)
    conv = t.nodes.new("ShaderNodeCombineColor")
    for i in range(3):
        t.links.new(shade, conv.inputs[i])
    t.links.new(conv.outputs[0], tint.inputs["B"])
    add = t.nodes.new("ShaderNodeMix")
    add.data_type = "RGBA"
    add.blend_type = "ADD"
    add.inputs["Factor"].default_value = 1.0
    t.links.new(tint.outputs["Result"], add.inputs["A"])
    glow = t.nodes.new("ShaderNodeCombineColor")
    k = math_node("MULTIPLY", rim, 0.16 + 0.5 * max(rgb))
    for i in range(3):
        t.links.new(k, glow.inputs[i])
    t.links.new(glow.outputs[0], add.inputs["B"])
    t.links.new(add.outputs["Result"], em.inputs["Color"])
    return m


# ------------------------------------------------------------------ vistas

class Baker:
    def __init__(self, design, folder):
        self.v, self.folder = design, folder
        self.scene, self.kind = make_scene()
        self.cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
        self.cam.data.type = "ORTHO"
        self.cam.data.clip_start = 0.5
        self.cam.data.clip_end = 3000
        self.scene.collection.objects.link(self.cam)
        self.scene.camera = self.cam
        self.mats = {}
        finish = design.c.get("finish", {})
        self.groups = {}
        for key, mesher in (("body", design.body), ("lid", design.lid)):
            coll = bpy.data.collections.new("h_" + key)
            self.scene.collection.children.link(coll)
            for ob in mesher.objects(coll):
                src = ob.data.materials[0]
                ob.data.materials[0] = self.mats.setdefault(src.name, bake_material(src, finish))
                if ob.name.startswith("display"):
                    ob.hide_render = True
            self.groups[key] = coll

    def shot(self, name, show, loc, rot, fw, fh, ppm):
        for key, coll in self.groups.items():
            coll.hide_render = key != show
        r = self.scene.render
        r.resolution_x, r.resolution_y = max(4, round(fw * ppm)), max(4, round(fh * ppm))
        self.cam.data.ortho_scale = max(fw, fh)
        self.cam.location = loc
        self.cam.rotation_euler = rot
        path = os.path.join(self.folder, name + ".jpg")
        # SPEC_REUSE=1 conserva las vistas ya calculadas (para retocar solo la pantalla o los materiales)
        if os.environ.get("SPEC_REUSE") and os.path.isfile(path):
            return path
        r.filepath = path
        bpy.ops.render.render(write_still=True, scene=self.scene.name)
        return path

    def close(self):
        for coll in self.groups.values():
            for ob in list(coll.objects):
                me = ob.data
                bpy.data.objects.remove(ob)
                bpy.data.meshes.remove(me)
            bpy.data.collections.remove(coll)
        cam = self.cam.data
        bpy.data.objects.remove(self.cam)
        bpy.data.cameras.remove(cam)
        world = self.scene.world
        bpy.data.scenes.remove(self.scene)
        bpy.data.worlds.remove(world)
        for m in self.mats.values():
            bpy.data.materials.remove(m)


def newell(P):
    nx = ny = nz = 0.0
    for i in range(len(P)):
        x0, y0, z0 = P[i]
        x1, y1, z1 = P[(i + 1) % len(P)]
        nx += (y0 - y1) * (z0 + z1)
        ny += (z0 - z1) * (x0 + x1)
        nz += (x0 - x1) * (y0 + y1)
    n = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
    return nx / n, ny / n, nz / n


def bake_design(v, kit, folder):
    W, D, L = v.W, v.D, v.L
    top = v.hR + v.c["lidT"] + 1.0
    b = Baker(v, folder)
    pi = math.pi
    deck_ppm = 2400 / W
    zc, zh = (top - 2.0) / 2, top + 2.0
    lid_y0, lid_y1 = -L - 4.0, 1.0
    paths = {
        "deck": b.shot("deck", "body", (0, 0, 400), (0, 0, 0), W, D, deck_ppm),
        "lid": b.shot("lid", "lid", (0, (lid_y0 + lid_y1) / 2, 400), (0, 0, 0), W, lid_y1 - lid_y0, 1800 / W),
        "bottom": b.shot("bottom", "body", (0, 0, -400), (pi, 0, 0), W, D, 1800 / W),
        "left": b.shot("left", "body", (-600, 0, zc), (pi / 2, 0, -pi / 2), D, zh, 6.0),
        "right": b.shot("right", "body", (600, 0, zc), (pi / 2, 0, pi / 2), D, zh, 6.0),
        "rear": b.shot("rear", "body", (0, 600, zc), (pi / 2, 0, pi), W, zh, 6.0),
        "front": b.shot("front", "body", (0, -600, zc), (pi / 2, 0, 0), W, zh, 6.0),
    }
    b.close()
    z0 = zc - zh / 2
    def cl(u):
        return min(0.999, max(0.001, u))
    uv = {
        "deck": lambda p: (cl((p[0] + W / 2) / W), cl((p[1] + D / 2) / D)),
        "bottom": lambda p: (cl((p[0] + W / 2) / W), cl((D / 2 - p[1]) / D)),
        "left": lambda p: (cl((D / 2 - p[1]) / D), cl((p[2] - z0) / zh)),
        "right": lambda p: (cl((p[1] + D / 2) / D), cl((p[2] - z0) / zh)),
        "rear": lambda p: (cl((W / 2 - p[0]) / W), cl((p[2] - z0) / zh)),
        "front": lambda p: (cl((p[0] + W / 2) / W), cl((p[2] - z0) / zh)),
        "lid": lambda p: (cl((p[0] + W / 2) / W), cl((p[1] - lid_y0) / (lid_y1 - lid_y0))),
    }
    fin = v.c.get("finish", {})
    rough, spec = fin.get("rough", 0.46), fin.get("spec", 0.3)
    names = {"deck": "Foto_cubierta", "lid": "Foto_tapa", "bottom": "Foto_base", "left": "Foto_izquierda", "right": "Foto_derecha", "rear": "Foto_trasera", "front": "Foto_frente"}
    mats = {}
    for key, path in paths.items():
        base = os.path.basename(path)
        v.ph[key] = pd.Photo(base, path, base)
        mats[key] = kit.photo(names[key], v.ph[key], rough, spec)
    # las luces del visor se suman a la textura: las superficies claras se atenuan para que conserven su detalle
    bright = max(core.hex_rgb(v.c["deck"]))
    gain = min(1.0, 0.55 / max(bright, 0.01))
    v.c["photoGains"] = {n: gain for n in names.values()}

    body = pd.Mesher()
    for (part, name), (mat, verts, faces, _) in v.body.groups.items():
        if part == "legend" or name in DROP or name.startswith(DROP_PREFIX):
            continue
        for f in faces:
            P = [verts[i] for i in f]
            if part == "hinge" or name in SOLID:
                body.poly(part, mat, P)
                continue
            nx, ny, nz = newell(P)
            view = "deck"
            if part == "base" or name == "Bloque_trasero":
                if nz < -0.6:
                    view = "bottom"
                elif nz <= 0.6:
                    if abs(nx) >= abs(ny):
                        view = "left" if nx < 0 else "right"
                    elif ny > 0:
                        view = "rear"
                    elif name != "Bloque_trasero":
                        view = "front"
            body.poly(part, mats[view], P, [uv[view](p) for p in P])
    v.body = body

    lid = pd.Mesher()
    for (part, name), (mat, verts, faces, _) in v.lid.groups.items():
        if name.startswith(DROP_PREFIX) or name in DROP:
            continue
        for f in faces:
            P = [verts[i] for i in f]
            if part != "lid" or name in SOLID or name == "Canto_tapa_liso":
                lid.poly(part, mat, P)
            else:
                lid.poly(part, mats["lid"], P, [uv["lid"](p) for p in P])
    v.lid = lid
    v.notes.append("texturas calculadas con un render del propio modelo: no son fotografías del equipo")


# ------------------------------------------------------------------ pantalla

def rgb_of(h):
    return np.array(core.hex_rgb(h), dtype=np.float32)


def ramp(t, colors):
    pts = np.stack([rgb_of(c) for c in colors])
    x = np.clip(t, 0, 1) * (len(pts) - 1)
    i = np.minimum(x.astype(int), len(pts) - 2)
    f = (x - i)[..., None]
    f = f * f * (3 - 2 * f)
    return pts[i] * (1 - f) + pts[i + 1] * f


def fbm(u, v, rng, octaves=5, base=2.0):
    out = np.zeros_like(u)
    amp, freq, total = 1.0, base, 0.0
    for _ in range(octaves):
        a, b, p, q = rng.uniform(-1, 1), rng.uniform(-1, 1), rng.uniform(0, 6.28), rng.uniform(0, 6.28)
        uu = u * math.cos(a * 3) - v * math.sin(a * 3)
        vv = u * math.sin(b * 3) + v * math.cos(b * 3)
        out = out + amp * np.sin(freq * uu * 6.28 + p + 1.4 * np.sin(freq * vv * 3.14 + q))
        total += amp
        amp *= 0.55
        freq *= 1.9
    return out / total


def wallpaper(w, h, theme, seed):
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    aspect = w / h
    u, v = x / w * aspect, y / h
    style, c = theme["style"], theme["colors"]
    warp = fbm(u, v, rng, 4, 0.9)
    if style == "ribbons":
        img = ramp(v * 0.75 + 0.12 * warp + 0.1, c[:3])
        for i in range(8):
            f, p = rng.uniform(0.5, 1.2), rng.uniform(0, 6.28)
            yc = 0.2 + i * 0.085 + 0.12 * np.sin(u * f * 6.28 + p) + 0.06 * warp
            s = rng.uniform(0.02, 0.07)
            d = (v - yc) / s
            band = np.exp(-d * d) * (0.65 + 0.35 * np.tanh(d * 2.0))
            img = img + rgb_of(c[3 + i % (len(c) - 3)]) * band[..., None] * 0.62
    elif style == "bloom":
        cx, cy = aspect * rng.uniform(0.45, 0.6), rng.uniform(0.55, 0.68)
        dx, dy = u - cx, v - cy
        r, a = np.hypot(dx, dy), np.arctan2(dy, dx)
        img = ramp(np.clip(r * 1.1 + 0.08 * warp, 0, 1), c[:3])
        core_fade = np.clip((r - 0.05) / 0.2, 0, 1)
        core_fade = core_fade * core_fade * (3 - 2 * core_fade)
        for k, n in enumerate((3, 4, 5)):
            petal = np.cos(n * a + 4.5 * r + k * 1.7 + 1.2 * warp)
            mask = np.clip(petal, 0, 1) ** (3 + k) * np.exp(-(r / (0.8 - k * 0.14)) ** 2) * core_fade
            img = img + rgb_of(c[3 + k % (len(c) - 3)]) * mask[..., None] * 0.72
        img = img + rgb_of(c[3]) * np.exp(-(r / 0.16) ** 2)[..., None] * 0.42
    elif style == "rings":
        cx, cy = aspect * 0.5, 0.5
        r = np.hypot(u - cx, v - cy) + 0.03 * warp
        img = ramp(np.clip(r * 1.3, 0, 1), c[:2])
        rings = np.sin(r * 46 + 3 * warp) ** 12 * np.exp(-r * 1.9)
        halo = np.exp(-((r - 0.24) / 0.06) ** 2)
        img = img + rgb_of(c[2]) * (rings * 0.8 + halo * 0.55)[..., None] + rgb_of(c[3]) * (np.exp(-((r - 0.42) / 0.03) ** 2) * 0.35)[..., None]
    elif style == "nebula":
        t = 0.5 + 0.5 * fbm(u, v, rng, 6, 1.1)
        t2 = 0.5 + 0.5 * fbm(u + 0.3 * warp, v, rng, 6, 1.7)
        img = ramp(t ** 1.6, c[:3]) + rgb_of(c[3]) * (t2 ** 5)[..., None] * 1.15
        n = int(w * h / 1400)
        px, py = rng.integers(0, w, n), rng.integers(0, h, n)
        img[py, px] += rng.uniform(0.25, 0.9, (n, 1))
    elif style == "hills":
        img = ramp(v * 1.25 + 0.04 * warp, c[:3])
        sx, sy = aspect * rng.uniform(0.3, 0.7), 0.42
        img = img + rgb_of(c[2]) * np.exp(-(((u - sx) ** 2 + (v - sy) ** 2) / 0.05))[..., None] * 0.55
        for i in range(5):
            p, f = rng.uniform(0, 6.28), rng.uniform(0.7, 1.5)
            ridge = 0.5 + i * 0.1 + 0.05 * np.sin(u * f * 6.28 + p) + 0.022 * fbm(u + i, v * 0, rng, 4, 2.5)
            m = np.clip((v - ridge) * h / 2.0, 0, 1)[..., None]
            col = ramp(np.full_like(v, i / 4.0), c[3:]) * (1.0 + 0.3 * np.clip(ridge + 0.1 - v, -0.3, 0)[..., None])
            img = img * (1 - m) + col * m
    elif style == "slash":
        s = (u * 0.72 + v * 0.69)
        img = ramp(np.clip(v * 0.8 + 0.1 * warp, 0, 1), c[:2])
        for i in range(9):
            p, wd = rng.uniform(0.15, 1.75), rng.uniform(0.006, 0.05)
            band = np.exp(-((s - p + 0.02 * warp) / wd) ** 2) * (0.45 + 0.55 * np.sin(u * 2.5 + i) ** 2)
            img = img + rgb_of(c[2 + i % (len(c) - 2)]) * band[..., None] * 0.85
    elif style == "folds":
        t = np.sin(5.0 * (u + 0.22 * np.sin(3.1 * v + warp)) + 2.2 * np.sin(3.6 * v + 1.4 * u))
        dt = np.gradient(t, axis=1) * w / 18
        img = ramp(0.5 + 0.5 * t, c[:4]) * (1.0 + np.clip(dt, -0.45, 0.45))[..., None]
    else:  # aurora
        img = ramp(v * 0.9, c[:2])
        for i in range(3):
            yc = 0.32 + i * 0.12 + 0.13 * fbm(u, v * 0, rng, 3, 0.7)
            cur = np.exp(-(((v - yc) / (0.11 + 0.03 * i)) ** 2)) * (0.45 + 0.55 * np.sin(u * 42 + 7 * fbm(u, v * 0.2, rng, 3, 1.2)) ** 2)
            cur = cur * np.clip((yc + 0.1 - v) * 7 + 0.25, 0, 1)
            img = img + rgb_of(c[2 + i % (len(c) - 2)]) * cur[..., None] * 0.9
    vignette = 1 - 0.28 * (((x / w - 0.5) * 2) ** 2 + ((y / h - 0.5) * 2) ** 2)[..., None] / 2
    return np.clip(img * vignette, 0, 1)


def text_mask(s, size, w, h, cx, cy, align="CENTER"):
    """Cobertura (0..1) de un texto convertido a triangulos."""
    curve = bpy.data.curves.new("t", "FONT")
    curve.body = s
    curve.size = 1.0
    curve.align_x = align
    curve.align_y = "CENTER"
    curve.resolution_u = 10
    ob = bpy.data.objects.new("t", curve)
    bpy.context.scene.collection.objects.link(ob)
    bpy.context.view_layer.update()
    me = bpy.data.meshes.new_from_object(ob)
    bpy.data.objects.remove(ob)
    bpy.data.curves.remove(curve)
    verts = np.array([(cx + q.co.x * size, cy - q.co.y * size) for q in me.vertices], dtype=np.float32)
    tris = []
    for p in me.polygons:
        idx = list(p.vertices)
        tris += [(idx[0], idx[i], idx[i + 1]) for i in range(1, len(idx) - 1)]
    bpy.data.meshes.remove(me)
    mask = np.zeros((h, w), dtype=np.float32)
    ss = 3
    for t in tris:
        a, c, e = verts[list(t)]
        x0, x1 = int(max(0, min(a[0], c[0], e[0]) - 1)), int(min(w - 1, max(a[0], c[0], e[0]) + 1))
        y0, y1 = int(max(0, min(a[1], c[1], e[1]) - 1)), int(min(h - 1, max(a[1], c[1], e[1]) + 1))
        if x1 <= x0 or y1 <= y0:
            continue
        ys, xs = np.mgrid[y0:y1 + 1:complex(0, (y1 - y0 + 1) * ss), x0:x1 + 1:complex(0, (x1 - x0 + 1) * ss)]
        d = (c[1] - e[1]) * (a[0] - e[0]) + (e[0] - c[0]) * (a[1] - e[1])
        if abs(d) < 1e-9:
            continue
        l0 = ((c[1] - e[1]) * (xs - e[0]) + (e[0] - c[0]) * (ys - e[1])) / d
        l1 = ((e[1] - a[1]) * (xs - e[0]) + (a[0] - e[0]) * (ys - e[1])) / d
        # margen pequeno: sin el, entre triangulos vecinos quedan lineas sin cubrir
        e_ = 0.6 / max(1.0, math.sqrt(abs(d)))
        ins = (l0 >= -e_) & (l1 >= -e_) & (l0 + l1 <= 1 + e_)
        cov = ins.reshape((y1 - y0 + 1), ss, (x1 - x0 + 1), ss).mean(axis=(1, 3))
        mask[y0:y1 + 1, x0:x1 + 1] = np.maximum(mask[y0:y1 + 1, x0:x1 + 1], cov)
    return mask


def over(img, mask, color, alpha=1.0):
    m = (mask * alpha)[..., None]
    return img * (1 - m) + rgb_of(color) * m


def round_box(w, h, x0, y0, x1, y1, r):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    cx, cy, hx, hy = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    qx, qy = np.abs(x - cx) - hx + r, np.abs(y - cy) - hy + r
    d = np.hypot(np.maximum(qx, 0), np.maximum(qy, 0)) + np.minimum(np.maximum(qx, qy), 0) - r
    return np.clip(0.5 - d, 0, 1)


BRAND_THEME = {
    "Framework": {"style": "hills", "colors": ["#16233a", "#6a5c8f", "#f2a65a", "#3c3a5d", "#2b2b49", "#1b1c33", "#10111f"], "os": "win"},
}
DEFAULT_THEME = {"style": "ribbons", "colors": ["#0b1324", "#142a4a", "#23406b", "#5aa7ff", "#9b7bff", "#3fd0e0"], "os": "win"}


def add_screen(v, kit, entry, folder):
    sp = entry["specs"]
    theme = v.c.get("screen") or BRAND_THEME.get(entry["brand"]) or DEFAULT_THEME
    W, L = v.W, v.L
    px_w = 1800
    k = px_w / W
    px_h = round(L * k)
    res = sp.get("resolution") or "2880x1800"
    a, b = [float(n) for n in res.lower().replace("×", "x").split("x")[:2]]
    diag = (sp.get("screenInches") or 16.0) * 25.4
    ph = diag / math.hypot(a / b, 1)
    pw = ph * a / b
    bez = (entry.get("design", {}) or {}).get("bezelsMm") or {}
    side = bez.get("side", max(2.2, (W - pw) / 2))
    pw = min(pw, W - 2 * side)
    ph = min(ph, pw * b / a)
    rest = max(4.0, L - ph)
    top = bez.get("top", min(max(3.6, rest * 0.34), 9.5))
    x0, x1 = (W - pw) / 2 * k, (W + pw) / 2 * k
    y0, y1 = top * k, (top + ph) * k
    img = np.empty((px_h, px_w, 3), dtype=np.float32)
    img[:] = rgb_of(theme.get("bezel", "#050608"))
    ix0, iy0, ix1, iy1 = int(x0), int(y0), int(math.ceil(x1)), min(px_h, int(math.ceil(y1)))
    seed = zlib.crc32(f"{entry['brand']} {entry['model']}".split("·")[0].split("—")[0].encode("utf-8")) & 0xffff
    wall = wallpaper(ix1 - ix0, iy1 - iy0, theme, seed)
    # pantalla de bloqueo con el reloj, como la que se ve al abrir un equipo
    pw_px, ph_px = ix1 - ix0, iy1 - iy0
    mac = theme.get("os") == "mac"
    cx = pw_px / 2
    # velo oscuro detras del reloj: debe leerse tambien sobre fondos claros
    rows = np.linspace(0, 1, ph_px, dtype=np.float32)[:, None, None]
    wall = wall * (1 - 0.58 * np.exp(-((rows - 0.14) / 0.2) ** 2))
    clock = text_mask("10:28", ph_px * (0.15 if not mac else 0.13), pw_px, ph_px, cx, ph_px * (0.2 if not mac else 0.17))
    date = text_mask("martes, 18 de noviembre", ph_px * 0.03, pw_px, ph_px, cx, ph_px * (0.105 if not mac else 0.085))
    shadow = np.clip(np.roll(clock, (3, 2), axis=(0, 1)) * 0.3, 0, 1)
    wall = over(wall, shadow, "#000000", 0.6)
    wall = over(wall, clock, "#ffffff", 0.94)
    wall = over(wall, date, "#ffffff", 0.9)
    if mac:
        pill = round_box(pw_px, ph_px, cx - ph_px * 0.09, ph_px * 0.86, cx + ph_px * 0.09, ph_px * 0.905, ph_px * 0.022)
        wall = over(wall, pill, "#ffffff", 0.22)
    else:
        for i, dx in enumerate((-0.03, 0, 0.03)):
            dot = round_box(pw_px, ph_px, pw_px * (0.955 + dx) - 7, ph_px * 0.94 - 7, pw_px * (0.955 + dx) + 7, ph_px * 0.94 + 7, 4)
            wall = over(wall, dot, "#ffffff", 0.75)
    panel = round_box(px_w, px_h, x0, y0, x1, y1, theme.get("corner", 1.0) * k)[iy0:iy1, ix0:ix1][..., None]
    img[iy0:iy1, ix0:ix1] = img[iy0:iy1, ix0:ix1] * (1 - panel) + wall * panel
    cam = round_box(px_w, px_h, px_w / 2 - 0.9 * k, top / 2 * k - 0.9 * k, px_w / 2 + 0.9 * k, top / 2 * k + 0.9 * k, 0.9 * k)
    img = over(img, cam, "#171a20")
    lens = round_box(px_w, px_h, px_w / 2 - 0.4 * k, top / 2 * k - 0.4 * k, px_w / 2 + 0.4 * k, top / 2 * k + 0.4 * k, 0.4 * k)
    img = over(img, lens, "#2a3a55")
    chin = theme.get("chin")
    if chin and px_h - y1 > 3 * k:
        mask = text_mask(chin, 4.0 * k, px_w, px_h, px_w / 2, (y1 + px_h) / 2)
        img = over(img, mask, "#a9adb5")
    path = os.path.join(folder, "screen.jpg")
    im = bpy.data.images.new("pantalla_tmp", px_w, px_h)
    rgba = np.concatenate([img[::-1], np.ones((px_h, px_w, 1), dtype=np.float32)], axis=2)
    im.pixels.foreach_set(rgba.astype(np.float32).ravel())
    im.filepath_raw = path
    im.file_format = "JPEG"
    im.save(quality=90)
    bpy.data.images.remove(im)
    v.ph["screen"] = pd.Photo("screen.jpg", path, "screen.jpg")
    v.syntheticScreen = True
    mat = kit.photo("Foto_pantalla", v.ph["screen"], 0.3, 0.12)
    lid = pd.Mesher()
    for (part, name), (old, verts, faces, uvs) in v.lid.groups.items():
        for f in faces:
            P = [verts[i] for i in f]
            if part == "display":
                lid.poly(part, mat, P, [(min(0.999, max(0.001, p[0] / W + 0.5)), min(0.999, max(0.001, -p[1] / L))) for p in P])
            else:
                lid.poly(part, old, P, [uvs[i] for i in f])
    v.lid = lid

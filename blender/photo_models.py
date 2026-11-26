"""
Diseños únicos del catálogo, por marca. Las posiciones son fracciones de la foto de cada equipo: (columna, fila) de 0 a 1.
Los lados llevan su orientación porque no todas las fotos muestran la parte trasera en el mismo extremo.
"""
from photo_catalog import amj_chassis, amj_register


def amj_R(a, b, c, d, **k):
    return {"t": "deckRect", "r": (a, b, c, d), **k}


def amj_L(a, b, c, d, h=0.06, **k):
    return {"t": "lidRect", "r": (a, b, c, d), "h": h, **k}


def amj_F(a, b, c, d, **k):
    return {"t": "foot", "r": (a, b, c, d), **k}


def amj_H(*cols, **k):
    return {"t": "hinge", "cols": cols, **k}


# ------------------------------------------------------------------ Lenovo
legion10 = amj_chassis("lg/photo", 5, skip=("rear",), lidT=6.0, wall="#2a2b2f", under="#232428", hinge="#141518", rFront=7, rRear=4, rLidFree=6, rLidHinge=2,
                   swapSides=True, leftRearFirst=False, rightRearFirst=True, leftBand=(0.03, 0.6), rightBand=(0.05, 0.6),
                   features=[{"t": "deckEllipse", "e": (0.498, 0.065, 0.0226, 0.0326), "h": 0.25, "part": "keys"}, amj_R(0.218, 0.647, 0.561, 0.953, radius=2),
                             amj_L(0.355, 0.477, 0.645, 0.547), amj_H((0.08, 0.22), (0.78, 0.92), radius=3), amj_F(0.069, 0.081, 0.935, 0.109), amj_F(0.073, 0.884, 0.266, 0.912), amj_F(0.737, 0.884, 0.927, 0.912)])
amj_register([5, 41], legion10, ["botón de encendido en relieve", "rótulo LEGION", "dos bisagras", "pies de goma"])

amj_register([1], amj_chassis("loq", 1, lidT=5.5, wall="#8c9196", under="#7d8287", keySide="#141518", hinge="#25262a", rFront=6, rRear=4, rLidFree=5, rLidHinge=2,
                      files={"left": "flank_left", "right": "flank"}, rearBand=(0.02, 0.62), photoGain=0.92,
                      features=[{"t": "deckEllipse", "e": (0.498, 0.093, 0.0205, 0.03), "h": 0.25, "part": "keys"}, amj_R(0.215, 0.663, 0.548, 0.977, radius=2), amj_R(0.964, 0.81, 0.994, 0.935, h=0.05),
                                amj_L(0.032, 0.093, 0.089, 0.33), amj_L(0.968, 0.802, 0.995, 0.93, 0.05), amj_H((0.077, 0.158), (0.842, 0.923), radius=3.2),
                                amj_F(0.081, 0.135, 0.923, 0.165), amj_F(0.081, 0.867, 0.303, 0.898), amj_F(0.697, 0.867, 0.923, 0.898)]),
         ["bloque trasero con puertos", "rótulo LOQ", "etiqueta Lenovo", "pies de goma"])

# ------------------------------------------------------------------ Dell y Alienware
amj_register([12], amj_chassis("dg/photo", 12, skip=("bottom",), lidT=6.0, wall="#3a3a42", under="#2f3036", rFront=6, rRear=2.5, rLidFree=4, rLidHinge=1.5, rearBand=(0.22, 0.78), rearCols=(0.012, 0.992),
                       steps=[(0, 0), (0.245, 0), (0.255, -0.5), (0.668, -0.5), (0.68, 0), (1, 0)],
                       features=[{"t": "lidEllipse", "e": (0.502, 0.549, 0.06, 0.087), "h": 0.07}, amj_R(0.285, 0.716, 0.581, 0.958, radius=2), amj_H((0.07, 0.93), radius=2.6)]),
         ["zona de rejillas sobre el teclado", "logotipo Dell", "lomo de bisagra", "trasera con puertos"])

amj_register([4], amj_chassis("aw/photo", 4, skip=("bottom",), lidT=7.0, wall="#3b4448", under="#2c3338", keySide="#0e1013", rFront=9, rRear=6, rLidFree=7, rLidHinge=2,
                      swapSides=True, leftRearFirst=False, rightRearFirst=True, leftBand=(0.07, 0.8), rightBand=(0.03, 0.66), rearBand=(0.05, 0.77), rearCols=(0.003, 0.997),
                      steps=[(0, 0), (0.195, 0), (0.205, -0.6), (0.575, -0.6), (0.59, 0), (1, 0)],
                      features=[{"t": "lidEllipse", "e": (0.498, 0.484, 0.029, 0.058), "egg": 0.18, "h": 0.1}, amj_R(0.258, 0.628, 0.581, 0.907, radius=2), amj_H((0.05, 0.95), radius=3.2)]),
         ["bloque térmico trasero", "zona de ventiladores sobre el teclado", "logotipo Alienware", "touchpad iluminado"])

amj_register([15], amj_chassis("am/photo", 15, skip=("bottom",), lidT=6.5, wall="#3a3c42", under="#232428", keySide="#0e1013", rFront=8, rRear=3, rLidFree=4, rLidHinge=1.5, rearBand=(0.27, 0.8), rearCols=(0.002, 0.998),
                       steps=[(0, 0), (0.215, 0), (0.225, -0.6), (0.63, -0.6), (0.645, 0), (1, 0)],
                       features=[{"t": "deckEllipse", "e": (0.908, 0.13, 0.0145, 0.03), "egg": 0.18, "h": 0.2}, {"t": "lidEllipse", "e": (0.498, 0.253, 0.024, 0.05), "egg": 0.18, "h": 0.1},
                                 amj_R(0.273, 0.69, 0.59, 0.977, radius=2), amj_H((0.097, 0.93), radius=3), {"t": "rearPanel", "cols": (0.288, 0.712), "d": 1.0}]),
         ["rejilla hexagonal superior", "panel trasero de puertos saliente", "logotipos Alienware", "lomo de bisagra"])

# ------------------------------------------------------------------ HP
omen_max = amj_chassis("om/photo", 6, skip=("rear", "bottom"), lidT=6.0, wall="#3a3b3f", under="#2b2c30", rFront=7, rRear=4, rLidFree=5, rLidHinge=2, swapSides=True, leftRearFirst=True, rightRearFirst=True,
                   leftBand=(0.31, 0.86), rightBand=(0.27, 0.93),
                   features=[amj_R(0.252, 0.595, 0.61, 0.94, radius=2), amj_L(0.395, 0.414, 0.613, 0.581, 0.05), amj_H((0.08, 0.2), (0.8, 0.92), radius=3)])
amj_register([6, 49], omen_max, ["logotipo HyperX", "rótulo O16 de la cubierta", "rejillas laterales"])

amj_register([22], amj_chassis("gen/omen16", 22, lidT=5.0, wall="#1f2123", rFront=6, rRear=3, rLidFree=4, rLidHinge=1.5, leftBand=(0.2, 0.95), rightBand=(0.2, 0.95),
                       features=[amj_L(0.371, 0.43, 0.584, 0.493, 0.05), amj_R(0.256, 0.644, 0.64, 0.963, radius=2), amj_H((0.065, 0.17), (0.82, 0.927), radius=2.4)]),
         ["rótulo OMEN", "rejilla sobre el teclado", "rejillas laterales", "dos bisagras"])

amj_register([11], amj_chassis("vc/photo", 11, skip=("rear", "bottom"), lidT=5.0, wall="#4a4b50", under="#38393d", rFront=6, rRear=4, rLidFree=5, rLidHinge=2, leftRearFirst=False, rightRearFirst=True, leftBand=(0.2, 0.87), rightBand=(0.2, 0.87), keys=False,
                       features=[{"t": "keyGrid", "r": (0.035, 0.072, 0.965, 0.558), "numpad": True}, {"t": "lidPoly", "p": [(0.432, 0.447), (0.568, 0.447), (0.5, 0.572)], "h": 0.06},
                                 amj_R(0.3, 0.63, 0.7, 0.93, radius=2), amj_H((0.1, 0.22), (0.78, 0.9), radius=2.6)]),
         ["logotipo V", "teclado completo con bloque numérico", "rejillas laterales", "dos bisagras"])

amj_register([8], amj_chassis("zb/photo", 8, skip=("rear", "bottom"), lidT=5.5, wall="#a4a8ae", under="#95999f", keySide="#15161a", hinge="#7f8388", rFront=7, rRear=4, rLidFree=5, rLidHinge=2, leftBand=(0.5, 0.98), rightBand=(0.25, 0.8), photoGain=0.9,
                      features=[amj_L(0.444, 0.353, 0.568, 0.593, 0.05), amj_R(0.261, 0.526, 0.605, 0.926, radius=2), amj_H((0.097, 0.917), radius=2.6)]),
         ["logotipo HP", "touchpad con tres botones", "cuerpo plateado", "lomo de bisagra"])

# ------------------------------------------------------------------ ASUS y Apple
amj_register([3], amj_chassis("scar/photo", 3, lidT=6.0, wall="#2a2b2f", under="#202125", rFront=8, rRear=5, rLidFree=6, rLidHinge=2,
                      features=[amj_L(0.673, 0.284, 0.903, 0.465, 0.06), amj_R(0.256, 0.609, 0.605, 0.953, radius=2), amj_H((0.08, 0.2), (0.8, 0.92), radius=3), amj_F(0.056, 0.058, 0.944, 0.081), amj_F(0.056, 0.902, 0.944, 0.926)]),
         ["logotipo ROG", "trama STRIX en la tapa", "tira de luz inferior", "rejillas traseras"])

macbook_pro = amj_chassis("mb/photo", 18, skip=("rear", "bottom"), lidT=5.0, wall="#2e2e30", under="#252527", rFront=9, rRear=9, rLidFree=9, rLidHinge=7, leftRearFirst=False, rightRearFirst=True, leftBand=(0.29, 0.98), rightBand=(0.28, 0.78), keyOpts={"height": 0.5},
                      features=[{"t": "lidEllipse", "e": (0.5, 0.484, 0.053, 0.093), "h": 0.05}, amj_R(0.315, 0.693, 0.685, 0.953, h=0.08, radius=2), amj_H((0.06, 0.94), radius=2.2),
                                amj_F(0.05, 0.08, 0.1, 0.125), amj_F(0.9, 0.08, 0.95, 0.125), amj_F(0.05, 0.875, 0.1, 0.92), amj_F(0.9, 0.875, 0.95, 0.92)])
amj_register([18, 7], macbook_pro, ["logotipo de Apple", "altavoces a los lados del teclado", "cuatro patas redondas"])

# ------------------------------------------------------------------ Acer
amj_register([9], amj_chassis("ac/photo", 9, skip=("bottom",), lidT=6.5, wall="#22263c", under="#1a1d2c", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, leftBand=(0.32, 0.97), rightBand=(0.5, 0.93), rearBand=(0.0, 0.9), rearCols=(0.0, 1.0),
                      features=[amj_L(0.48, 0.042, 0.52, 0.105, 0.08), amj_R(0.229, 0.612, 0.605, 0.935, radius=2), amj_H((0.065, 0.148), (0.852, 0.935), radius=3)]),
         ["logotipo Predator", "bisagras en bloque", "rejillas laterales inclinadas", "trasera iluminada"])

amj_register([10], amj_chassis("h16/photo", 10, skip=("bottom",), lidT=6.5, wall="#1d2030", under="#171925", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, leftBand=(0.37, 0.8), rightBand=(0.27, 0.73), rearBand=(0.45, 1.0), rearCols=(0.0, 1.0),
                       features=[amj_L(0.452, 0.284, 0.545, 0.46, 0.08), amj_R(0.285, 0.642, 0.64, 0.953, radius=2), amj_H((0.068, 0.152), (0.845, 0.929), radius=3)]),
         ["logotipo Predator", "marca 16 junto a la bisagra", "trasera con puertos", "rejillas laterales"],
         {"left": ["rj45", "usba", "microsd", "audio"], "right": ["usba", "usba", "kensington"], "rear": ["power", "hdmi", "usbc", "usbc"]})

amj_register([29], amj_chassis("gen/ph16_72", 29, lidT=6.5, wall="#1c1e2b", under="#161823", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, leftBand=(0.0, 0.98), rightBand=(0.25, 0.75), rearBand=(0.31, 0.8), rearCols=(0.0, 1.0),
                       features=[amj_L(0.455, 0.293, 0.545, 0.465, 0.08), amj_R(0.223, 0.663, 0.569, 0.953, radius=2), amj_R(0.855, 0.658, 0.887, 0.705, h=0.1, radius=0.5), amj_H((0.07, 0.156), (0.845, 0.929), radius=3)]),
         ["logotipos Predator", "trasera con puertos y rejillas", "bisagras en bloque"])

# ------------------------------------------------------------------ Gigabyte, ORIGIN PC, Razer y ASUS
amj_register([13], amj_chassis("ao/photo", 13, skip=("bottom",), lidT=6.0, wall="#35363b", under="#27282c", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, leftBand=(0.3, 0.79), rightBand=(0.25, 0.77), rearBand=(0.17, 0.77), rearCols=(0.008, 0.99),
                       features=[amj_L(0.452, 0.314, 0.545, 0.453, 0.08), amj_R(0.292, 0.66, 0.6, 0.949, radius=2), {"t": "rearPanel", "cols": (0.314, 0.683), "d": 1.0}, amj_H((0.016, 0.073), (0.93, 0.995), radius=3)]),
         ["logotipo AORUS", "touchpad con el águila", "panel trasero de puertos", "rejillas de cobre"])

amj_register([16], amj_chassis("op/photo", 16, skip=("left", "right", "rear", "bottom"), lidT=6.0, wall="#26262a", under="#1d1d21", rFront=8, rRear=5, rLidFree=5, rLidHinge=2, keys=False,
                       features=[{"t": "keyGrid", "r": (0.083, 0.06, 0.917, 0.46), "numpad": True}, {"t": "lidEllipse", "e": (0.5, 0.31, 0.045, 0.065), "h": 0.08}, amj_R(0.232, 0.553, 0.61, 0.918, radius=2), amj_H((0.08, 0.2), (0.8, 0.92), radius=3)]),
         ["logotipo ORIGIN", "cubierta con su zona de teclado", "touchpad"])

amj_register([14], amj_chassis("rz/photo", 14, skip=("rear", "bottom"), lidT=5.5, wall="#1f1f22", under="#17171a", rFront=5, rRear=3, rLidFree=4, rLidHinge=1.5, leftRearFirst=False, rightRearFirst=True, leftBand=(0.0, 0.7), rightBand=(0.33, 0.79),
                       features=[{"t": "lidEllipse", "e": (0.494, 0.453, 0.079, 0.116), "h": 0.08}, amj_R(0.285, 0.593, 0.71, 0.977, radius=2), amj_H((0.09, 0.91), radius=2.4)]),
         ["logotipo Razer", "altavoces a los lados del teclado", "touchpad grande", "bisagra de barra"])

amj_register([2], amj_chassis("tuf/photo", 2, lidT=6.0, wall="#4a4b50", under="#35363a", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, leftBand=(0.33, 0.87), rightBand=(0.33, 0.87), rearBand=(0.37, 0.93), rearCols=(0.0, 1.0),
                      steps=[(0, -0.4), (0.575, -0.4), (0.59, 0), (1, 0)],
                      features=[amj_L(0.895, 0.07, 0.952, 0.314, 0.07), amj_R(0.219, 0.607, 0.597, 0.977, radius=2), {"t": "deckEllipse", "e": (0.897, 0.14, 0.0194, 0.028), "h": 0.2, "part": "keys"}, amj_H((0.043, 0.14), (0.862, 0.957), radius=3.4), amj_F(0.333, 0.919, 0.666, 0.933)]),
         ["rótulo TUF", "rejillas traseras", "base con patrón hexagonal", "bisagras en bloque"])

amj_register([27], amj_chassis("gen/zen14", 27, lidT=4.2, wall="#c4c7cd", under="#b4b7bd", keySide="#9ea1a8", hinge="#a9acb2", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, photoGain=0.84, photoGains={"Foto_cubierta": 0.72},
                       features=[amj_R(0.29, 0.63, 0.71, 0.97, radius=1.5), amj_H((0.089, 0.916), radius=2)]),
         ["líneas grabadas en la tapa", "touchpad con teclado numérico", "bisagra de barra"])

# ------------------------------------------------------------------ Framework, LG, Apple, Microsoft, MSI y Dell XPS
framework = amj_chassis("gen/fw16", 26, D=270.0, T=17.95, Tf=17.95, lidT=5.0, wall="#a2a5aa", under="#8f9297", keySide="#141518", hinge="#6f7277", rFront=6, rRear=3, rLidFree=5, rLidHinge=1.5, photoGain=0.86,
                    features=[{"t": "lidEllipse", "e": (0.503, 0.5, 0.065, 0.087), "h": 0.06}, amj_R(0.327, 0.588, 0.673, 0.902, radius=2), amj_R(0.11, 0.109, 0.89, 0.537, h=0.15, radius=1), amj_R(0.01, 0.112, 0.089, 0.551, h=0.1, radius=0.8), amj_R(0.911, 0.112, 0.99, 0.551, h=0.1, radius=0.8),
                              amj_H((0.1, 0.9), radius=2.2), {"t": "rearBlock", "d": 20.2, "z0": -3.0, "z1": 11.5}])
amj_register([26, 52], framework, ["módulos de teclado y espaciadores", "módulo gráfico trasero", "logotipo Framework", "seis ranuras de expansión"])

amj_register([30], amj_chassis("gen/gram16", 30, lidT=4.2, wall="#4b4d52", under="#3e4044", rFront=6, rRear=3, rLidFree=5, rLidHinge=2,
                       features=[amj_L(0.452, 0.465, 0.548, 0.526, 0.05), amj_R(0.281, 0.6, 0.655, 0.944, radius=1.5), amj_H((0.123, 0.879), radius=2)]),
         ["rótulo gram", "bloque numérico de tres columnas", "bisagra de barra"], {"left": ["hdmi", "usbc", "usbc", "audio"], "right": ["microsd", "usba", "usba", "kensington"]})

macbook_air = amj_chassis("gen/mba15", 23, lidT=4.2, wall="#2e3642", under="#262d38", rFront=9, rRear=9, rLidFree=9, rLidHinge=7, keyOpts={"height": 0.45},
                      features=[{"t": "lidEllipse", "e": (0.5, 0.484, 0.053, 0.093), "h": 0.04}, amj_R(0.315, 0.691, 0.685, 0.953, h=0.08, radius=2), amj_H((0.06, 0.94), radius=1.8)])
amj_register([23, 42], macbook_air, ["logotipo de Apple", "altavoces a los lados del teclado", "muesca de la cámara en pantalla"])

amj_register([25], amj_chassis("gen/surf7", 25, lidT=4.5, wall="#c7c9cc", under="#b6b8bb", keySide="#6f7277", hinge="#9b9da1", rFront=8, rRear=3, rLidFree=6, rLidHinge=2, photoGain=0.84,
                       features=[amj_R(0.318, 0.598, 0.686, 0.937, radius=2), amj_H((0.095, 0.905), radius=2)]),
         ["bandeja del teclado", "touchpad grande", "conector Surface"])

titan = amj_chassis("gen/titan18", 28, lidT=6.5, wall="#1d1e22", under="#8e939c", rFront=6, rRear=3, rLidFree=5, rLidHinge=2, rearBand=(0.05, 0.95), rearCols=(0.0, 1.0),
                steps=[(0, 0), (0.625, 0), (0.635, 0.25), (1, 0.25)],
                features=[amj_L(0.448, 0.226, 0.552, 0.412, 0.1), amj_R(0.335, 0.679, 0.665, 0.949, radius=1.5), amj_H((0.06, 0.94), radius=3)])
amj_register([28, 53], titan, ["logotipo MSI", "reposamanos de vidrio", "touchpad RGB", "base plateada con rótulo TITAN", "trasera con puertos"])

amj_register([21], amj_chassis("gen/xps16", 21, lidT=4.7, wall="#3a3c40", under="#2e3034", rFront=7, rRear=4, rLidFree=5, rLidHinge=2, keyOpts={"height": 0.4},
                       features=[{"t": "lidEllipse", "e": (0.5, 0.505, 0.054, 0.08), "h": 0.05}, amj_H((0.105, 0.895), radius=2)]),
         ["logotipo Dell", "touchpad invisible", "teclado sin separación", "rejillas inferiores laterales"])

# los laptops sin fotos en el proyecto se modelan a partir de sus descripciones
import spec_designs  # noqa: E402,F401

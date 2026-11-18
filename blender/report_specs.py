"""
Genera docs/VERIFICACION_LAPTOPS.md a partir de las fichas verificadas y de los meta.json de los modelos de Blender.

Uso (sirve el Python que trae Blender):
  python blender/report_specs.py
"""
import glob
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPECS = os.path.join(ROOT, "src", "main", "resources", "specs", "verified")
MODELS = os.path.join(ROOT, "src", "main", "resources", "laptops3d")
OUT = os.path.join(ROOT, "docs", "VERIFICACION_LAPTOPS.md")

LABELS = {
    "cpu": "procesador", "cpuCores": "núcleos", "cpuBaseWatts": "potencia base", "cpuBoostWatts": "potencia máxima",
    "gpu": "gráficos", "gpuVramGb": "VRAM", "gpuTgpWatts": "TGP", "ramGb": "RAM", "ramType": "tipo de RAM",
    "ramUpgradable": "RAM ampliable", "storageGb": "almacenamiento", "storageSlots": "ranuras M.2",
    "screenInches": "pantalla (pulgadas)", "resolution": "resolución", "refreshRateHz": "frecuencia", "panelType": "panel",
    "widthMm": "ancho", "depthMm": "fondo", "thicknessMm": "grosor", "weightKg": "peso", "batteryWh": "batería",
    "releaseYear": "año", "chassisMaterial": "material", "priceUsd": "precio", "colorHex": "color",
}
PORTS = {"usba": "USB-A", "usbc": "USB-C", "hdmi": "HDMI", "minidp": "Mini DisplayPort", "rj45": "RJ-45", "audio": "audio",
         "sd": "SD", "microsd": "microSD", "power": "alimentación", "magsafe": "MagSafe", "kensington": "Kensington",
         "surface": "Surface Connect"}
SIDES = {"left": "izquierda", "right": "derecha", "rear": "trasera"}


def listing(items):
    return ", ".join(items) if items else "ninguno"


def main():
    index = {(e["brand"], e["model"]): e["folder"] for e in json.load(open(os.path.join(MODELS, "index.json"), encoding="utf-8"))}
    entries = []
    for path in sorted(glob.glob(os.path.join(SPECS, "*.json"))):
        entries += json.load(open(path, encoding="utf-8"))
    entries.sort(key=lambda e: (e["brand"].lower(), e["model"].lower()))
    rows, blocks = [], []
    complete = partial = none = 0
    for e in entries:
        folder = index.get((e["brand"], e["model"]))
        meta = json.load(open(os.path.join(MODELS, folder, "meta.json"), encoding="utf-8")) if folder else {}
        placed, missing = meta.get("portsPlaced", []), meta.get("portsNotPlaced", [])
        if placed and not missing:
            complete += 1
            state = "todos con su lado"
        elif placed:
            partial += 1
            state = f"{len(placed)} colocados, {len(missing)} sin lado"
        else:
            none += 1
            state = "sin colocar"
        name = f"{e['brand']} {e['model']}"
        rows.append(f"| {name} | {len(e['specs'])} | {len(e.get('notVerified', []))} | {state} | {len(meta.get('assumed', []))} |")
        by_side = {}
        for p in placed:
            by_side.setdefault(p["side"], []).append(PORTS.get(p["type"], p["type"]))
        lines = [f"### {name}", "", f"- **Configuración:** {e.get('config', '')}",
                 "- **Datos verificados:** " + listing([LABELS.get(k, k) for k in e["specs"]]),
                 "- **Sin confirmar:** " + listing([LABELS.get(k, k) for k in e.get("notVerified", [])])]
        for c in e.get("conflicts", []):
            lines.append(f"- **Discrepancia:** {c}")
        lines.append("- **Puertos en el modelo:** " + (("; ".join(f"{SIDES[s]}: {', '.join(v)}" for s, v in by_side.items())) or "ninguno"))
        if missing:
            lines.append("- **Puertos sin lado confirmado (no se dibujan):** " + listing([PORTS.get(p, p) for p in missing]))
        lines.append("- **Rasgos confirmados en 3D:** " + listing(meta.get("verifiedFeatures", [])))
        lines.append("- **Valores supuestos en 3D:** " + listing(meta.get("assumed", [])))
        lines.append("- **Sin información:** " + listing(meta.get("notVerified", [])))
        lines.append("- **Fuentes:**")
        lines += [f"  - {s}" for s in e.get("sources", [])]
        blocks.append("\n".join(lines))
    head = [
        "# Verificación de laptops y modelos 3D", "",
        "Documento generado por `blender/report_specs.py`. Resume, para cada laptop del catálogo, qué datos proceden de una fuente, "
        "qué no se pudo confirmar y qué partes del modelo de Blender son supuestas.", "",
        f"- Laptops: **{len(entries)}**. Con modelo de Blender: **{len(index)}**.",
        f"- Puertos: **{complete}** modelos con todos colocados, **{partial}** con parte sin lado confirmado y **{none}** sin ninguno colocado.",
        "- Los colores de los modelos son aproximaciones visuales del color que nombra la fuente, no valores publicados.",
        "- Los logotipos gráficos oficiales no se reproducen: solo aparecen en las fotos reales ya incluidas o como rótulo de texto.", "",
        "| Laptop | Datos verificados | Sin confirmar | Puertos | Supuestos 3D |", "|---|---:|---:|---|---:|"]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(head + rows) + "\n\n## Detalle por laptop\n\n" + "\n\n".join(blocks) + "\n")
    print("laptops", len(entries), "modelos", len(index), "puertos completos", complete, "parciales", partial, "sin colocar", none)


main()

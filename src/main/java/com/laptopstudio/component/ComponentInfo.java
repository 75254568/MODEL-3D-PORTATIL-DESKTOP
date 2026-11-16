package com.laptopstudio.component;

/**
 * Componente de la biblioteca 3D (un procesador, una grafica, un modulo de memoria o una unidad SSD).
 * Viene de {@code components/index.json}; sus archivos estan en {@code components/<dir>/<id>/}.
 *
 * @param category CPU, GPU, RAM o SSD
 * @param segment  Escritorio, Laptop, Workstation o Servidor
 */
public record ComponentInfo(String id, String dir, String category, String name, String segment) {

    private String folder() {
        return "/components/" + dir + "/" + id + "/";
    }

    /** Modelo OBJ; su MTL esta a su lado con el mismo nombre. */
    public String objPath() {
        return folder() + id + ".obj";
    }

    /** Ficha tecnica en Markdown. */
    public String specPath() {
        return folder() + "ficha.md";
    }

    /** Nombre de la categoria para la interfaz. */
    public String categoryLabel() {
        return switch (category) {
            case "CPU" -> "Procesador";
            case "GPU" -> "Gráficos";
            case "RAM" -> "Memoria RAM";
            case "SSD" -> "Almacenamiento";
            default -> category;
        };
    }
}

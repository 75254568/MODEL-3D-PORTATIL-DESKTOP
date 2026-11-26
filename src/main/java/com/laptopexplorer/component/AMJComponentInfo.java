package com.laptopexplorer.component;

/**
 * Componente de la biblioteca 3D (un procesador, una gráfica, un módulo de memoria o una unidad SSD).
 * Viene de {@code components/index.json}; sus archivos están en {@code components/<dir>/<id>/}.
 *
 * @param category CPU, GPU, RAM o SSD
 * @param segment  Escritorio, Laptop, Workstation o Servidor
 */
public record AMJComponentInfo(String id, String dir, String category, String name, String segment) {

    private String amjFolder() {
        return "/components/" + dir + "/" + id + "/";
    }

    /** Modelo OBJ; su MTL esta a su lado con el mismo nombre. */
    public String amjObjPath() {
        return amjFolder() + id + ".obj";
    }

    /** Ficha técnica en Markdown. */
    public String amjSpecPath() {
        return amjFolder() + "ficha.md";
    }

    /** Nombre de la categoría para la interfaz. */
    public String amjCategoryLabel() {
        return switch (category) {
            case "CPU" -> "Procesador";
            case "GPU" -> "Gráficos";
            case "RAM" -> "Memoria RAM";
            case "SSD" -> "Almacenamiento";
            default -> category;
        };
    }
}

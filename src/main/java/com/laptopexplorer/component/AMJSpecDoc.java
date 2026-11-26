package com.laptopexplorer.component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ficha técnica de un componente, ya limpia y estructurada para mostrarla.
 * {@code heading} es el título que trae la propia ficha; {@code highlights}, sus datos destacados.
 */
public record AMJSpecDoc(String heading, List<String> highlights, List<AMJSection> sections) {

    /** Tramo de texto; {@code bold} marca lo que la ficha resalta. */
    public record AMJRun(String text, boolean bold) {
    }

    /**
     * Texto con tramos resaltados. {@code incomplete} es verdadero cuando a la ficha original le faltan cifras en
     * este texto (se perdieron al generarla): se muestra tal cual y se avisa, no se completa con suposiciones.
     */
    public record AMJRich(List<AMJRun> runs, boolean incomplete) {

        public String amjPlain() {
            return runs.stream().map(AMJRun::text).collect(Collectors.joining());
        }
    }

    public record AMJSection(String title, List<AMJBlock> blocks) {
    }

    public sealed interface AMJBlock permits AMJParagraph, AMJSubheading, AMJBullets, AMJTable, AMJNote {
    }

    public record AMJParagraph(AMJRich text) implements AMJBlock {
    }

    public record AMJSubheading(String text) implements AMJBlock {
    }

    /** Elemento de lista. {@code marker} es "•" o el número ("1.") de una lista ordenada. */
    public record AMJItem(int level, String marker, AMJRich text) {
    }

    public record AMJBullets(List<AMJItem> items) implements AMJBlock {
    }

    /** Tabla: la primera columna nombra el dato y las demás lo describen. */
    public record AMJTable(List<String> headers, List<List<AMJRich>> rows) implements AMJBlock {
    }

    /** Aviso destacado de la ficha (cita). */
    public record AMJNote(List<AMJBlock> blocks) implements AMJBlock {
    }
}

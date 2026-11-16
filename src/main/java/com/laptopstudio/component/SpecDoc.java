package com.laptopstudio.component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ficha tecnica de un componente, ya limpia y estructurada para mostrarla.
 * {@code heading} es el titulo que trae la propia ficha; {@code highlights}, sus datos destacados.
 */
public record SpecDoc(String heading, List<String> highlights, List<Section> sections) {

    /** Tramo de texto; {@code bold} marca lo que la ficha resalta. */
    public record Run(String text, boolean bold) {
    }

    /**
     * Texto con tramos resaltados. {@code incomplete} es verdadero cuando a la ficha original le faltan cifras en
     * este texto (se perdieron al generarla): se muestra tal cual y se avisa, no se completa con suposiciones.
     */
    public record Rich(List<Run> runs, boolean incomplete) {

        public String plain() {
            return runs.stream().map(Run::text).collect(Collectors.joining());
        }
    }

    public record Section(String title, List<Block> blocks) {
    }

    public sealed interface Block permits Paragraph, Subheading, Bullets, Table, Note {
    }

    public record Paragraph(Rich text) implements Block {
    }

    public record Subheading(String text) implements Block {
    }

    /** Elemento de lista. {@code marker} es "•" o el numero ("1.") de una lista ordenada. */
    public record Item(int level, String marker, Rich text) {
    }

    public record Bullets(List<Item> items) implements Block {
    }

    /** Tabla: la primera columna nombra el dato y las demas lo describen. */
    public record Table(List<String> headers, List<List<Rich>> rows) implements Block {
    }

    /** Aviso destacado de la ficha (cita). */
    public record Note(List<Block> blocks) implements Block {
    }
}

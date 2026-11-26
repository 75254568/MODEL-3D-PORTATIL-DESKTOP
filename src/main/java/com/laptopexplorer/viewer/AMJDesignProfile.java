package com.laptopexplorer.viewer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopexplorer.model.AMJLaptop;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Rasgos de diseño exterior de una serie de laptops ({@code /models/designs.json}): colores de tapa, cubierta y
 * teclas, color de la retroiluminación, rótulo de la tapa y rasgos propios de la serie (muesca en la pantalla,
 * TrackPoint, barra de luz frontal, repisa trasera de ventilación, trazos decorativos en la tapa).
 *
 * Se usa solo para los laptops que no tienen fotografías propias: con el perfil su modelo 3D deja de ser un
 * bloque gris igual para todos y toma el lenguaje de diseño de su serie. Es una aproximación, no una
 * reconstrucción del equipo: los perfiles describen la serie, no cada generación, y así se indica en la interfaz.
 * Las fotos de un modelo nunca se reutilizan para otro; lo que se comparte aquí son rasgos, no imágenes.
 *
 * @param brand     marca o marcas separadas por "|"
 * @param series    fragmento del nombre del modelo que identifica la serie; vacío = cualquier modelo de la marca
 * @param logoPlace "center" o "corner"
 * @param lidAccent "slash" (línea diagonal), "lines" (trazos grabados) o nulo
 * @param shelf     fondo en mm de la repisa trasera, detrás de la bisagra; 0 = la tapa llega al borde
 */
public record AMJDesignProfile(String brand, String series, String lid, String deck, String keys, String legend,
                            String logo, String logoColor, String logoPlace, String lidAccent, String accent,
                            double shelf, boolean lightBar, boolean notch, boolean trackpoint) {

    private static volatile List<AMJDesignProfile> all;

    private static List<AMJDesignProfile> amjAll() {
        List<AMJDesignProfile> list = all;
        if (list == null) {
            list = List.of();
            try (InputStream in = AMJDesignProfile.class.getResourceAsStream("/models/designs.json")) {
                if (in != null) {
                    list = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            .readValue(in, new TypeReference<List<AMJDesignProfile>>() { });
                }
            } catch (IOException | RuntimeException e) {
                // sin la tabla, los modelos sin fotos se dibujan con el aspecto neutro de siempre
            }
            all = list;
        }
        return list;
    }

    /** Perfil de la serie del laptop: el primero de la tabla cuya marca y fragmento de modelo coinciden; null si ninguno. */
    static AMJDesignProfile amjOf(AMJLaptop l) {
        if (l == null || l.getBrand() == null || l.getModel() == null) {
            return null;
        }
        String brand = l.getBrand().trim().toLowerCase(Locale.ROOT);
        String model = l.getModel().toLowerCase(Locale.ROOT);
        for (AMJDesignProfile p : amjAll()) {
            boolean brandOk = false;
            for (String b : p.brand().split("\\|")) {
                brandOk |= b.trim().toLowerCase(Locale.ROOT).equals(brand);
            }
            if (brandOk && (p.series() == null || p.series().isBlank() || model.contains(p.series().toLowerCase(Locale.ROOT)))) {
                return p;
            }
        }
        return null;
    }

    static Color amjColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            return Color.web(hex);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

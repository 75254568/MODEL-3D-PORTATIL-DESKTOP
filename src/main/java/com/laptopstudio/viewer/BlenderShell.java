package com.laptopstudio.viewer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.model.Laptop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Exterior de un laptop modelado en Blender ({@code blender/build_laptops.py}), empaquetado en {@code /laptops3d}.
 * El cuerpo y la tapa son dos OBJ: la tapa tiene su origen en el eje de la bisagra para poder abrirse.
 * Cada laptop se enlaza por marca y modelo a traves de {@code index.json}.
 */
final class BlenderShell {

    private static final Logger log = LoggerFactory.getLogger(BlenderShell.class);
    private static volatile Map<String, String> index;

    final ExternalModel.Loaded body;
    final ExternalModel.Loaded lid;
    final double width, depth, thickness, bodyRear, bodyFront, lidThickness, lidLength, hingeY, maxLidDeg;
    /** Modelo con diseno propio: sus fotos ya van proyectadas sobre la geometria. */
    final boolean photoTextured;
    /** Diseno propio sin foto de la pantalla: la aplicacion le pone el fondo generado con los datos de la ficha. */
    final boolean generatedScreen;
    /** Modelado a partir de descripciones, con materiales lisos y sin fotos proyectadas. */
    final boolean described;
    /** Atenuacion de las fotos claras, para que las luces del visor no las quemen. */
    final double photoGain;
    private final JsonNode photoGains;
    /** Silueta de los modelos con diseno propio: altura desde la que la base alcanza su ancho y radios de sus esquinas. */
    final double outlineFrom, cornerFront, cornerRear, cornerLidFree, cornerLidHinge;
    /** Ajuste del interior para que quepa dentro de la silueta inferior de la base. */
    final double innerScaleX, innerScaleZ, innerOffsetZ;

    /** Atenuacion de la foto de un material concreto. */
    double photoGain(String material) {
        return photoGains.path(material).asDouble(photoGain);
    }

    private BlenderShell(ExternalModel.Loaded body, ExternalModel.Loaded lid, JsonNode meta) {
        this.body = body;
        this.lid = lid;
        width = meta.path("widthMm").asDouble();
        depth = meta.path("depthMm").asDouble();
        thickness = meta.path("thicknessMm").asDouble();
        bodyRear = meta.path("bodyRearMm").asDouble();
        bodyFront = meta.path("bodyFrontMm").asDouble(bodyRear);
        lidThickness = meta.path("lidThicknessMm").asDouble();
        lidLength = meta.path("lidLengthMm").asDouble();
        hingeY = meta.path("hingeYMm").asDouble();
        maxLidDeg = meta.path("maxLidDeg").asDouble(180);
        photoTextured = meta.path("photoTextured").asBoolean(false);
        generatedScreen = meta.path("generatedScreen").asBoolean(false);
        described = "descripciones".equals(meta.path("modelBasis").asText(""));
        photoGain = meta.path("photoGain").asDouble(1);
        photoGains = meta.path("photoGains");
        outlineFrom = meta.path("outlineFromFrac").asDouble(0);
        cornerFront = meta.path("cornerFrontMm").asDouble(12);
        cornerRear = meta.path("cornerRearMm").asDouble(7);
        cornerLidFree = meta.path("cornerLidFreeMm").asDouble(12);
        cornerLidHinge = meta.path("cornerLidHingeMm").asDouble(3);
        JsonNode fit = meta.path("innerFit");
        innerScaleX = fit.path("scaleX").asDouble(1);
        innerScaleZ = fit.path("scaleZ").asDouble(1);
        innerOffsetZ = fit.path("offsetZ").asDouble(0);
    }

    private static String key(String brand, String model) {
        return (brand + "|" + model).toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> index() {
        Map<String, String> map = index;
        if (map == null) {
            map = new HashMap<>();
            try (InputStream in = BlenderShell.class.getResourceAsStream("/laptops3d/index.json")) {
                if (in != null) {
                    for (JsonNode n : new ObjectMapper().readTree(in)) {
                        map.put(key(n.path("brand").asText(), n.path("model").asText()), n.path("folder").asText());
                    }
                }
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudo leer el índice de modelos Blender: {}", e.toString());
            }
            index = map;
        }
        return map;
    }

    /** Verdadero si el laptop tiene modelo de Blender, sin cargarlo. */
    static boolean exists(Laptop l) {
        return l != null && index().containsKey(key(l.getBrand(), l.getModel()));
    }

    /** Datos del modelo (medidas y que rasgos son verificados o supuestos), sin cargar la geometria; null si no tiene. */
    static JsonNode meta(Laptop l) {
        if (!exists(l)) {
            return null;
        }
        try (InputStream in = BlenderShell.class.getResourceAsStream("/laptops3d/" + index().get(key(l.getBrand(), l.getModel())) + "/meta.json")) {
            return in == null ? null : new ObjectMapper().readTree(in);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudieron leer los datos del modelo de {}: {}", l.getDisplayName(), e.toString());
            return null;
        }
    }

    /** Modelo del laptop, o null si no tiene o no se puede leer (en ese caso se usa la representacion anterior). */
    static BlenderShell of(Laptop l) {
        if (!exists(l)) {
            return null;
        }
        String dir = "/laptops3d/" + index().get(key(l.getBrand(), l.getModel())) + "/";
        try (InputStream in = BlenderShell.class.getResourceAsStream(dir + "meta.json")) {
            if (in == null) {
                return null;
            }
            JsonNode meta = new ObjectMapper().readTree(in);
            return new BlenderShell(ExternalModel.loadInPlace(ExternalModel.resource(dir + "body.obj")),
                    ExternalModel.loadInPlace(ExternalModel.resource(dir + "lid.obj")), meta);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo cargar el modelo Blender de {}: {}", l.getDisplayName(), e.toString());
            return null;
        }
    }

    /** Nombre de la pieza sin el sufijo numerico que Blender anade a los objetos repetidos ("base.041"). */
    static String part(ExternalModel.Part p) {
        String n = p.object();
        int dot = n.indexOf('.');
        return dot < 0 ? n : n.substring(0, dot);
    }
}

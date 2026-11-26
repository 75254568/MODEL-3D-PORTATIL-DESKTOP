package com.laptopexplorer.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopexplorer.viewer.AMJExternalModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biblioteca de componentes en 3D empaquetada con la aplicación ({@code /components}): el índice, el modelo de
 * cada componente, su ficha técnica y el nombre legible de sus materiales. Solo lee recursos; no usa la base de datos.
 * Cargar un modelo o una ficha lee y analiza archivos: los modelos, fuera del hilo de la interfaz.
 */
@Service
@Slf4j
public class AMJComponentLibrary {

    /** Lado mayor en planta, en unidades del visor, al que se escala todo modelo: así todos se ven al mismo tamaño. */
    public static final double VIEW_SIZE = 300;

    private final List<AMJComponentInfo> components;
    private final Map<String, String> materialLabels;
    private final Map<String, AMJSpecDoc> specs = new ConcurrentHashMap<>();

    public AMJComponentLibrary(ObjectMapper mapper) {
        List<AMJComponentInfo> found = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/components/index.json")) {
            if (in != null) {
                for (AMJComponentInfo c : mapper.readValue(in, new TypeReference<List<AMJComponentInfo>>() { })) {
                    if (getClass().getResource(c.amjObjPath()) != null) {
                        found.add(c);
                    } else {
                        log.warn("El componente {} está en el índice pero no tiene modelo: se omite", c.id());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo leer el índice de componentes 3D: {}", e.getMessage());
        }
        this.components = List.copyOf(found);
        Map<String, String> labels = Map.of();
        try (InputStream in = getClass().getResourceAsStream("/components/materials.json")) {
            if (in != null) {
                labels = mapper.readValue(in, new TypeReference<Map<String, String>>() { });
            }
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudieron leer los nombres de los materiales: {}", e.getMessage());
        }
        this.materialLabels = labels;
        log.info("Biblioteca de componentes 3D: {} modelos", components.size());
    }

    public List<AMJComponentInfo> amjAll() {
        return components;
    }

    /** Modelo listo para el visor, escalado a {@link #VIEW_SIZE}. Los archivos de la biblioteca tienen +Z hacia arriba. */
    public AMJExternalModel.AMJLoaded amjLoad(AMJComponentInfo c) throws IOException {
        return AMJExternalModel.amjLoad(AMJExternalModel.amjResource(c.amjObjPath()), AMJExternalModel.AMJUp.Z, VIEW_SIZE);
    }

    /** Ficha técnica del componente; vacía si no tiene o no se puede leer. */
    public AMJSpecDoc amjSpec(AMJComponentInfo c) {
        return specs.computeIfAbsent(c.id(), id -> {
            try (InputStream in = getClass().getResourceAsStream(c.amjSpecPath())) {
                if (in != null) {
                    return AMJSpecMarkdown.amjParse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudo leer la ficha de {}: {}", c.id(), e.getMessage());
            }
            return new AMJSpecDoc(null, List.of(), List.of());
        });
    }

    /** Nombre legible de un material del modelo; si no esta en la tabla, su identificador con espacios. */
    public String amjMaterialLabel(String material) {
        if (material == null || material.isBlank()) {
            return "Sin material";
        }
        String label = materialLabels.get(material);
        return label != null ? label : material.replace('_', ' ').replaceAll("(?<=[a-z])(?=[A-Z])", " ").trim();
    }
}

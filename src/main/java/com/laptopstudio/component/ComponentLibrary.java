package com.laptopstudio.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.viewer.ExternalModel;
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
 * Biblioteca de componentes en 3D empaquetada con la aplicacion ({@code /components}): el indice, el modelo de
 * cada componente, su ficha tecnica y el nombre legible de sus materiales. Solo lee recursos; no usa la base de datos.
 * Cargar un modelo o una ficha lee y analiza archivos: los modelos, fuera del hilo de la interfaz.
 */
@Service
@Slf4j
public class ComponentLibrary {

    /** Lado mayor en planta, en unidades del visor, al que se escala todo modelo: asi todos se ven al mismo tamano. */
    public static final double VIEW_SIZE = 300;

    private final List<ComponentInfo> components;
    private final Map<String, String> materialLabels;
    private final Map<String, SpecDoc> specs = new ConcurrentHashMap<>();

    public ComponentLibrary(ObjectMapper mapper) {
        List<ComponentInfo> found = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream("/components/index.json")) {
            if (in != null) {
                for (ComponentInfo c : mapper.readValue(in, new TypeReference<List<ComponentInfo>>() { })) {
                    if (getClass().getResource(c.objPath()) != null) {
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

    public List<ComponentInfo> all() {
        return components;
    }

    /** Modelo listo para el visor, escalado a {@link #VIEW_SIZE}. Los archivos de la biblioteca tienen +Z hacia arriba. */
    public ExternalModel.Loaded load(ComponentInfo c) throws IOException {
        return ExternalModel.load(ExternalModel.resource(c.objPath()), ExternalModel.Up.Z, VIEW_SIZE);
    }

    /** Ficha tecnica del componente; vacia si no tiene o no se puede leer. */
    public SpecDoc spec(ComponentInfo c) {
        return specs.computeIfAbsent(c.id(), id -> {
            try (InputStream in = getClass().getResourceAsStream(c.specPath())) {
                if (in != null) {
                    return SpecMarkdown.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudo leer la ficha de {}: {}", c.id(), e.getMessage());
            }
            return new SpecDoc(null, List.of(), List.of());
        });
    }

    /** Nombre legible de un material del modelo; si no esta en la tabla, su identificador con espacios. */
    public String materialLabel(String material) {
        if (material == null || material.isBlank()) {
            return "Sin material";
        }
        String label = materialLabels.get(material);
        return label != null ? label : material.replace('_', ' ').replaceAll("(?<=[a-z])(?=[A-Z])", " ").trim();
    }
}

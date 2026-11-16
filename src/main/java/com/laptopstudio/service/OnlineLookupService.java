package com.laptopstudio.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.WebSearchTool20260209;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.exception.NegocioException;
import com.laptopstudio.model.Laptop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta en internet las especificaciones de un laptop que no esta en el catalogo. Es la unica parte de la
 * aplicacion que usa red: llama a la API de Claude con la herramienta de busqueda web, que lee las paginas y
 * devuelve la ficha en JSON junto a las direcciones de donde salio cada dato. No guarda nada: el usuario revisa
 * el resultado y decide si lo incorpora. Bloquea mientras espera a la red; llamar fuera del hilo de la interfaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineLookupService {

    private static final String MODEL = "claude-opus-5-5";
    private static final int MAX_CONTINUATIONS = 4;

    private static final String SYSTEM = """
            Eres el buscador de fichas técnicas de una aplicación de catálogo de laptops. El usuario escribe una marca, \
            un modelo o unas características. Tu trabajo es identificar UN modelo concreto de laptop y devolver su \
            ficha técnica verificada con la búsqueda web.

            Fuentes: usa primero la página oficial del fabricante (ficha de producto, PSREF, manual de servicio) y \
            después análisis técnicos reconocidos (por ejemplo Notebookcheck). No uses tiendas de terceros ni foros \
            como única fuente de un dato.

            Exactitud: muchos laptops se venden en varias configuraciones. Elige una configuración concreta, la que \
            mejor encaje con lo que pidió el usuario, y da todos los datos de ESA configuración. Si un dato no \
            aparece en las fuentes, déjalo en null: un campo vacío es correcto, un valor supuesto no. Si no puedes \
            confirmar que los datos corresponden exactamente al modelo pedido, pon exact_match en false y explica \
            por qué en match_note. Si la consulta es demasiado vaga para identificar un único modelo, devuelve \
            found en false y propón en match_note los modelos concretos entre los que el usuario debería elegir.

            Modelo 3D: rellena model_3d_obj_url solo si encuentras un enlace de descarga directa a un archivo .obj \
            de ese mismo equipo. Una página de visualización o que exige iniciar sesión no sirve: en ese caso, null.

            Responde únicamente con un objeto JSON dentro de un bloque ```json, sin texto antes ni después, con \
            estos campos (números sin unidades):
            {
              "found": true,
              "exact_match": true,
              "match_note": "configuración elegida y cualquier duda, en español",
              "sources": ["https://..."],
              "model_3d_obj_url": null,
              "laptop": {
                "brand": "", "model": "", "category": "Gaming | Ultrabook | Workstation | Creador | Profesional | Uso general",
                "releaseYear": 2026, "availability": "Disponible | Anunciado",
                "cpu": "", "cpuCores": 0, "cpuBaseWatts": 0, "cpuBoostWatts": 0,
                "gpu": "", "gpuVramGb": 0, "gpuTgpWatts": 0,
                "ramGb": 0, "ramType": "", "ramUpgradable": true,
                "storageGb": 0, "storageSlots": 0,
                "screenInches": 0.0, "resolution": "2560x1600", "refreshRateHz": 0, "panelType": "",
                "weightKg": 0.0, "thicknessMm": 0.0, "widthMm": 0.0, "depthMm": 0.0, "batteryWh": 0.0,
                "chassisMaterial": "", "priceUsd": 0
              }
            }
            priceUsd es el precio de lista en dólares de esa configuración según una fuente; null si no lo encuentras.""";

    private final AppConfig config;
    private final ObjectMapper mapper;

    /** Ficha encontrada. {@code laptop} no esta guardado; {@code exactMatch} falso exige revision del usuario. */
    public record Found(Laptop laptop, List<String> sources, boolean exactMatch, String note, String model3dUrl) {
    }

    public boolean hasKey() {
        return key() != null;
    }

    private String key() {
        String env = System.getenv("ANTHROPIC_API_KEY");
        String k = env != null && !env.isBlank() ? env : config.get(AppConfig.API_KEY, null);
        return k == null || k.isBlank() ? null : k.trim();
    }

    public Found buscar(String consulta) {
        String key = key();
        if (key == null) {
            throw new NegocioException("Falta la clave de la API de Claude. Escríbela en Ajustes > Consulta en internet.");
        }
        if (consulta == null || consulta.isBlank()) {
            throw new NegocioException("Escribe la marca y el modelo que quieres buscar.");
        }
        AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(key).build();
        try {
            String text;
            try {
                text = ask(client, consulta.trim(), true);
            } catch (BadRequestException e) {
                // Algunas cuentas o plataformas no aceptan el parametro de respaldo ante rechazos: se repite sin el
                log.info("Reintentando la consulta sin 'fallbacks': {}", e.getMessage());
                text = ask(client, consulta.trim(), false);
            }
            return parse(text);
        } catch (UnauthorizedException e) {
            throw new NegocioException("La clave de la API de Claude no es válida. Revísala en Ajustes.");
        } catch (RateLimitException e) {
            throw new NegocioException("La API alcanzó su límite de uso. Espera un momento y vuelve a intentarlo.");
        } catch (AnthropicServiceException e) {
            throw new NegocioException("La API respondió con un error (" + e.statusCode() + "): " + e.getMessage());
        } catch (AnthropicIoException e) {
            throw new NegocioException("No hay conexión con la API. Comprueba tu conexión a internet.");
        } finally {
            client.close();
        }
    }

    /** Hace la peticion y reanuda las pausas de la busqueda web hasta tener la respuesta final en texto. */
    private String ask(AnthropicClient client, String consulta, boolean withFallbacks) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                .system(SYSTEM)
                .addTool(WebSearchTool20260209.builder().maxUses(8L).build())
                .addUserMessage(consulta);
        if (withFallbacks) {
            // Si un clasificador de seguridad rechaza la peticion, el servidor la reintenta en otro modelo
            builder.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        Message response = client.messages().create(builder.build());
        for (int i = 0; i < MAX_CONTINUATIONS && isPaused(response); i++) {
            // pause_turn: la busqueda web aun no termino; se reenvia lo recibido y el servidor continua
            builder.addMessage(response);
            response = client.messages().create(builder.build());
        }
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new NegocioException("El servicio no pudo atender esta consulta. Prueba a escribir marca y modelo exactos.");
        }
        if (response.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false) || isPaused(response)) {
            throw new NegocioException("La consulta no llegó a terminar. Prueba con una búsqueda más concreta.");
        }
        StringBuilder sb = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> sb.append(t.text())));
        return sb.toString();
    }

    private static boolean isPaused(Message m) {
        return m.stopReason().map(StopReason.PAUSE_TURN::equals).orElse(false);
    }

    private Found parse(String text) {
        int from = text.indexOf('{');
        int to = text.lastIndexOf('}');
        if (from < 0 || to <= from) {
            throw new NegocioException("La consulta no devolvió una ficha. Prueba a escribir marca y modelo exactos.");
        }
        try {
            JsonNode root = mapper.readTree(text.substring(from, to + 1));
            String note = root.path("match_note").asText("");
            if (!root.path("found").asBoolean(false) || !root.hasNonNull("laptop")) {
                throw new NegocioException(note.isBlank() ? "No se encontró ese laptop." : "No se identificó un único modelo. " + note);
            }
            Laptop l = mapper.copy()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .treeToValue(root.get("laptop"), Laptop.class);
            l.setId(null);
            sinCeros(l);
            if (blank(l.getBrand()) || blank(l.getModel()) || blank(l.getCpu()) || blank(l.getGpu())) {
                throw new NegocioException("Las fuentes no dieron marca, modelo, procesador y gráficos. " + note);
            }
            if (blank(l.getCategory())) {
                l.setCategory("Sin clasificar");
            }
            List<String> sources = new ArrayList<>();
            root.path("sources").forEach(s -> {
                if (s.asText("").startsWith("http")) {
                    sources.add(s.asText());
                }
            });
            if (sources.isEmpty()) {
                throw new NegocioException("La consulta no indicó de qué páginas salieron los datos, así que se descartó.");
            }
            l.setSourceUrl(cut(sources.get(0), 300));
            l.setNotes(cut("Incorporado desde internet. " + note, 400));
            l.setPriceEstimated(l.getPriceUsd() != null ? Boolean.TRUE : null);
            // solo se ofrece un enlace https: es lo unico que ModelStore acepta descargar
            String obj = root.path("model_3d_obj_url").asText("").trim();
            String objLower = obj.toLowerCase(java.util.Locale.ROOT);
            return new Found(l, sources, root.path("exact_match").asBoolean(false), note,
                    objLower.startsWith("https://") && objLower.contains(".obj") ? obj : null);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new NegocioException("La respuesta de la consulta no se pudo leer. Vuelve a intentarlo.");
        }
    }

    /** Un 0 en la respuesta significa "no encontrado": se guarda como dato ausente, no como cero. */
    private static void sinCeros(Laptop l) {
        l.setCpuCores(nz(l.getCpuCores()));
        l.setCpuBaseWatts(nz(l.getCpuBaseWatts()));
        l.setCpuBoostWatts(nz(l.getCpuBoostWatts()));
        l.setGpuVramGb(nz(l.getGpuVramGb()));
        l.setGpuTgpWatts(nz(l.getGpuTgpWatts()));
        l.setRamGb(nz(l.getRamGb()));
        l.setStorageGb(nz(l.getStorageGb()));
        l.setStorageSlots(nz(l.getStorageSlots()));
        l.setRefreshRateHz(nz(l.getRefreshRateHz()));
        l.setPriceUsd(nz(l.getPriceUsd()));
        l.setScreenInches(nz(l.getScreenInches()));
        l.setWeightKg(nz(l.getWeightKg()));
        l.setThicknessMm(nz(l.getThicknessMm()));
        l.setWidthMm(nz(l.getWidthMm()));
        l.setDepthMm(nz(l.getDepthMm()));
        l.setBatteryWh(nz(l.getBatteryWh()));
    }

    private static Integer nz(Integer v) {
        return v == null || v <= 0 ? null : v;
    }

    private static Double nz(Double v) {
        return v == null || v <= 0 ? null : v;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}

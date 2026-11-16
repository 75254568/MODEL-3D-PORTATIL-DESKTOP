package com.laptopstudio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Preferencias del usuario, guardadas en {@code config.properties} junto a la base de datos.
 * Es el unico sitio que lee y escribe ese archivo; el resto de la aplicacion pide valores por clave.
 */
@Component
@Slf4j
public class AppConfig {

    public static final String CURRENCY = "moneda";
    public static final String PEN_RATE = "tipo_cambio_pen";
    public static final String PEN_RATE_NOTE = "tipo_cambio_pen_origen";
    public static final String API_KEY = "anthropic_api_key";

    private final Path file = Path.of(System.getProperty("user.home"), ".laptop-comparator", "config.properties");
    private final Properties props = new Properties();

    public AppConfig() {
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException e) {
                log.warn("No se pudo leer {}: {}", file, e.getMessage());
            }
        }
    }

    public String get(String key, String fallback) {
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    public synchronized void set(String key, String value) {
        if (value == null || value.isBlank()) {
            props.remove(key);
        } else {
            props.setProperty(key, value.trim());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(w, "Laptop Explorer");
            }
        } catch (IOException e) {
            log.warn("No se pudo guardar {}: {}", file, e.getMessage());
        }
    }

    public Path dataDir() {
        return file.getParent();
    }
}

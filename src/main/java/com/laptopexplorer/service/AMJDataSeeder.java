package com.laptopexplorer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.repository.AMJLaptopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Al arrancar carga en la base de datos los modelos del catálogo de referencia ({@code seed/laptops.json}) que
 * todavía no se hayan cargado nunca. Cada modelo cargado se anota en la tabla {@code seed_aplicado} para que un
 * modelo que el usuario elimine no vuelva a aparecer en el siguiente arranque. La lista esta en la misma base
 * que el catálogo a propósito: si la base se pierde, la lista se pierde con ella y el catálogo se carga de nuevo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AMJDataSeeder implements ApplicationRunner {

    /** Marca de la tarea de una sola vez que da precio de referencia a los modelos que no tienen ninguno. */
    private static final String PRECIOS = "#precios-de-referencia";

    private final AMJLaptopRepository repository;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final AMJAppConfig config;
    private final AMJVerifiedSpecs fichas;

    private static String amjKey(AMJLaptop l) {
        return (l.getBrand() + "|" + l.getModel()).toLowerCase(Locale.ROOT);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> aplicados = new HashSet<>(jdbc.queryForList("SELECT clave FROM seed_aplicado", String.class));
        amjImportarListaAntigua(aplicados);
        // laptops.json: catálogo original; laptops-2026.json: modelos 2026 con datos tomados de fuentes citadas
        for (String archivo : List.of("seed/laptops.json", "seed/laptops-2026.json")) {
            amjSembrar(archivo, aplicados);
        }
        if (!aplicados.contains(PRECIOS)) {
            amjRellenarPrecios();
            amjMarcar(PRECIOS, aplicados);
        }
        fichas.amjAplicar(aplicados, clave -> amjMarcar(clave, aplicados));
    }

    private void amjMarcar(String clave, Set<String> aplicados) {
        jdbc.update("INSERT OR IGNORE INTO seed_aplicado (clave) VALUES (?)", clave);
        aplicados.add(clave);
    }

    /**
     * Las versiones anteriores guardaban la lista en {@code seed-aplicado.txt}, junto a la base. Se pasa a la tabla
     * una sola vez y el archivo se borra. Si el catálogo esta vacío, el archivo no corresponde a esta base (es nueva
     * o se borro la anterior) y se descarta sin leerlo: de lo contrario el catálogo se quedaria vacío para siempre.
     */
    private void amjImportarListaAntigua(Set<String> aplicados) {
        Path antiguo = config.amjDataDir().resolve("seed-aplicado.txt");
        if (!Files.exists(antiguo)) {
            return;
        }
        try {
            if (aplicados.isEmpty() && repository.count() > 0) {
                for (String linea : Files.readAllLines(antiguo, StandardCharsets.UTF_8)) {
                    if (!linea.isBlank()) {
                        amjMarcar(linea.trim(), aplicados);
                    }
                }
                log.info("Lista de modelos ya cargados pasada a la base de datos: {}", aplicados.size());
            }
            Files.delete(antiguo);
        } catch (IOException e) {
            log.warn("No se pudo leer o borrar {}: {}", antiguo, e.getMessage());
        }
    }

    private void amjSembrar(String archivo, Set<String> aplicados) throws Exception {
        try (InputStream in = new ClassPathResource(archivo).getInputStream()) {
            AMJLaptop[] lote = mapper.readValue(in, AMJLaptop[].class);
            int nuevos = 0;
            for (AMJLaptop l : lote) {
                String k = amjKey(l);
                if (aplicados.contains(k)) {
                    continue;
                }
                try {
                    if (repository.findByBrandIgnoreCaseAndModelIgnoreCase(l.getBrand(), l.getModel()).isEmpty()) {
                        amjPrecioDeReferencia(l);
                        repository.save(l);
                        nuevos++;
                    }
                    amjMarcar(k, aplicados);
                } catch (RuntimeException e) {
                    log.warn("No se pudo cargar {}: {}", l.getModel(), e.getMessage());
                }
            }
            log.info("{}: {} modelos nuevos de {}", archivo, nuevos, lote.length);
        }
    }

    /** Da al laptop su precio de referencia aproximado si no trae ninguno. Devuelve verdadero si lo cambio. */
    private static boolean amjPrecioDeReferencia(AMJLaptop l) {
        if (l.getPriceUsd() != null) {
            return false;
        }
        Integer p = AMJPrecioReferencia.amjDe(l.getBrand(), l.getModel());
        if (p == null) {
            return false;
        }
        l.setPriceUsd(p);
        l.setPriceEstimated(Boolean.TRUE);
        return true;
    }

    /**
     * Tarea de una sola vez para bases creadas antes de que existiera el precio: da el de referencia a los
     * modelos sin precio. No se repite en cada arranque, así que un precio que el usuario borre sigue borrado.
     */
    private void amjRellenarPrecios() {
        int conPrecio = 0;
        for (AMJLaptop l : repository.findAll()) {
            if (amjPrecioDeReferencia(l)) {
                repository.save(l);
                conPrecio++;
            }
        }
        if (conPrecio > 0) {
            log.info("Precios de referencia aproximados cargados: {}", conPrecio);
        }
    }
}

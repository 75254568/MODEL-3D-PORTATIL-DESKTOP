package com.laptopstudio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.repository.LaptopRepository;
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
 * Al arrancar carga en la base de datos los modelos del catalogo de referencia ({@code seed/laptops.json}) que
 * todavia no se hayan cargado nunca. Cada modelo cargado se anota en la tabla {@code seed_aplicado} para que un
 * modelo que el usuario elimine no vuelva a aparecer en el siguiente arranque. La lista esta en la misma base
 * que el catalogo a proposito: si la base se pierde, la lista se pierde con ella y el catalogo se carga de nuevo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    /** Marca de la tarea de una sola vez que da precio de referencia a los modelos que no tienen ninguno. */
    private static final String PRECIOS = "#precios-de-referencia";

    private final LaptopRepository repository;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final AppConfig config;
    private final VerifiedSpecs fichas;

    private static String key(Laptop l) {
        return (l.getBrand() + "|" + l.getModel()).toLowerCase(Locale.ROOT);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> aplicados = new HashSet<>(jdbc.queryForList("SELECT clave FROM seed_aplicado", String.class));
        importarListaAntigua(aplicados);
        // laptops.json: catalogo original; laptops-2026.json: modelos 2026 con datos tomados de fuentes citadas
        for (String archivo : List.of("seed/laptops.json", "seed/laptops-2026.json")) {
            sembrar(archivo, aplicados);
        }
        if (!aplicados.contains(PRECIOS)) {
            rellenarPrecios();
            marcar(PRECIOS, aplicados);
        }
        fichas.aplicar(aplicados, clave -> marcar(clave, aplicados));
    }

    private void marcar(String clave, Set<String> aplicados) {
        jdbc.update("INSERT OR IGNORE INTO seed_aplicado (clave) VALUES (?)", clave);
        aplicados.add(clave);
    }

    /**
     * Las versiones anteriores guardaban la lista en {@code seed-aplicado.txt}, junto a la base. Se pasa a la tabla
     * una sola vez y el archivo se borra. Si el catalogo esta vacio, el archivo no corresponde a esta base (es nueva
     * o se borro la anterior) y se descarta sin leerlo: de lo contrario el catalogo se quedaria vacio para siempre.
     */
    private void importarListaAntigua(Set<String> aplicados) {
        Path antiguo = config.dataDir().resolve("seed-aplicado.txt");
        if (!Files.exists(antiguo)) {
            return;
        }
        try {
            if (aplicados.isEmpty() && repository.count() > 0) {
                for (String linea : Files.readAllLines(antiguo, StandardCharsets.UTF_8)) {
                    if (!linea.isBlank()) {
                        marcar(linea.trim(), aplicados);
                    }
                }
                log.info("Lista de modelos ya cargados pasada a la base de datos: {}", aplicados.size());
            }
            Files.delete(antiguo);
        } catch (IOException e) {
            log.warn("No se pudo leer o borrar {}: {}", antiguo, e.getMessage());
        }
    }

    private void sembrar(String archivo, Set<String> aplicados) throws Exception {
        try (InputStream in = new ClassPathResource(archivo).getInputStream()) {
            Laptop[] lote = mapper.readValue(in, Laptop[].class);
            int nuevos = 0;
            for (Laptop l : lote) {
                String k = key(l);
                if (aplicados.contains(k)) {
                    continue;
                }
                try {
                    if (repository.findByBrandIgnoreCaseAndModelIgnoreCase(l.getBrand(), l.getModel()).isEmpty()) {
                        precioDeReferencia(l);
                        repository.save(l);
                        nuevos++;
                    }
                    marcar(k, aplicados);
                } catch (RuntimeException e) {
                    log.warn("No se pudo cargar {}: {}", l.getModel(), e.getMessage());
                }
            }
            log.info("{}: {} modelos nuevos de {}", archivo, nuevos, lote.length);
        }
    }

    /** Da al laptop su precio de referencia aproximado si no trae ninguno. Devuelve verdadero si lo cambio. */
    private static boolean precioDeReferencia(Laptop l) {
        if (l.getPriceUsd() != null) {
            return false;
        }
        Integer p = PrecioReferencia.de(l.getBrand(), l.getModel());
        if (p == null) {
            return false;
        }
        l.setPriceUsd(p);
        l.setPriceEstimated(Boolean.TRUE);
        return true;
    }

    /**
     * Tarea de una sola vez para bases creadas antes de que existiera el precio: da el de referencia a los
     * modelos sin precio. No se repite en cada arranque, asi que un precio que el usuario borre sigue borrado.
     */
    private void rellenarPrecios() {
        int conPrecio = 0;
        for (Laptop l : repository.findAll()) {
            if (precioDeReferencia(l)) {
                repository.save(l);
                conPrecio++;
            }
        }
        if (conPrecio > 0) {
            log.info("Precios de referencia aproximados cargados: {}", conPrecio);
        }
    }
}

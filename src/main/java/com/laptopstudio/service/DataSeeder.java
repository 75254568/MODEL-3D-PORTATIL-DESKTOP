package com.laptopstudio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.repository.LaptopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

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
 * todavia no se hayan cargado nunca. Cada modelo cargado se anota en {@code seed-aplicado.txt}, junto a la base
 * de datos, para que un modelo que el usuario elimine no vuelva a aparecer en el siguiente arranque.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final LaptopRepository repository;
    private final ObjectMapper mapper;

    private static String key(Laptop l) {
        return (l.getBrand() + "|" + l.getModel()).toLowerCase(Locale.ROOT);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path marca = Path.of(System.getProperty("user.home"), ".laptop-comparator", "seed-aplicado.txt");
        Set<String> aplicados = new HashSet<>();
        if (Files.exists(marca)) {
            aplicados.addAll(Files.readAllLines(marca, StandardCharsets.UTF_8));
        }
        try (InputStream in = new ClassPathResource("seed/laptops.json").getInputStream()) {
            Laptop[] lote = mapper.readValue(in, Laptop[].class);
            int nuevos = 0;
            for (Laptop l : lote) {
                String k = key(l);
                if (aplicados.contains(k)) {
                    continue;
                }
                try {
                    if (repository.findByBrandIgnoreCaseAndModelIgnoreCase(l.getBrand(), l.getModel()).isEmpty()) {
                        repository.save(l);
                        nuevos++;
                    }
                    aplicados.add(k);
                } catch (RuntimeException e) {
                    log.warn("No se pudo cargar {}: {}", l.getModel(), e.getMessage());
                }
            }
            Files.write(marca, List.copyOf(aplicados), StandardCharsets.UTF_8);
            log.info("Catalogo de referencia: {} modelos nuevos de {}", nuevos, lote.length);
        }
    }
}

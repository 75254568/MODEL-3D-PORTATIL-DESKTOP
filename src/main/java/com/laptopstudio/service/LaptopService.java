package com.laptopstudio.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstudio.exception.NegocioException;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.repository.LaptopRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LaptopService {

    public static final int TAMANIO_PAGINA = 15;

    private final LaptopRepository repository;
    private final ObjectMapper mapper;

    public Page<Laptop> listar(String filtro, int pagina) {
        Pageable pageable = PageRequest.of(pagina, TAMANIO_PAGINA, Sort.by("id").ascending());
        if (filtro == null || filtro.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.buscarPorTexto(filtro.trim(), pageable);
    }

    public List<Laptop> todos() {
        return repository.findAll(Sort.by("brand").ascending().and(Sort.by("model").ascending()));
    }

    public Optional<Laptop> buscarPorModelo(String fragmento) {
        return repository.findFirstByModelContainingIgnoreCase(fragmento);
    }

    public Optional<Laptop> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Laptop guardar(@Valid Laptop laptop) {
        repository.findByBrandIgnoreCaseAndModelIgnoreCase(laptop.getBrand(), laptop.getModel())
                .ifPresent(existente -> {
                    boolean esOtro = laptop.getId() == null || !existente.getId().equals(laptop.getId());
                    if (esOtro) {
                        throw new NegocioException(
                                "Ya existe un portatil " + laptop.getBrand() + " " + laptop.getModel());
                    }
                });
        Laptop guardado = repository.save(laptop);
        log.info("Portatil guardado: id={} {}", guardado.getId(), guardado.getDisplayName());
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NegocioException("El portatil ya no existe en la base de datos.");
        }
        repository.deleteById(id);
    }

    public long contar() {
        return repository.count();
    }

    /** Diferenciales porcentuales de A respecto a B (grosor, peso, GPU, bateria, CPU). */
    public Comparison comparar(Laptop a, Laptop b) {
        List<Comparison.Delta> d = new ArrayList<>();
        d.add(delta("Espesor", a.getThicknessMm(), b.getThicknessMm(), "mm", true));
        d.add(delta("Peso", a.getWeightKg(), b.getWeightKg(), "kg", true));
        d.add(delta("Vatios GPU", num(a.getGpuTgpWatts()), num(b.getGpuTgpWatts()), "W", false));
        d.add(delta("Bateria", a.getBatteryWh(), b.getBatteryWh(), "Wh", false));
        d.add(delta("Vatios CPU", num(a.getCpuBoostWatts()), num(b.getCpuBoostWatts()), "W", false));
        return new Comparison(d);
    }

    private static Double num(Integer v) {
        return v == null ? null : v.doubleValue();
    }

    private static Comparison.Delta delta(String label, Double a, Double b, String unit, boolean lowerIsBetter) {
        boolean ok = a != null && b != null && a > 0 && b > 0;
        return new Comparison.Delta(label, ok ? a : 0, ok ? b : 0, unit, ok, lowerIsBetter);
    }

    /**
     * Importa un CSV cuya primera fila contiene los nombres de los campos de {@link Laptop}.
     * Devuelve el numero de filas guardadas y las incidencias de las que no se pudieron guardar.
     */
    public ImportResult importarCsv(Path archivo) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.size() < 2) {
            throw new NegocioException("El CSV debe tener una fila de cabecera y al menos un registro.");
        }
        ObjectMapper lector = mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<String> cabecera = parseCsvLine(lineas.get(0));
        int guardados = 0;
        List<String> incidencias = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            List<String> celdas = parseCsvLine(lineas.get(i));
            Map<String, Object> fila = new LinkedHashMap<>();
            for (int c = 0; c < cabecera.size() && c < celdas.size(); c++) {
                String valor = celdas.get(c).trim();
                if (!valor.isEmpty()) {
                    fila.put(cabecera.get(c).trim(), valor);
                }
            }
            try {
                Laptop l = lector.convertValue(fila, Laptop.class);
                l.setId(null);
                guardar(l);
                guardados++;
            } catch (ConstraintViolationException e) {
                String msg = e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage).findFirst().orElse("datos invalidos");
                incidencias.add("Fila " + (i + 1) + ": " + msg);
            } catch (Exception e) {
                incidencias.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new ImportResult(guardados, incidencias);
    }

    public record ImportResult(int guardados, List<String> incidencias) {
    }

    static List<String> parseCsvLine(String linea) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (ch == '"') {
                if (comillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    comillas = !comillas;
                }
            } else if (ch == ',' && !comillas) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        return out;
    }
}

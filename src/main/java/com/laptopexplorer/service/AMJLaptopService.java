package com.laptopexplorer.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopexplorer.exception.AMJNegocioException;
import com.laptopexplorer.model.AMJLaptop;
import com.laptopexplorer.repository.AMJLaptopRepository;
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
public class AMJLaptopService {

    public static final int TAMANIO_PAGINA = 15;

    private final AMJLaptopRepository repository;
    private final ObjectMapper mapper;

    public Page<AMJLaptop> amjListar(String filtro, int pagina) {
        Pageable pageable = PageRequest.of(pagina, TAMANIO_PAGINA, Sort.by("id").ascending());
        if (filtro == null || filtro.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.amjBuscarPorTexto(filtro.trim(), pageable);
    }

    public List<AMJLaptop> amjTodos() {
        return repository.findAll(Sort.by("brand").ascending().and(Sort.by("model").ascending()));
    }

    public Optional<AMJLaptop> amjBuscarPorModelo(String fragmento) {
        return repository.findFirstByModelContainingIgnoreCase(fragmento);
    }

    public Optional<AMJLaptop> amjBuscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public AMJLaptop amjGuardar(@Valid AMJLaptop laptop) {
        repository.findByBrandIgnoreCaseAndModelIgnoreCase(laptop.getBrand(), laptop.getModel())
                .ifPresent(existente -> {
                    boolean esOtro = laptop.getId() == null || !existente.getId().equals(laptop.getId());
                    if (esOtro) {
                        throw new AMJNegocioException(
                                "Ya existe un portátil " + laptop.getBrand() + " " + laptop.getModel());
                    }
                });
        AMJLaptop guardado = repository.save(laptop);
        log.info("Portátil guardado: id={} {}", guardado.getId(), guardado.amjGetDisplayName());
        return guardado;
    }

    @Transactional
    public void amjEliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new AMJNegocioException("El portátil ya no existe en la base de datos.");
        }
        repository.deleteById(id);
    }

    public long amjContar() {
        return repository.count();
    }

    /** Diferenciales porcentuales de A respecto a B (grosor, peso, GPU, batería, CPU). */
    public AMJComparison amjComparar(AMJLaptop a, AMJLaptop b) {
        List<AMJComparison.AMJDelta> d = new ArrayList<>();
        d.add(amjDelta("Espesor", a.getThicknessMm(), b.getThicknessMm(), "mm", true));
        d.add(amjDelta("Peso", a.getWeightKg(), b.getWeightKg(), "kg", true));
        d.add(amjDelta("Vatios GPU", amjNum(a.getGpuTgpWatts()), amjNum(b.getGpuTgpWatts()), "W", false));
        d.add(amjDelta("Batería", a.getBatteryWh(), b.getBatteryWh(), "Wh", false));
        d.add(amjDelta("Vatios CPU", amjNum(a.getCpuBoostWatts()), amjNum(b.getCpuBoostWatts()), "W", false));
        return new AMJComparison(d);
    }

    private static Double amjNum(Integer v) {
        return v == null ? null : v.doubleValue();
    }

    private static AMJComparison.AMJDelta amjDelta(String label, Double a, Double b, String unit, boolean lowerIsBetter) {
        boolean ok = a != null && b != null && a > 0 && b > 0;
        return new AMJComparison.AMJDelta(label, ok ? a : 0, ok ? b : 0, unit, ok, lowerIsBetter);
    }

    /**
     * Importa un CSV cuya primera fila contiene los nombres de los campos de {@link AMJLaptop}.
     * Devuelve el número de filas guardadas y las incidencias de las que no se pudieron guardar.
     */
    public AMJImportResult amjImportarCsv(Path archivo) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.size() < 2) {
            throw new AMJNegocioException("El CSV debe tener una fila de cabecera y al menos un registro.");
        }
        ObjectMapper lector = mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<String> cabecera = amjParseCsvLine(lineas.get(0));
        int guardados = 0;
        List<String> incidencias = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            List<String> celdas = amjParseCsvLine(lineas.get(i));
            Map<String, Object> fila = new LinkedHashMap<>();
            for (int c = 0; c < cabecera.size() && c < celdas.size(); c++) {
                String valor = celdas.get(c).trim();
                if (!valor.isEmpty()) {
                    fila.put(cabecera.get(c).trim(), valor);
                }
            }
            try {
                AMJLaptop l = lector.convertValue(fila, AMJLaptop.class);
                l.setId(null);
                amjGuardar(l);
                guardados++;
            } catch (ConstraintViolationException e) {
                String msg = e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage).findFirst().orElse("datos invalidos");
                incidencias.add("Fila " + (i + 1) + ": " + msg);
            } catch (Exception e) {
                incidencias.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new AMJImportResult(guardados, incidencias);
    }

    public record AMJImportResult(int guardados, List<String> incidencias) {
    }

    static List<String> amjParseCsvLine(String linea) {
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

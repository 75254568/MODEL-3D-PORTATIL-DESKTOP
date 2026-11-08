package com.laptopstudio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "laptop",
       uniqueConstraints = @UniqueConstraint(name = "uk_laptop_brand_model", columnNames = {"brand", "model"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Laptop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String brand;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String model;

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String category;

    @NotBlank(message = "El procesador es obligatorio")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String cpu;

    @Min(value = 1, message = "Los nucleos deben ser al menos 1")
    private Integer cpuCores;

    @Min(value = 0, message = "Los vatios base no pueden ser negativos")
    private Integer cpuBaseWatts;

    @Min(value = 0, message = "Los vatios boost no pueden ser negativos")
    private Integer cpuBoostWatts;

    @NotBlank(message = "La GPU es obligatoria")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String gpu;

    @Min(0)
    private Integer gpuVramGb;

    @Min(0)
    private Integer gpuTgpWatts;

    @Min(value = 1, message = "La RAM debe ser al menos 1 GB")
    private Integer ramGb;

    @Size(max = 40)
    @Column(length = 40)
    private String ramType;

    private Boolean ramUpgradable;

    @Min(value = 1, message = "El almacenamiento debe ser al menos 1 GB")
    private Integer storageGb;

    @Min(0)
    private Integer storageSlots;

    @DecimalMin(value = "5.0", message = "La pantalla debe ser de al menos 5 pulgadas")
    private Double screenInches;

    @Size(max = 20)
    @Column(length = 20)
    private String resolution;

    @Min(0)
    private Integer refreshRateHz;

    @Size(max = 40)
    @Column(length = 40)
    private String panelType;

    @DecimalMin(value = "0.1", message = "El peso debe ser mayor que 0")
    private Double weightKg;

    @DecimalMin(value = "1.0", message = "El grosor debe ser mayor que 0")
    private Double thicknessMm;

    @DecimalMin(value = "100.0", message = "El ancho debe ser mayor que 100 mm")
    private Double widthMm;

    @DecimalMin(value = "100.0", message = "El fondo debe ser mayor que 100 mm")
    private Double depthMm;

    @DecimalMin(value = "1.0", message = "La bateria debe ser mayor que 0")
    private Double batteryWh;

    @Size(max = 300)
    @Column(length = 300)
    private String imageUrl;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color debe tener formato #rrggbb")
    @Column(length = 7)
    private String colorHex;

    @Size(max = 60)
    @Column(length = 60)
    private String chassisMaterial;

    @Size(max = 300)
    @Column(length = 300)
    private String sourceUrl;

    @Size(max = 400)
    @Column(length = 400)
    private String notes;

    public String getDisplayName() {
        return brand + " " + model;
    }
}

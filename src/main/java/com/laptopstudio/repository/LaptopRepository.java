package com.laptopstudio.repository;

import com.laptopstudio.model.Laptop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LaptopRepository extends JpaRepository<Laptop, Long> {

    Optional<Laptop> findByModel(String model);

    Optional<Laptop> findFirstByModelContainingIgnoreCase(String fragment);

    Optional<Laptop> findByBrandIgnoreCaseAndModelIgnoreCase(String brand, String model);

    @Query("""
            SELECT l FROM Laptop l
            WHERE LOWER(l.brand) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.model) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.cpu)   LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.gpu)   LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.category) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Laptop> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
}

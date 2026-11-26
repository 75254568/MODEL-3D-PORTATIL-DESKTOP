package com.laptopexplorer.repository;

import com.laptopexplorer.model.AMJLaptop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AMJLaptopRepository extends JpaRepository<AMJLaptop, Long> {

    Optional<AMJLaptop> findByModel(String model);

    Optional<AMJLaptop> findFirstByModelContainingIgnoreCase(String fragment);

    Optional<AMJLaptop> findByBrandIgnoreCaseAndModelIgnoreCase(String brand, String model);

    @Query("""
            SELECT l FROM AMJLaptop l
            WHERE LOWER(l.brand) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.model) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.cpu)   LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.gpu)   LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(l.category) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<AMJLaptop> amjBuscarPorTexto(@Param("texto") String texto, Pageable pageable);
}

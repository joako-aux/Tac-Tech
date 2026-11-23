package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.FotoGuia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoGuiaRepository extends JpaRepository<FotoGuia, String> {

    List<FotoGuia> findByGuia_IdOrderByFechaCapturaAsc(String guiaId);
}

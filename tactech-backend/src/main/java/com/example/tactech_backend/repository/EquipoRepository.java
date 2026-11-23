package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, String> {

    Optional<Equipo> findFirstBySerie(String serie);
}

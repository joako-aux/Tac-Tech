package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByRutAndIdNot(String rut, Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByRut(String rut);

    List<Empresa> findAllByOrderByNombreAsc();
}

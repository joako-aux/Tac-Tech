package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.EstadoSolicitud;
import com.example.tactech_backend.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, String> {

    List<Solicitud> findAllByOrderByFechaCreacionDesc();

    List<Solicitud> findByClienteCorreoIgnoreCaseOrderByFechaCreacionDesc(String correo);

    long countByEstado(EstadoSolicitud estado);

    boolean existsByCliente_Id(Long clienteId);
}

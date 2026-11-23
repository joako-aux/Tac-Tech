package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.Guia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuiaRepository extends JpaRepository<Guia, String> {

    List<Guia> findByTicket_IdOrderByNumeroDesc(String ticketId);

    long countByTicket_Id(String ticketId);

    /** Guias de los tickets asignados a un tecnico. */
    List<Guia> findByTicket_TecnicoCorreoIgnoreCaseOrderByNumeroDesc(String tecnicoCorreo);
}

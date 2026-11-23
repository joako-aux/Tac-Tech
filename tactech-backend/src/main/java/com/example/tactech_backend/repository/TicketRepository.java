package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.EstadoTicket;
import com.example.tactech_backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/** Spring Data genera el SQL a partir del nombre de cada metodo. */
public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findAllByOrderByFechaActualizacionDesc();

    List<Ticket> findByEstadoInOrderByFechaActualizacionDesc(Collection<EstadoTicket> estados);

    List<Ticket> findByTecnicoCorreoIgnoreCaseOrderByFechaActualizacionDesc(String correo);

    long countByEstadoIn(Collection<EstadoTicket> estados);

    long countByEstado(EstadoTicket estado);

    List<Ticket> findBySolicitudIdInOrderByFechaActualizacionDesc(Collection<String> solicitudIds);

    boolean existsByTecnico_Id(Long tecnicoId);

    /** Numero correlativo: el servidor lo asigna al crear el ticket desde una solicitud. */
    @Query("select coalesce(max(t.numero), 0) from Ticket t")
    Integer maxNumero();
}

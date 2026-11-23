package cl.duoc.serviciotecnico.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Propiedad
import java.time.LocalDateTime
import java.util.UUID

/**
 * Solicitud de servicio creada por un cliente para un equipo de frio.
 * Nace siempre en POR_CONFIRMAR; el administrador la convierte en ticket.
 */
@Entity(tableName = "solicitudes", indices = [Index(value = ["clienteCorreo"])])
data class Solicitud(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val clienteCorreo: String,
    val clienteNombre: String? = null,
    val empresaNombre: String? = null,
    val titulo: String,
    val descripcion: String,
    val direccion: String,
    val equipoNombre: String,
    val equipoModelo: String,
    val equipoSerie: String,
    val propiedad: Propiedad,
    val estado: EstadoSolicitud = EstadoSolicitud.POR_CONFIRMAR,
    /** Motivo de cancelacion (cliente) o de rechazo (administrador). */
    val motivo: String? = null,
    /** Ticket creado por el administrador al confirmar. */
    val ticketId: String? = null,
    val fechaCreacion: LocalDateTime = LocalDateTime.now(),
    val fechaActualizacion: LocalDateTime = fechaCreacion,
    val pendienteSync: Boolean = true
)

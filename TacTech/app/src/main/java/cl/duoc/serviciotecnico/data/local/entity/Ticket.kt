package cl.duoc.serviciotecnico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = Equipo::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["equipoId"])]
)
data class Ticket(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val numero: Int,
    val titulo: String,
    val descripcion: String,
    val cliente: String,
    val direccion: String,
    val prioridad: Prioridad,
    val estado: EstadoTicket = EstadoTicket.ABIERTO,
    val equipoId: String,
    val fechaCreacion: LocalDateTime = LocalDateTime.now(),
    val fechaActualizacion: LocalDateTime = fechaCreacion,
    /** Comentario del ultimo cambio de estado. */
    val comentarioEstado: String? = null,
    val fechaCierre: LocalDateTime? = null,
    val solucion: String? = null,
    /** Correo del tecnico asignado por el administrador. */
    val tecnicoCorreo: String = "",
    /** Solicitud del cliente que dio origen al ticket. */
    val solicitudId: String? = null,
    /** true = hay cambios locales que aun no llegan al backend. */
    val pendienteSync: Boolean = true
) {
    /** Cerrado o cancelado = solo lectura. */
    val editable: Boolean
        get() = !estado.esFinal
}

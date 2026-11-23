package cl.duoc.serviciotecnico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "guias",
    foreignKeys = [
        ForeignKey(
            entity = Ticket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Equipo::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["ticketId"]), Index(value = ["equipoId"])]
)
data class Guia(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val numero: Int,
    val ticketId: String,
    val equipoId: String,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fecha: LocalDateTime = LocalDateTime.now(),
    val tecnico: String,
    val trabajoRealizado: String,
    val repuestos: String,
    val observaciones: String,
    val recibidoPor: String,
    /** true = la guia aun no llega al backend. */
    val pendienteSync: Boolean = true
)

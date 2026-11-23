package cl.duoc.serviciotecnico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "fotos_guia",
    foreignKeys = [
        ForeignKey(
            entity = Guia::class,
            parentColumns = ["id"],
            childColumns = ["guiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["guiaId"])]
)
data class FotoGuia(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val guiaId: String,
    val uri: String,
    val fechaCaptura: LocalDateTime = LocalDateTime.now()
)

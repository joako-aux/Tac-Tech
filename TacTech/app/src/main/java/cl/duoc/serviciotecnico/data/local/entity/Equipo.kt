package cl.duoc.serviciotecnico.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.serviciotecnico.domain.model.Propiedad
import java.util.UUID

@Entity(
    tableName = "equipos",
    indices = [Index(value = ["serie"])]
)
data class Equipo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val modelo: String,
    val serie: String,
    val propiedad: Propiedad,
    /** true = hay cambios locales que aun no llegan al backend. */
    val pendienteSync: Boolean = true
)

package cl.duoc.serviciotecnico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import cl.duoc.serviciotecnico.data.local.entity.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipos WHERE id = :id")
    fun observarPorId(id: String): Flow<Equipo?>

    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun obtenerPorId(id: String): Equipo?

    @Query("SELECT * FROM equipos WHERE serie = :serie LIMIT 1")
    suspend fun buscarPorSerie(serie: String): Equipo?

    @Insert
    suspend fun insertar(equipo: Equipo)

    @Update
    suspend fun actualizar(equipo: Equipo)

    // ---------- Sincronizacion ----------

    @Query("SELECT * FROM equipos WHERE pendienteSync = 1")
    suspend fun pendientes(): List<Equipo>

    @Query("SELECT id FROM equipos WHERE pendienteSync = 1")
    suspend fun idsPendientes(): List<String>

    @Query("SELECT COUNT(*) FROM equipos WHERE pendienteSync = 1")
    fun contarPendientes(): Flow<Int>

    @Query("UPDATE equipos SET pendienteSync = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    @Upsert
    suspend fun upsertTodos(equipos: List<Equipo>)
}

package cl.duoc.serviciotecnico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cl.duoc.serviciotecnico.data.local.entity.Guia
import kotlinx.coroutines.flow.Flow

@Dao
interface GuiaDao {
    @Query("SELECT * FROM guias WHERE ticketId = :ticketId ORDER BY numero DESC")
    fun observarPorTicket(ticketId: String): Flow<List<Guia>>

    @Query("SELECT * FROM guias WHERE id = :id")
    fun observarPorId(id: String): Flow<Guia?>

    @Query("SELECT COUNT(*) FROM guias WHERE ticketId = :ticketId")
    suspend fun contarPorTicket(ticketId: String): Int

    @Query("SELECT MAX(numero) FROM guias")
    suspend fun ultimoNumero(): Int?

    @Insert
    suspend fun insertar(guia: Guia)

    // ---------- Sincronizacion ----------

    @Query("SELECT * FROM guias WHERE pendienteSync = 1")
    suspend fun pendientes(): List<Guia>

    @Query("SELECT id FROM guias WHERE pendienteSync = 1")
    suspend fun idsPendientes(): List<String>

    @Query("SELECT COUNT(*) FROM guias WHERE pendienteSync = 1")
    fun contarPendientes(): Flow<Int>

    @Query("UPDATE guias SET pendienteSync = 0 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    @Upsert
    suspend fun upsertTodos(guias: List<Guia>)
}

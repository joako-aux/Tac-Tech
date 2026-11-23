package cl.duoc.serviciotecnico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY fechaActualizacion DESC")
    fun observarTodos(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE tecnicoCorreo = :correo ORDER BY numero DESC")
    fun observarPorTecnico(correo: String): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    fun observarPorId(id: String): Flow<Ticket?>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun obtenerPorId(id: String): Ticket?

    @Query("SELECT MAX(numero) FROM tickets")
    suspend fun ultimoNumero(): Int?

    @Insert
    suspend fun insertar(ticket: Ticket)

    @Update
    suspend fun actualizar(ticket: Ticket)

    // ---------- Sincronizacion ----------

    @Query("SELECT * FROM tickets WHERE pendienteSync = 1")
    suspend fun pendientes(): List<Ticket>

    @Query("SELECT id FROM tickets WHERE pendienteSync = 1")
    suspend fun idsPendientes(): List<String>

    @Query("SELECT COUNT(*) FROM tickets WHERE pendienteSync = 1")
    fun contarPendientes(): Flow<Int>

    /**
     * Se marca sincronizado solo si nadie lo modifico mientras se enviaba:
     * si la fecha de actualizacion cambio, sigue pendiente para el proximo envio.
     */
    @Query("UPDATE tickets SET pendienteSync = 0 WHERE id = :id AND fechaActualizacion = :fechaEnviada")
    suspend fun marcarSincronizado(id: String, fechaEnviada: LocalDateTime)

    /** Inserta o actualiza los tickets que llegan del backend. */
    @Upsert
    suspend fun upsertTodos(tickets: List<Ticket>)
}

package cl.duoc.serviciotecnico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SolicitudDao {
    /** Panel del cliente: solo sus solicitudes. */
    @Query("SELECT * FROM solicitudes WHERE clienteCorreo = :correo ORDER BY fechaCreacion DESC")
    fun observarDeCliente(correo: String): Flow<List<Solicitud>>

    /** Panel del administrador: todas. */
    @Query("SELECT * FROM solicitudes ORDER BY fechaCreacion DESC")
    fun observarTodas(): Flow<List<Solicitud>>

    @Query("SELECT * FROM solicitudes WHERE id = :id")
    fun observarPorId(id: String): Flow<Solicitud?>

    @Query("SELECT * FROM solicitudes WHERE id = :id")
    suspend fun obtenerPorId(id: String): Solicitud?

    @Insert
    suspend fun insertar(solicitud: Solicitud)

    @Update
    suspend fun actualizar(solicitud: Solicitud)

    // ---------- Sincronizacion ----------

    @Query("SELECT * FROM solicitudes WHERE pendienteSync = 1")
    suspend fun pendientes(): List<Solicitud>

    @Query("SELECT id FROM solicitudes WHERE pendienteSync = 1")
    suspend fun idsPendientes(): List<String>

    @Query("SELECT COUNT(*) FROM solicitudes WHERE pendienteSync = 1")
    fun contarPendientes(): Flow<Int>

    @Query("UPDATE solicitudes SET pendienteSync = 0 WHERE id = :id AND fechaActualizacion = :fechaEnviada")
    suspend fun marcarSincronizado(id: String, fechaEnviada: LocalDateTime)

    @Upsert
    suspend fun upsertTodas(solicitudes: List<Solicitud>)
}

package cl.duoc.serviciotecnico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import kotlinx.coroutines.flow.Flow

@Dao
interface FotoGuiaDao {
    @Query("SELECT * FROM fotos_guia WHERE guiaId = :guiaId ORDER BY fechaCaptura")
    fun observarPorGuia(guiaId: String): Flow<List<FotoGuia>>

    @Query("SELECT * FROM fotos_guia WHERE guiaId = :guiaId ORDER BY fechaCaptura")
    suspend fun obtenerPorGuia(guiaId: String): List<FotoGuia>

    @Insert
    suspend fun insertarTodas(fotos: List<FotoGuia>)

    @Upsert
    suspend fun upsertTodas(fotos: List<FotoGuia>)
}

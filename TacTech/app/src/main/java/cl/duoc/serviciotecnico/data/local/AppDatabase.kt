package cl.duoc.serviciotecnico.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.duoc.serviciotecnico.data.local.dao.EquipoDao
import cl.duoc.serviciotecnico.data.local.dao.FotoGuiaDao
import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.dao.SolicitudDao
import cl.duoc.serviciotecnico.data.local.dao.TicketDao
import cl.duoc.serviciotecnico.data.local.entity.Equipo
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket

@Database(
    entities = [Ticket::class, Equipo::class, Guia::class, FotoGuia::class, Solicitud::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun equipoDao(): EquipoDao
    abstract fun guiaDao(): GuiaDao
    abstract fun fotoGuiaDao(): FotoGuiaDao
    abstract fun solicitudDao(): SolicitudDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "servicio_tecnico.db"
                )
                    // Prototipo: al cambiar la version se recrea la base en vez de migrar
                    .fallbackToDestructiveMigration()
                    .build().also { instancia = it }
            }
    }
}
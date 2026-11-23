package cl.duoc.serviciotecnico.data.local

import androidx.room.TypeConverter
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fechaATexto(valor: LocalDateTime?): String? = valor?.toString()

    @TypeConverter
    fun textoAFecha(valor: String?): LocalDateTime? = valor?.let { LocalDateTime.parse(it) }

    @TypeConverter
    fun estadoATexto(valor: EstadoTicket): String = valor.name

    @TypeConverter
    fun textoAEstado(valor: String): EstadoTicket = EstadoTicket.valueOf(valor)

    @TypeConverter
    fun prioridadATexto(valor: Prioridad): String = valor.name

    @TypeConverter
    fun textoAPrioridad(valor: String): Prioridad = Prioridad.valueOf(valor)

    @TypeConverter
    fun propiedadATexto(valor: Propiedad): String = valor.name

    @TypeConverter
    fun textoAPropiedad(valor: String): Propiedad = Propiedad.valueOf(valor)

    @TypeConverter
    fun estadoSolicitudATexto(valor: EstadoSolicitud): String = valor.name

    @TypeConverter
    fun textoAEstadoSolicitud(valor: String): EstadoSolicitud = EstadoSolicitud.valueOf(valor)
}

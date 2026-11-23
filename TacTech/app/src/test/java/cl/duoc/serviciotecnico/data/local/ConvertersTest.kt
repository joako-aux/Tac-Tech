package cl.duoc.serviciotecnico.data.local

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/** Capa de DATOS (Room): los conversores guardan fechas y enum como texto y los recuperan igual. */
class ConvertersTest {

    private val c = Converters()

    @Test
    fun fechaIdaYVuelta() {
        val fecha = LocalDateTime.of(2026, 11, 2, 16, 45, 10)
        assertEquals("2026-11-02T16:45:10", c.fechaATexto(fecha))
        assertEquals(fecha, c.textoAFecha(c.fechaATexto(fecha)))
        assertNull(c.fechaATexto(null))
        assertNull(c.textoAFecha(null))
    }

    @Test
    fun enumsIdaYVuelta() {
        EstadoTicket.entries.forEach { assertEquals(it, c.textoAEstado(c.estadoATexto(it))) }
        Prioridad.entries.forEach { assertEquals(it, c.textoAPrioridad(c.prioridadATexto(it))) }
        Propiedad.entries.forEach { assertEquals(it, c.textoAPropiedad(c.propiedadATexto(it))) }
        EstadoSolicitud.entries.forEach { assertEquals(it, c.textoAEstadoSolicitud(c.estadoSolicitudATexto(it))) }
    }
}

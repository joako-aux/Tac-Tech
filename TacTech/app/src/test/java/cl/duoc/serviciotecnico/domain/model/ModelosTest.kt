package cl.duoc.serviciotecnico.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Capa de DOMINIO: reglas que viven en los modelos (estados finales, roles, direccion GPS). */
class ModelosTest {

    @Test
    fun soloCerradoYCanceladoSonFinales() {
        val finales = EstadoTicket.entries.filter { it.esFinal }
        assertEquals(listOf(EstadoTicket.CERRADO, EstadoTicket.CANCELADO), finales)
        assertEquals(EstadoTicket.entries.size - 2, EstadoTicket.intermedios.size)
    }

    @Test
    fun enProcesoEsTransitoOTrabajo() {
        assertTrue(EstadoTicket.TRANSITO.enProceso)
        assertTrue(EstadoTicket.TRABAJO_EN_PROCESO.enProceso)
        assertFalse(EstadoTicket.ABIERTO.enProceso)
        assertEquals("Trabajo en proceso", EstadoTicket.TRABAJO_EN_PROCESO.etiqueta)
    }

    @Test
    fun clienteSoloCancelaPorConfirmar() {
        assertTrue(EstadoSolicitud.POR_CONFIRMAR.clientePuedeCancelar)
        assertFalse(EstadoSolicitud.CONFIRMADA.clientePuedeCancelar)
        assertFalse(EstadoSolicitud.RECHAZADA.clientePuedeCancelar)
        assertFalse(EstadoSolicitud.CANCELADA.clientePuedeCancelar)
    }

    @Test
    fun usuarioSabeSuRol() {
        val admin = Usuario("a@t.cl", "Admin", Rol.ADMINISTRADOR)
        val tecnico = Usuario("t@t.cl", "Tecnico", Rol.TECNICO)
        val cliente = Usuario("c@t.cl", "Cliente", Rol.CLIENTE, "Clinica")
        assertTrue(admin.esAdministrador && !admin.esTecnico && !admin.esCliente)
        assertTrue(tecnico.esTecnico && !tecnico.esAdministrador)
        assertTrue(cliente.esCliente)
        assertEquals("Clinica", cliente.empresaNombre)
        assertEquals("Administrador", Rol.ADMINISTRADOR.etiqueta)
        assertEquals("Alta", Prioridad.ALTA.etiqueta)
        assertEquals("Arrendado", Propiedad.ARRENDADO.etiqueta)
    }

    @Test
    fun direccionCortaConCalleNumeroYComuna() {
        val d = DireccionGps("Avenida Apoquindo", "1234", "Las Condes", "RM", "texto completo")
        assertEquals("Avenida Apoquindo 1234, Las Condes", d.corta)
    }

    @Test
    fun direccionCortaSinNumero() {
        val d = DireccionGps("Avenida Apoquindo", null, null, null, "texto completo")
        assertEquals("Avenida Apoquindo", d.corta)
    }

    @Test
    fun direccionCortaUsaElTextoCompletoSiNoHayPartes() {
        val d = DireccionGps(null, null, null, null, "Ruta 68, Valparaiso, Chile")
        assertEquals("Ruta 68, Valparaiso, Chile", d.corta)
    }
}

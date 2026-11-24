package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.ui.viewmodel.SolicitudesAdminViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Bandeja de solicitudes del administrador, filtrada por estado. */
@RunWith(RobolectricTestRunner::class)
class SolicitudesAdminScreenTest : PruebaDePantalla() {

    private var abierta: String? = null
    private var volvio = false

    private fun abrir() {
        sesion(ADMIN)
        e.solicitudes.guardar(solicitud(id = "s-1"))
        e.solicitudes.guardar(solicitud(id = "s-2", estado = EstadoSolicitud.RECHAZADA))
        val vm = SolicitudesAdminViewModel(e.solicitudRepository, e.sincronizacion)
        mostrar { SolicitudesAdminScreen(onVolver = { volvio = true }, onAbrirSolicitud = { abierta = it }, vm = vm) }
        esperarTexto("Solicitudes de clientes")
    }

    @Test
    fun abreUnaSolicitudPorConfirmar() {
        abrir()
        esperarTexto("Equipo no enfria")
        existe("Clinica Las Condes", parcial = true)
        tocarIcono("Revisar solicitud")
        assertEquals("s-1", abierta)
    }

    @Test
    fun filtraPorEstadoYMuestraVacio() {
        abrir()
        tocar("Todas (", parcial = true)
        tocar("Rechazada (", parcial = true)
        esperarTexto("Equipo no enfria")
        tocar("Cancelada (", parcial = true)
        esperarTexto("No hay solicitudes en este estado")

        tocarIcono("Volver")
        assertTrue(volvio)
    }
}

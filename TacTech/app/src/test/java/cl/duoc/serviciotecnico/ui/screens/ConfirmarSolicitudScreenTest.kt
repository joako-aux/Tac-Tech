package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.testutil.usuarioDto
import cl.duoc.serviciotecnico.ui.viewmodel.ConfirmarSolicitudViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** El administrador revisa una solicitud: crea el ticket asignando tecnico o la rechaza. */
@RunWith(RobolectricTestRunner::class)
class ConfirmarSolicitudScreenTest : PruebaDePantalla() {

    private var volvio = false

    private fun abrir(id: String = "s-1") {
        sesion(ADMIN)
        val vm = ConfirmarSolicitudViewModel(id, e.solicitudRepository, e.usuarioRepository, e.sincronizacion)
        mostrar { ConfirmarSolicitudScreen(solicitudId = id, onVolver = { volvio = true }, vm = vm) }
    }

    private fun conSolicitud(estado: EstadoSolicitud = EstadoSolicitud.POR_CONFIRMAR) {
        e.solicitudes.guardar(solicitud(estado = estado).copy(motivo = if (estado == EstadoSolicitud.RECHAZADA) "Sin cobertura" else null))
        e.api.solicitudes += solicitud(estado = estado).aDto()
        e.api.usuarios += usuarioDto(id = 2)
        e.api.ticketConfirmado = ticket(solicitudId = "s-1").aDto()
    }

    @Test
    fun creaElTicketAsignandoTecnicoYPrioridad() {
        conSolicitud()
        abrir()
        esperarTexto("Crear ticket y asignar")
        existe("MSAFB-12")

        tocar("Selecciona una opcion")
        tocar("Tecnico Demo · tecnico@tactech.cl")
        tocar("Alta")
        tocar("Crear ticket")

        esperar { e.api.llamadas.any { it.startsWith("confirmarSolicitud") } }
    }

    @Test
    fun ofreceRechazarLaSolicitud() {
        conSolicitud()
        abrir()
        esperarTexto("Rechazar solicitud")
        existe("Prioridad")
    }

    @Test
    fun sinConexionAvisaQueNoSePuedeProcesar() {
        conSolicitud()
        e.red.enLinea.value = false
        abrir()
        esperarTexto("Necesitas conexion para crear o rechazar el ticket.")
    }

    @Test
    fun sinTecnicosActivosLoIndica() {
        e.solicitudes.guardar(solicitud())
        abrir()
        esperarTexto("Crear ticket y asignar")
        tocar("Selecciona una opcion")
        existe("No hay tecnicos activos. Crealos en Usuarios.")
    }

    @Test
    fun siFallaLaCargaDeTecnicosSePuedeReintentar() {
        e.solicitudes.guardar(solicitud())
        e.api.fallaRed = IOException("sin red")
        abrir()
        esperarTexto("Reintentar")
        e.api.fallaRed = null
        tocar("Reintentar")
    }

    @Test
    fun unaSolicitudYaProcesadaSoloSeMuestra() {
        conSolicitud(EstadoSolicitud.RECHAZADA)
        abrir()
        esperarTexto("Sin cobertura")
        noExiste("Crear ticket y asignar")
        tocarIcono("Volver")
    }
}
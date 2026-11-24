package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.TECNICO
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.AdminViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.ClienteViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.ConfirmarSolicitudViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleGuiaViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleTicketViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin
import cl.duoc.serviciotecnico.ui.viewmodel.SolicitudesAdminViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Casos menos frecuentes de las pantallas: datos opcionales vacios, equipos propios del
 * cliente, tickets cancelados y listas sin resultados. Cada pantalla debe mostrarse bien igual.
 */
@RunWith(RobolectricTestRunner::class)
class EstadosAlternativosScreenTest : PruebaDePantalla() {

    @Test
    fun solicitudConfirmadaCuyoTicketAunNoTieneTecnico() {
        sesion(CLIENTE)
        e.solicitudes.guardar(solicitud(id = "s-1", estado = EstadoSolicitud.CONFIRMADA, ticketId = "t-1"))
        e.tickets.guardar(ticket(id = "t-1", solicitudId = "s-1", tecnicoCorreo = ""))
        val vm = ClienteViewModel(e.solicitudRepository, e.ticketRepository, e.sesion, e.sincronizacion)
        mostrar { ClienteHomeScreen(onNuevaSolicitud = {}, onSesionCerrada = {}, vm = vm) }

        esperarTexto("Ticket N° 1")
        noExiste("Tecnico asignado")
        noExiste("Cancelar solicitud")
    }

    @Test
    fun ticketSinTecnicoConEquipoPropioDelCliente() {
        sesion(TECNICO)
        e.equipos.guardar(equipo().copy(propiedad = Propiedad.PROPIO_DEL_CLIENTE))
        e.tickets.guardar(ticket(tecnicoCorreo = ""))
        val vm = DetalleTicketViewModel("t-1", e.ticketRepository, e.guiaRepository, e.equipoRepository, e.sesion)
        mostrar { DetalleTicketScreen(ticketId = "t-1", onVolver = {}, onNuevaGuia = {}, onAbrirGuia = {}, vm = vm) }

        esperarTexto("Propio del cliente")
        noExiste("Tecnico asignado")
        existe("Aun no hay reportes. Para cerrar el ticket necesitas registrar al menos uno.")
    }

    @Test
    fun ticketCanceladoSinSolucionNiFechaDeCierre() {
        sesion(ADMIN)
        e.tickets.guardar(ticket(estado = EstadoTicket.CANCELADO))
        val vm = DetalleTicketViewModel("t-1", e.ticketRepository, e.guiaRepository, e.equipoRepository, e.sesion)
        mostrar { DetalleTicketScreen(ticketId = "t-1", onVolver = {}, onNuevaGuia = {}, onAbrirGuia = {}, vm = vm) }

        esperarTexto("Ticket cancelado")
        noExiste("Solucion")
        noExiste("Fecha de cierre")
    }

    @Test
    fun reporteSinRepuestosNiTicket() {
        sesion(TECNICO)
        e.guias.guardar(guia(latitud = null, longitud = null).copy(repuestos = ""))
        val vm = DetalleGuiaViewModel("g-1", e.guiaRepository, e.ticketRepository, e.equipoRepository, e.geocodificacion)
        mostrar { DetalleGuiaScreen(guiaId = "g-1", onVolver = {}, vm = vm) }

        esperarTexto("Reporte N° 1")
        noExiste("Repuestos")
    }

    @Test
    fun gestionDeTicketsSinNingunTicket() {
        sesion(ADMIN)
        val vm = AdminViewModel(e.ticketRepository, e.solicitudRepository, e.sesion, e.sincronizacion, FiltroAdmin.TODOS, null)
        mostrar {
            GestionTicketsAdminScreen(
                filtroInicial = FiltroAdmin.TODOS, tecnicoInicial = null, onVolver = {}, onAbrirTicket = {}, vm = vm
            )
        }
        esperarTexto("No hay tickets con estos filtros")
        noExiste("Todos los tecnicos")
    }

    @Test
    fun solicitudSinNombreDeClienteNiEmpresa() {
        sesion(ADMIN)
        e.solicitudes.guardar(solicitud().copy(clienteNombre = null, empresaNombre = null))
        val vm = SolicitudesAdminViewModel(e.solicitudRepository, e.sincronizacion)
        mostrar { SolicitudesAdminScreen(onVolver = {}, onAbrirSolicitud = {}, vm = vm) }

        esperarTexto(CLIENTE.correo, parcial = true)
    }

    @Test
    fun revisarUnaSolicitudQueNoExisteMuestraCargando() {
        sesion(ADMIN)
        val vm = ConfirmarSolicitudViewModel("no-existe", e.solicitudRepository, e.usuarioRepository, e.sincronizacion)
        mostrar { ConfirmarSolicitudScreen(solicitudId = "no-existe", onVolver = {}, vm = vm) }

        esperarTexto("Revisar solicitud")
        noExiste("Crear ticket y asignar")
    }
}

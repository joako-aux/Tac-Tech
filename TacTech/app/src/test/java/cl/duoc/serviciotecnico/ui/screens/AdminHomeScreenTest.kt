package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.AdminViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Panel del administrador: contadores por estado, accesos y avance por tecnico. */
@RunWith(RobolectricTestRunner::class)
class AdminHomeScreenTest : PruebaDePantalla() {

    private var filtroElegido: FiltroAdmin? = null
    private var tecnicoElegido: String? = null
    private val accesos = mutableListOf<String>()
    private var salio = false

    private fun abrir() {
        sesion(ADMIN)
        val vm = AdminViewModel(e.ticketRepository, e.solicitudRepository, e.sesion, e.sincronizacion)
        mostrar {
            AdminHomeScreen(
                onGestionarTickets = { filtro, tecnico -> filtroElegido = filtro; tecnicoElegido = tecnico },
                onSolicitudes = { accesos.add("solicitudes") },
                onUsuarios = { accesos.add("usuarios") },
                onEmpresas = { accesos.add("empresas") },
                onSesionCerrada = { salio = true },
                vm = vm
            )
        }
        esperarTexto("Panel administrador")
    }

    private fun conTareas() {
        e.tickets.guardar(ticket(id = "t-1", numero = 1))
        e.tickets.guardar(ticket(id = "t-2", numero = 2, estado = EstadoTicket.TRANSITO))
        e.tickets.guardar(ticket(id = "t-3", numero = 3, estado = EstadoTicket.CERRADO, tecnicoCorreo = "otro@tactech.cl"))
        e.tickets.guardar(ticket(id = "t-4", numero = 4, estado = EstadoTicket.CANCELADO))
        e.solicitudes.guardar(solicitud())
    }

    @Test
    fun muestraContadoresSolicitudesYAvancePorTecnico() {
        conTareas()
        abrir()
        esperarTexto("Revisa y crea el ticket asignando un tecnico")
        existe("Estado de las tareas")
        existe("Avance por tecnico")
        existe("otro@tactech.cl", parcial = true)
    }

    @Test
    fun cadaContadorYAccesoNavega() {
        conTareas()
        abrir()
        esperarTexto("Avance por tecnico")

        tocar("Pendientes")
        assertEquals(FiltroAdmin.PENDIENTES, filtroElegido)
        tocar("En proceso")
        assertEquals(FiltroAdmin.EN_PROCESO, filtroElegido)
        tocar("Terminadas")
        assertEquals(FiltroAdmin.TERMINADOS, filtroElegido)
        tocar("Canceladas")
        assertEquals(FiltroAdmin.CANCELADOS, filtroElegido)

        tocar("Tickets")
        tocar("Solicitudes por confirmar")
        tocar("Usuarios")
        tocar("Empresas")
        assertTrue(accesos.containsAll(listOf("solicitudes", "usuarios", "empresas")))

        tocarIcono("Ver tickets del tecnico")
        assertEquals(FiltroAdmin.TODOS, filtroElegido)
        assertTrue(tecnicoElegido != null)

        tocar("Sincronizar")
    }

    @Test
    fun sinTareasMuestraElEstadoVacio() {
        abrir()
        esperarTexto("Sin tareas registradas")
        existe("No hay solicitudes nuevas")
    }

    @Test
    fun cerrarSesionPideConfirmacion() {
        abrir()
        tocarIcono("Cerrar sesion")
        existe("¿Quieres salir del panel de administrador?")
        tocar("Cancelar")
        assertFalse(salio)

        tocarIcono("Cerrar sesion")
        tocar("Salir")
        esperar { salio }
    }
}

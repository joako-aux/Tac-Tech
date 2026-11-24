package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.viewmodel.AdminViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Gestion de tickets del administrador: filtros por estado, tecnico y busqueda. */
@RunWith(RobolectricTestRunner::class)
class GestionTicketsAdminScreenTest : PruebaDePantalla() {

    private var ticketAbierto: String? = null
    private var volvio = false
    private lateinit var vm: AdminViewModel

    private fun abrir(filtro: FiltroAdmin = FiltroAdmin.TODOS, tecnico: String? = null) {
        sesion(ADMIN)
        e.tickets.guardar(ticket(id = "t-1", numero = 1))
        e.tickets.guardar(ticket(id = "t-2", numero = 2, estado = EstadoTicket.TRANSITO, titulo = "Mantencion preventiva"))
        e.tickets.guardar(ticket(id = "t-3", numero = 3, estado = EstadoTicket.CERRADO, tecnicoCorreo = "otro@tactech.cl"))
        vm = AdminViewModel(e.ticketRepository, e.solicitudRepository, e.sesion, e.sincronizacion, filtro, tecnico)
        mostrar {
            GestionTicketsAdminScreen(
                filtroInicial = filtro,
                tecnicoInicial = tecnico,
                onVolver = { volvio = true },
                onAbrirTicket = { ticketAbierto = it },
                vm = vm
            )
        }
        esperarTexto("Gestion de tickets")
    }

    @Test
    fun filtraPorEstadoYPorTecnico() {
        abrir()
        esperarTexto("Mantencion preventiva")

        tocar("En proceso (", parcial = true)
        assertEquals(FiltroAdmin.EN_PROCESO, vm.estado.value.filtro)
        tocar("Terminados (", parcial = true)
        tocar("Todos (", parcial = true)

        tocar("Todos los tecnicos")
        assertEquals(null, vm.estado.value.tecnico)

        tocar("Equipo no enfria")
        assertEquals("t-1", ticketAbierto)
    }

    @Test
    fun buscaPorTextoYLimpiaLaBusqueda() {
        abrir()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Mantencion")
        compose.waitForIdle()
        assertEquals("Mantencion", vm.estado.value.busqueda)

        tocarIcono("Limpiar busqueda")
        assertEquals("", vm.estado.value.busqueda)

        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("no existe nada")
        esperarTexto("No hay tickets con estos filtros")
    }

    @Test
    fun abreConElTecnicoElegidoYVuelve() {
        abrir(FiltroAdmin.PENDIENTES, "tecnico@tactech.cl")
        tocarIcono("Volver")
        assertTrue(volvio)
        tocar("Sincronizar")
    }
}

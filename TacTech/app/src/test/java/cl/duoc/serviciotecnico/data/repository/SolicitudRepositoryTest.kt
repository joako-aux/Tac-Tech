package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.aDto
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.testutil.ApiServiceFalsa
import cl.duoc.serviciotecnico.testutil.CLIENTE
import cl.duoc.serviciotecnico.testutil.SolicitudDaoEnMemoria
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Capa REPOSITORY: solicitudes del cliente (offline) y su gestion por el administrador (online).
 */
class SolicitudRepositoryTest {

    private val dao = SolicitudDaoEnMemoria()
    private val api = ApiServiceFalsa()
    private val repo = SolicitudRepository(dao, api)

    @Test
    fun crearDejaLaSolicitudPorConfirmarYPendiente() = runTest {
        val creada = repo.crear(
            cliente = CLIENTE,
            titulo = "Equipo no enfria",
            descripcion = "El split no baja de 26 grados",
            direccion = "Av. Apoquindo 1234",
            equipoNombre = "Split muro",
            equipoModelo = "MSAFB-12",
            equipoSerie = "MID-12345",
            propiedad = Propiedad.PROPIO_DEL_CLIENTE
        )

        assertEquals(EstadoSolicitud.POR_CONFIRMAR, creada.estado)
        assertTrue(creada.pendienteSync)
        assertEquals("Clinica Las Condes", creada.empresaNombre)
        assertEquals(listOf(creada), repo.observarDeCliente(CLIENTE.correo).first())
        assertEquals(creada, repo.observarSolicitud(creada.id).first())
        assertEquals(1, repo.observarTodas().first().size)
        assertTrue("Crear no debe llamar al servidor (funciona sin internet)", api.llamadas.isEmpty())
    }

    @Test
    fun cancelarUnaSolicitudPorConfirmar() = runTest {
        dao.guardar(solicitud())

        assertTrue(repo.cancelar("s-1", "Me equivoque de equipo"))

        val s = dao.buscar("s-1")!!
        assertEquals(EstadoSolicitud.CANCELADA, s.estado)
        assertEquals("Me equivoque de equipo", s.motivo)
        assertTrue(s.pendienteSync)
    }

    @Test
    fun noSeCancelaLoQueYaFueProcesado() = runTest {
        dao.guardar(solicitud(estado = EstadoSolicitud.CONFIRMADA))
        assertFalse(repo.cancelar("s-1", "Ya no la necesito"))
        assertFalse(repo.cancelar("no-existe", "Ya no la necesito"))
    }

    @Test
    fun confirmarCreaElTicketEnElServidorYActualizaLaCopiaLocal() = runTest {
        dao.guardar(solicitud(pendienteSync = true))
        api.ticketConfirmado = ticket(id = "t-55", numero = 55).aDto()

        val r = repo.confirmar("s-1", "tecnico@tactech.cl", Prioridad.ALTA)

        assertEquals(Resultado.Exito(55), r)
        assertEquals(listOf("confirmarSolicitud(s-1,tecnico@tactech.cl,ALTA)"), api.llamadas)
        val s = dao.buscar("s-1")!!
        assertEquals(EstadoSolicitud.CONFIRMADA, s.estado)
        assertEquals("t-55", s.ticketId)
        assertFalse(s.pendienteSync)
    }

    @Test
    fun confirmarSinCopiaLocalIgualDevuelveElNumero() = runTest {
        api.ticketConfirmado = ticket(numero = 7).aDto()
        assertEquals(Resultado.Exito(7), repo.confirmar("s-1", "tecnico@tactech.cl", Prioridad.BAJA))
    }

    @Test
    fun confirmarConErrorDelServidorDevuelveSuMensaje() = runTest {
        dao.guardar(solicitud())
        api.ticketConfirmado = null // el servidor responde 404

        val r = repo.confirmar("s-1", "nadie@tactech.cl", Prioridad.MEDIA)

        assertEquals(Resultado.Error("Tecnico no encontrado"), r)
        assertEquals(EstadoSolicitud.POR_CONFIRMAR, dao.buscar("s-1")?.estado)
    }

    @Test
    fun rechazarGuardaLaVersionDelServidor() = runTest {
        api.solicitudes += solicitud().aDto()

        val r = repo.rechazar("s-1", "Equipo fuera de cobertura")

        assertEquals(Resultado.Exito(Unit), r)
        val s = dao.buscar("s-1")!!
        assertEquals(EstadoSolicitud.RECHAZADA, s.estado)
        assertEquals("Equipo fuera de cobertura", s.motivo)
        assertFalse(s.pendienteSync)
    }

    @Test
    fun rechazarSinInternetEsError() = runTest {
        api.fallaRed = IOException("sin red")

        val r = repo.rechazar("s-1", "Equipo fuera de cobertura")

        assertEquals(Resultado.Error("Sin conexion con el servidor. Esta accion necesita internet."), r)
    }
}

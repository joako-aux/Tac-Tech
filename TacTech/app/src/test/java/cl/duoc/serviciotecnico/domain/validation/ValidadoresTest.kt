package cl.duoc.serviciotecnico.domain.validation

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las validaciones viven fuera de la interfaz, asi que se prueban sin emulador.
 * Ejecutar con: ./gradlew test
 */
class ValidadoresTest {

    // ---------- Reglas base ----------

    @Test
    fun correoValidoNoTieneError() {
        assertNull(Validaciones.correo("tecnico@tactech.cl"))
    }

    @Test
    fun correoSinDominioTieneError() {
        assertNotNull(Validaciones.correo("tecnico@"))
        assertNotNull(Validaciones.correo(""))
    }

    @Test
    fun nombreConNumerosTieneError() {
        assertNotNull(Validaciones.nombrePersona("Juan123", "El nombre"))
        assertNull(Validaciones.nombrePersona("María José Pérez", "El nombre"))
    }

    @Test
    fun serieRespetaFormato() {
        assertNull(Validaciones.serie("AC-2024-0001"))
        assertNotNull(Validaciones.serie("abc"))
        assertNotNull(Validaciones.serie("AC 2024"))
    }

    // ---------- Formularios ----------

    @Test
    fun loginConDatosCorrectosEsValido() {
        assertTrue(ValidadorLogin.validar("tecnico@tactech.cl", "tecnico123").isEmpty())
    }

    @Test
    fun loginVacioMarcaAmbosCampos() {
        val errores = ValidadorLogin.validar("", "")
        assertEquals(setOf(CampoLogin.CORREO, CampoLogin.CLAVE), errores.keys)
    }

    @Test
    fun solicitudCompletaEsValida() {
        val datos = DatosSolicitud(
            titulo = "Equipo no enfria",
            descripcion = "El split del segundo piso no baja de 26 grados",
            direccion = "Av. Apoquindo 1234, Las Condes",
            nombreEquipo = "Split muro",
            modelo = "MSAFB-12",
            serie = "MID-12345"
        )
        assertTrue(ValidadorSolicitud.validar(datos).isEmpty())
    }

    @Test
    fun solicitudVaciaMarcaTodosLosCampos() {
        val errores = ValidadorSolicitud.validar(DatosSolicitud("", "", "", "", "", ""))
        assertEquals(CampoSolicitud.entries.toSet(), errores.keys)
    }

    @Test
    fun rutConDigitoCorrectoEsValido() {
        assertNull(Validaciones.rut("76.123.456-0"))
        assertNull(Validaciones.rut("12345678-5"))
        assertNotNull(Validaciones.rut("12345678-9"))
        assertNotNull(Validaciones.rut("abc"))
    }

    @Test
    fun clienteSinEmpresaTieneError() {
        val errores = ValidadorUsuario.validar(
            nombre = "Ana Rojas",
            correo = "ana@clinica.cl",
            clave = "cliente123",
            rol = Rol.CLIENTE,
            empresaId = null,
            esNuevo = true
        )
        assertEquals(setOf(CampoUsuario.EMPRESA), errores.keys)
    }

    @Test
    fun alEditarUsuarioLaClaveVaciaNoEsError() {
        val errores = ValidadorUsuario.validar("Tecnico Demo", "tecnico@tactech.cl", "", Rol.TECNICO, null, esNuevo = false)
        assertTrue(errores.isEmpty())
    }

    @Test
    fun reporteSinFotoNiUbicacionTieneError() {
        val errores = ValidadorGuia.validar(
            DatosGuia(
                direccion = "Av. Apoquindo 1234",
                tecnico = "Carlos Lobos",
                recibidoPor = "Ana Rojas",
                trabajoRealizado = "Cambio de capacitor y limpieza de filtros",
                repuestos = "",
                observaciones = ""
            )
        )
        assertEquals(setOf(CampoGuia.FOTOS, CampoGuia.UBICACION), errores.keys)
    }

    @Test
    fun guiaSinTrabajoRealizadoTieneError() {
        val errores = ValidadorGuia.validar(
            DatosGuia(
                direccion = "Av. Apoquindo 1234",
                tecnico = "Carlos Lobos",
                recibidoPor = "Ana Rojas",
                trabajoRealizado = "",
                repuestos = "",
                observaciones = "",
                cantidadFotos = 1,
                tieneUbicacion = true
            )
        )
        assertEquals(setOf(CampoGuia.TRABAJO), errores.keys)
    }

    @Test
    fun noSePuedeCerrarSinGuias() {
        val errores = ValidadorCambioEstado.validar(
            actual = EstadoTicket.TRABAJO_EN_PROCESO,
            seleccionado = EstadoTicket.CERRADO,
            comentario = "Se cambio el capacitor",
            cantidadDeGuias = 0
        )
        assertTrue(CampoCambioEstado.ESTADO in errores)
    }

    @Test
    fun seCierraConAlMenosUnaGuia() {
        val errores = ValidadorCambioEstado.validar(
            actual = EstadoTicket.TRABAJO_EN_PROCESO,
            seleccionado = EstadoTicket.CERRADO,
            comentario = "Se cambio el capacitor",
            cantidadDeGuias = 1
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun tecnicoNoPuedeElegirCancelado() {
        val disponibles = ValidadorCambioEstado.estadosDisponibles(EstadoTicket.ABIERTO)
        assertFalse(EstadoTicket.CANCELADO in disponibles)
        assertFalse(EstadoTicket.ABIERTO in disponibles)
    }

    @Test
    fun administradorPuedeCancelarConMotivo() {
        val disponibles = ValidadorCambioEstado.estadosDisponibles(EstadoTicket.ABIERTO, esAdministrador = true)
        assertTrue(EstadoTicket.CANCELADO in disponibles)
        val errores = ValidadorCambioEstado.validar(
            actual = EstadoTicket.ABIERTO,
            seleccionado = EstadoTicket.CANCELADO,
            comentario = "Cliente desiste del servicio",
            cantidadDeGuias = 0,
            esAdministrador = true
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun tecnicoNoPuedeCancelarAunqueLoIntente() {
        val errores = ValidadorCambioEstado.validar(
            actual = EstadoTicket.ABIERTO,
            seleccionado = EstadoTicket.CANCELADO,
            comentario = "Cliente desiste del servicio",
            cantidadDeGuias = 0
        )
        assertTrue(CampoCambioEstado.ESTADO in errores)
    }
}

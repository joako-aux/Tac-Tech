package cl.duoc.serviciotecnico.domain.validation

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Capa de DOMINIO: reglas de validacion en Kotlin puro.
 * Complementa ValidadoresTest cubriendo cada rama de cada regla (caso valido y cada error).
 */
class ValidacionesTest {

    // ---------- Reglas base ----------

    @Test
    fun obligatorioDetectaVacioYEspacios() {
        assertEquals("El nombre es obligatorio", Validaciones.obligatorio("   ", "El nombre"))
        assertNull(Validaciones.obligatorio("Ana", "El nombre"))
    }

    @Test
    fun largoMinimoYMaximo() {
        assertEquals("El titulo es obligatorio", Validaciones.largoMinimo("", 5, "El titulo"))
        assertEquals("El titulo debe tener al menos 5 caracteres", Validaciones.largoMinimo("abc", 5, "El titulo"))
        assertNull(Validaciones.largoMinimo("abcde", 5, "El titulo"))
        assertEquals("Repuestos admite hasta 3 caracteres", Validaciones.largoMaximo("abcd", 3, "Repuestos"))
        assertNull(Validaciones.largoMaximo("abc", 3, "Repuestos"))
    }

    @Test
    fun rangoAplicaMinimoAntesQueMaximo() {
        assertNotNull(Validaciones.rango("ab", 3, 5, "X"))
        assertEquals("X admite hasta 5 caracteres", Validaciones.rango("abcdef", 3, 5, "X"))
        assertNull(Validaciones.rango("abcd", 3, 5, "X"))
    }

    @Test
    fun correoValidoEInvalido() {
        assertEquals("El correo es obligatorio", Validaciones.correo(""))
        assertEquals("Ingresa un correo valido (ej: nombre@empresa.cl)", Validaciones.correo("ana@clinica"))
        assertNull(Validaciones.correo("  ana.rojas@clinica.cl  "))
    }

    @Test
    fun nombrePersonaSoloLetras() {
        assertEquals("El nombre es obligatorio", Validaciones.nombrePersona("", "El nombre"))
        assertEquals("El nombre debe tener al menos 3 caracteres", Validaciones.nombrePersona("Al", "El nombre"))
        assertEquals("El nombre solo puede contener letras", Validaciones.nombrePersona("Ana 123", "El nombre"))
        assertNull(Validaciones.nombrePersona("María José O'Higgins", "El nombre"))
    }

    @Test
    fun serieDelEquipo() {
        assertEquals("El numero de serie es obligatorio", Validaciones.serie(""))
        assertEquals("La serie debe tener entre 4 y 20 caracteres", Validaciones.serie("AB1"))
        assertEquals("La serie solo admite letras, numeros y guion", Validaciones.serie("mid_123"))
        assertNull(Validaciones.serie("MID-12345"))
    }

    @Test
    fun rutConFormatosYDigitoK() {
        assertEquals("El RUT es obligatorio", Validaciones.rut(""))
        assertEquals("Formato de RUT: 12345678-5", Validaciones.rut("123"))
        assertNull(Validaciones.rut("10.000.013-K"))
        assertNull(Validaciones.rut("10000013-k"))
        assertNull(Validaciones.rut("1000013-0"))
        assertEquals("El digito verificador del RUT no es correcto", Validaciones.rut("10000013-1"))
    }

    @Test
    fun digitoVerificadorModulo11() {
        assertEquals('5', Validaciones.digitoVerificador("12345678"))
        assertEquals('K', Validaciones.digitoVerificador("10000013"))
        assertEquals('0', Validaciones.digitoVerificador("76123456"))
    }

    @Test
    fun telefonoEsOpcionalPeroConFormato() {
        assertNull(Validaciones.telefonoOpcional(""))
        assertNull(Validaciones.telefonoOpcional("+56 9 1234 5678"))
        assertNull(Validaciones.telefonoOpcional("56912345678"))
        assertEquals("Formato: +56912345678", Validaciones.telefonoOpcional("912345678"))
    }

    // ---------- Validadores por formulario ----------

    @Test
    fun motivoDeCancelacionORechazo() {
        assertEquals("El motivo debe tener al menos 5 caracteres", ValidadorSolicitud.validarMotivo("no"))
        assertNull(ValidadorSolicitud.validarMotivo("Cliente desiste del servicio"))
    }

    @Test
    fun confirmarSolicitudExigeTecnico() {
        assertEquals("Debes asignar un tecnico al ticket", ValidadorConfirmacion.validar(null))
        assertEquals("Debes asignar un tecnico al ticket", ValidadorConfirmacion.validar(" "))
        assertNull(ValidadorConfirmacion.validar("tecnico@tactech.cl"))
    }

    @Test
    fun empresaValidaEInvalida() {
        assertTrue(ValidadorEmpresa.validar("Clinica Las Condes", "76.123.456-0", "Av. Apoquindo 1234", "").isEmpty())
        val errores = ValidadorEmpresa.validar("", "76123456-1", "Av", "123")
        assertEquals(CampoEmpresa.entries.toSet(), errores.keys)
    }

    @Test
    fun usuarioConNombreYCorreoInvalidosYClaveCorta() {
        val errores = ValidadorUsuario.validar("A1", "correo", "123", Rol.TECNICO, null, esNuevo = false)
        assertEquals(setOf(CampoUsuario.NOMBRE, CampoUsuario.CORREO, CampoUsuario.CLAVE), errores.keys)
    }

    @Test
    fun usuarioNuevoExigeClave() {
        val errores = ValidadorUsuario.validar("Ana Rojas", "ana@clinica.cl", "", Rol.ADMINISTRADOR, null, esNuevo = true)
        assertEquals(setOf(CampoUsuario.CLAVE), errores.keys)
    }

    @Test
    fun guiaConDemasiadasFotosYTextosLargos() {
        val errores = ValidadorGuia.validar(
            DatosGuia(
                direccion = "Av. Apoquindo 1234",
                tecnico = "Carlos Lobos",
                recibidoPor = "Ana Rojas",
                trabajoRealizado = "Cambio de capacitor y limpieza de filtros",
                repuestos = "x".repeat(201),
                observaciones = "x".repeat(301),
                cantidadFotos = ValidadorGuia.MAX_FOTOS + 1,
                tieneUbicacion = true
            )
        )
        assertEquals(setOf(CampoGuia.REPUESTOS, CampoGuia.OBSERVACIONES, CampoGuia.FOTOS), errores.keys)
        assertEquals("Maximo 15 fotografias por reporte", errores[CampoGuia.FOTOS])
    }

    @Test
    fun cambioDeEstadoSinSeleccionNiComentario() {
        val errores = ValidadorCambioEstado.validar(EstadoTicket.ABIERTO, null, "", 0)
        assertEquals("Selecciona el nuevo estado", errores[CampoCambioEstado.ESTADO])
        assertEquals("El comentario es obligatorio", errores[CampoCambioEstado.COMENTARIO])
    }

    @Test
    fun cambioAlMismoEstadoNoSePermite() {
        val errores = ValidadorCambioEstado.validar(EstadoTicket.COORDINADO, EstadoTicket.COORDINADO, "Sin cambios", 0)
        assertEquals("El ticket ya esta en ese estado", errores[CampoCambioEstado.ESTADO])
    }

    @Test
    fun etiquetaDelComentarioSegunEstado() {
        val alCerrar = ValidadorCambioEstado.validar(EstadoTicket.ABIERTO, EstadoTicket.CERRADO, "", 1)
        assertEquals("La solucion es obligatorio", alCerrar[CampoCambioEstado.COMENTARIO])
        val alCancelar = ValidadorCambioEstado.validar(EstadoTicket.ABIERTO, EstadoTicket.CANCELADO, "", 0, esAdministrador = true)
        assertEquals("El motivo es obligatorio", alCancelar[CampoCambioEstado.COMENTARIO])
    }

    @Test
    fun estadosDisponiblesParaTecnicoYAdministrador() {
        val tecnico = ValidadorCambioEstado.estadosDisponibles(EstadoTicket.TRANSITO)
        assertEquals(EstadoTicket.intermedios.size - 1 + 1, tecnico.size)
        assertTrue(EstadoTicket.CERRADO in tecnico)
        val admin = ValidadorCambioEstado.estadosDisponibles(EstadoTicket.TRANSITO, esAdministrador = true)
        assertEquals(tecnico + EstadoTicket.CANCELADO, admin)
    }
}

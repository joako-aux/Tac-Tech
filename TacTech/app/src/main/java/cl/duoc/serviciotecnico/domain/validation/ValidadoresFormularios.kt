package cl.duoc.serviciotecnico.domain.validation

import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Rol

/*
 * Un validador por formulario de la aplicacion.
 *
 * Cada uno recibe los valores escritos y devuelve un mapa campo -> mensaje de error.
 * Un mapa vacio significa que el formulario es valido. La interfaz nunca decide
 * si algo es valido: solo pinta lo que este mapa le dice.
 */

// ---------------------------------------------------------------- Login

enum class CampoLogin { CORREO, CLAVE }

object ValidadorLogin {
    fun validar(correo: String, clave: String): Map<CampoLogin, String> = buildMap {
        Validaciones.correo(correo)?.let { put(CampoLogin.CORREO, it) }
        Validaciones.largoMinimo(clave, 6, "La contraseña")?.let { put(CampoLogin.CLAVE, it) }
    }
}

// ---------------------------------------------------------------- Solicitud (cliente)

enum class CampoSolicitud { TITULO, DESCRIPCION, DIRECCION, NOMBRE_EQUIPO, MODELO, SERIE }

data class DatosSolicitud(
    val titulo: String,
    val descripcion: String,
    val direccion: String,
    val nombreEquipo: String,
    val modelo: String,
    val serie: String
)

object ValidadorSolicitud {
    fun validar(d: DatosSolicitud): Map<CampoSolicitud, String> = buildMap {
        Validaciones.rango(d.titulo, 5, 60, "El titulo")?.let { put(CampoSolicitud.TITULO, it) }
        Validaciones.rango(d.descripcion, 10, 500, "La descripcion")?.let { put(CampoSolicitud.DESCRIPCION, it) }
        Validaciones.rango(d.direccion, 5, 120, "La direccion")?.let { put(CampoSolicitud.DIRECCION, it) }
        Validaciones.rango(d.nombreEquipo, 3, 60, "El nombre del equipo")?.let { put(CampoSolicitud.NOMBRE_EQUIPO, it) }
        Validaciones.rango(d.modelo, 2, 40, "El modelo")?.let { put(CampoSolicitud.MODELO, it) }
        Validaciones.serie(d.serie)?.let { put(CampoSolicitud.SERIE, it) }
    }

    /** Motivo de cancelacion (cliente) o de rechazo (administrador). */
    fun validarMotivo(motivo: String): String? = Validaciones.rango(motivo, 5, 300, "El motivo")
}

// ---------------------------------------------------------------- Confirmar solicitud (administrador)

object ValidadorConfirmacion {
    /** Para crear el ticket hay que asignar un tecnico. */
    fun validar(tecnicoCorreo: String?): String? =
        if (tecnicoCorreo.isNullOrBlank()) "Debes asignar un tecnico al ticket" else null
}

// ---------------------------------------------------------------- Usuario (administrador)

enum class CampoUsuario { NOMBRE, CORREO, CLAVE, EMPRESA }

object ValidadorUsuario {
    /**
     * @param esNuevo al crear la clave es obligatoria; al editar, vacia = no cambiarla.
     */
    fun validar(
        nombre: String,
        correo: String,
        clave: String,
        rol: Rol,
        empresaId: Long?,
        esNuevo: Boolean
    ): Map<CampoUsuario, String> = buildMap {
        Validaciones.nombrePersona(nombre, "El nombre")?.let { put(CampoUsuario.NOMBRE, it) }
        Validaciones.correo(correo)?.let { put(CampoUsuario.CORREO, it) }
        if (esNuevo || clave.isNotEmpty()) {
            Validaciones.rango(clave, 6, 60, "La contraseña")?.let { put(CampoUsuario.CLAVE, it) }
        }
        // Regla de negocio: todo cliente pertenece a una empresa
        if (rol == Rol.CLIENTE && empresaId == null) {
            put(CampoUsuario.EMPRESA, "Un cliente debe pertenecer a una empresa")
        }
    }
}

// ---------------------------------------------------------------- Empresa (administrador)

enum class CampoEmpresa { NOMBRE, RUT, DIRECCION, TELEFONO }

object ValidadorEmpresa {
    fun validar(nombre: String, rut: String, direccion: String, telefono: String): Map<CampoEmpresa, String> = buildMap {
        Validaciones.rango(nombre, 2, 80, "El nombre")?.let { put(CampoEmpresa.NOMBRE, it) }
        Validaciones.rut(rut)?.let { put(CampoEmpresa.RUT, it) }
        Validaciones.rango(direccion, 5, 120, "La direccion")?.let { put(CampoEmpresa.DIRECCION, it) }
        Validaciones.telefonoOpcional(telefono)?.let { put(CampoEmpresa.TELEFONO, it) }
    }
}

// ---------------------------------------------------------------- Guia / reporte tecnico

enum class CampoGuia { DIRECCION, TECNICO, RECIBIDO_POR, TRABAJO, REPUESTOS, OBSERVACIONES, FOTOS, UBICACION }

data class DatosGuia(
    val direccion: String,
    val tecnico: String,
    val recibidoPor: String,
    val trabajoRealizado: String,
    val repuestos: String,
    val observaciones: String,
    val cantidadFotos: Int = 0,
    val tieneUbicacion: Boolean = false
)

object ValidadorGuia {
    const val MAX_FOTOS = 15

    fun validar(d: DatosGuia): Map<CampoGuia, String> = buildMap {
        Validaciones.rango(d.direccion, 5, 120, "La direccion")?.let { put(CampoGuia.DIRECCION, it) }
        Validaciones.nombrePersona(d.tecnico, "El nombre del tecnico")?.let { put(CampoGuia.TECNICO, it) }
        Validaciones.nombrePersona(d.recibidoPor, "Quien recibe")?.let { put(CampoGuia.RECIBIDO_POR, it) }
        Validaciones.rango(d.trabajoRealizado, 10, 500, "El trabajo realizado")?.let { put(CampoGuia.TRABAJO, it) }
        // Opcionales: solo se limita el largo
        Validaciones.largoMaximo(d.repuestos, 200, "Repuestos")?.let { put(CampoGuia.REPUESTOS, it) }
        Validaciones.largoMaximo(d.observaciones, 300, "Observaciones")?.let { put(CampoGuia.OBSERVACIONES, it) }
        // Evidencia obligatoria del reporte: al menos una foto y la geolocalizacion
        when {
            d.cantidadFotos == 0 -> put(CampoGuia.FOTOS, "Debes adjuntar al menos una fotografia de evidencia")
            d.cantidadFotos > MAX_FOTOS -> put(CampoGuia.FOTOS, "Maximo $MAX_FOTOS fotografias por reporte")
        }
        if (!d.tieneUbicacion) put(CampoGuia.UBICACION, "Debes registrar la ubicacion de la visita")
    }
}

// ---------------------------------------------------------------- Cambio de estado

enum class CampoCambioEstado { ESTADO, COMENTARIO }

object ValidadorCambioEstado {

    /**
     * Estados que se pueden elegir desde el estado actual.
     * El tecnico ve los intermedios y Cerrado. El administrador ademas puede Cancelar.
     */
    fun estadosDisponibles(actual: EstadoTicket, esAdministrador: Boolean = false): List<EstadoTicket> {
        val finales = if (esAdministrador) listOf(EstadoTicket.CERRADO, EstadoTicket.CANCELADO)
        else listOf(EstadoTicket.CERRADO)
        return (EstadoTicket.intermedios + finales).filter { it != actual }
    }

    fun validar(
        actual: EstadoTicket,
        seleccionado: EstadoTicket?,
        comentario: String,
        cantidadDeGuias: Int,
        esAdministrador: Boolean = false
    ): Map<CampoCambioEstado, String> = buildMap {
        when {
            seleccionado == null -> put(CampoCambioEstado.ESTADO, "Selecciona el nuevo estado")
            seleccionado == actual -> put(CampoCambioEstado.ESTADO, "El ticket ya esta en ese estado")
            seleccionado == EstadoTicket.CANCELADO && !esAdministrador ->
                put(CampoCambioEstado.ESTADO, "Solo el administrador puede cancelar un ticket")
            // Regla de negocio: no se cierra un ticket sin al menos una guia emitida
            seleccionado == EstadoTicket.CERRADO && cantidadDeGuias == 0 ->
                put(CampoCambioEstado.ESTADO, "Para cerrar el ticket debes emitir al menos una guia")
        }
        val nombre = when (seleccionado) {
            EstadoTicket.CERRADO -> "La solucion"
            EstadoTicket.CANCELADO -> "El motivo"
            else -> "El comentario"
        }
        Validaciones.rango(comentario, 5, 300, nombre)?.let { put(CampoCambioEstado.COMENTARIO, it) }
    }
}

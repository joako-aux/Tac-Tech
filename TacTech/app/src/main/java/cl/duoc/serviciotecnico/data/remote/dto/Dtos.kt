package cl.duoc.serviciotecnico.data.remote.dto

/*
 * Objetos que viajan por HTTP entre la app y tactech-backend (JSON con Gson).
 * Son distintos de las entidades de Room: la red no necesita saber de pendienteSync
 * y las fechas viajan como texto ISO-8601 (ej: 2026-11-02T16:45:00).
 * Los enum viajan por su nombre (ABIERTO, ALTA, ARRENDADO, TECNICO...).
 */

data class LoginRequest(
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Long? = null,
    val nombre: String,
    val correo: String,
    val rol: String,
    val activo: Boolean = true,
    val empresaId: Long? = null,
    val empresaNombre: String? = null
)

/** Crear o editar un usuario (administrador). clave vacia al editar = no cambiarla. */
data class UsuarioRequest(
    val nombre: String,
    val correo: String,
    val clave: String?,
    val rol: String,
    val activo: Boolean,
    val empresaId: Long?
)

data class EmpresaDto(
    val id: Long? = null,
    val nombre: String,
    val rut: String,
    val direccion: String,
    val telefono: String?
)

data class SolicitudDto(
    val id: String,
    val clienteCorreo: String,
    val clienteNombre: String?,
    val empresaNombre: String?,
    val titulo: String,
    val descripcion: String,
    val direccion: String,
    val equipoNombre: String,
    val equipoModelo: String,
    val equipoSerie: String,
    val propiedad: String,
    val estado: String,
    val motivo: String?,
    val ticketId: String?,
    val fechaCreacion: String,
    val fechaActualizacion: String
)

data class ConfirmarSolicitudRequest(
    val tecnicoCorreo: String,
    val prioridad: String
)

data class MotivoRequest(val motivo: String)

/** Cuerpo de error que devuelve tactech-backend (GlobalExceptionHandler). */
data class ErrorDto(
    val estado: Int?,
    val mensaje: String?,
    val errores: Map<String, String>?
)

data class EquipoDto(
    val id: String,
    val nombre: String,
    val modelo: String,
    val serie: String,
    val propiedad: String
)

data class TicketDto(
    val id: String,
    val numero: Int,
    val titulo: String,
    val descripcion: String,
    val cliente: String,
    val direccion: String,
    val prioridad: String,
    val estado: String,
    val equipoId: String,
    val tecnicoCorreo: String?,
    val fechaCreacion: String,
    val fechaActualizacion: String,
    val comentarioEstado: String?,
    val fechaCierre: String?,
    val solucion: String?,
    val solicitudId: String? = null
)

data class CambioEstadoRequest(
    val estado: String,
    val comentario: String
)

data class FotoGuiaDto(
    val id: String,
    val uri: String,
    val fechaCaptura: String
)

data class GuiaDto(
    val id: String,
    val numero: Int,
    val ticketId: String,
    val equipoId: String,
    val direccion: String,
    val latitud: Double?,
    val longitud: Double?,
    val fecha: String,
    val tecnico: String,
    val trabajoRealizado: String,
    val repuestos: String,
    val observaciones: String,
    val recibidoPor: String,
    val fotos: List<FotoGuiaDto> = emptyList()
)

data class ResumenDto(
    val total: Long,
    val pendientes: Long,
    val terminados: Long,
    val porEstado: Map<String, Long> = emptyMap()
)

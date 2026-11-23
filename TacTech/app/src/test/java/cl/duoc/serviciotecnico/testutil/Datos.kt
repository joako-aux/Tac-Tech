package cl.duoc.serviciotecnico.testutil

import cl.duoc.serviciotecnico.data.local.entity.Equipo
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.model.Usuario
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDateTime

/*
 * Datos de prueba reutilizables. Cada funcion crea un objeto valido con valores por defecto;
 * el test solo cambia lo que le importa (ej: ticket(estado = EstadoTicket.CERRADO)).
 */

val FECHA_BASE: LocalDateTime = LocalDateTime.of(2025, 11, 2, 10, 30)

val TECNICO = Usuario("tecnico@tactech.cl", "Tecnico Demo", Rol.TECNICO)
val ADMIN = Usuario("admin@tactech.cl", "Administrador", Rol.ADMINISTRADOR)
val CLIENTE = Usuario("cliente@tactech.cl", "Cliente Demo", Rol.CLIENTE, "Clinica Las Condes")

fun equipo(
    id: String = "eq-1",
    serie: String = "MID-12345",
    pendienteSync: Boolean = false
) = Equipo(id = id, nombre = "Split muro", modelo = "MSAFB-12", serie = serie, propiedad = Propiedad.ARRENDADO, pendienteSync = pendienteSync)

fun ticket(
    id: String = "t-1",
    numero: Int = 1,
    estado: EstadoTicket = EstadoTicket.ABIERTO,
    tecnicoCorreo: String = TECNICO.correo,
    titulo: String = "Equipo no enfria",
    cliente: String = "Clinica Las Condes",
    equipoId: String = "eq-1",
    solicitudId: String? = null,
    fecha: LocalDateTime = FECHA_BASE,
    pendienteSync: Boolean = false
) = Ticket(
    id = id,
    numero = numero,
    titulo = titulo,
    descripcion = "El split del segundo piso no baja de 26 grados",
    cliente = cliente,
    direccion = "Av. Apoquindo 1234, Las Condes",
    prioridad = Prioridad.MEDIA,
    estado = estado,
    equipoId = equipoId,
    fechaCreacion = fecha,
    fechaActualizacion = fecha,
    tecnicoCorreo = tecnicoCorreo,
    solicitudId = solicitudId,
    pendienteSync = pendienteSync
)

fun guia(
    id: String = "g-1",
    numero: Int = 1,
    ticketId: String = "t-1",
    latitud: Double? = -33.4172,
    longitud: Double? = -70.6044,
    pendienteSync: Boolean = false
) = Guia(
    id = id,
    numero = numero,
    ticketId = ticketId,
    equipoId = "eq-1",
    direccion = "Av. Apoquindo 1234",
    latitud = latitud,
    longitud = longitud,
    fecha = FECHA_BASE,
    tecnico = "Carlos Lobos",
    trabajoRealizado = "Cambio de capacitor y limpieza de filtros",
    repuestos = "Capacitor 35uF",
    observaciones = "",
    recibidoPor = "Ana Rojas",
    pendienteSync = pendienteSync
)

fun solicitud(
    id: String = "s-1",
    clienteCorreo: String = CLIENTE.correo,
    estado: EstadoSolicitud = EstadoSolicitud.POR_CONFIRMAR,
    ticketId: String? = null,
    fecha: LocalDateTime = FECHA_BASE,
    pendienteSync: Boolean = false
) = Solicitud(
    id = id,
    clienteCorreo = clienteCorreo,
    clienteNombre = CLIENTE.nombre,
    empresaNombre = CLIENTE.empresaNombre,
    titulo = "Equipo no enfria",
    descripcion = "El split del segundo piso no baja de 26 grados",
    direccion = "Av. Apoquindo 1234, Las Condes",
    equipoNombre = "Split muro",
    equipoModelo = "MSAFB-12",
    equipoSerie = "MID-12345",
    propiedad = Propiedad.ARRENDADO,
    estado = estado,
    ticketId = ticketId,
    fechaCreacion = fecha,
    fechaActualizacion = fecha,
    pendienteSync = pendienteSync
)

fun usuarioDto(
    id: Long = 1,
    nombre: String = "Tecnico Demo",
    correo: String = "tecnico@tactech.cl",
    rol: Rol = Rol.TECNICO,
    activo: Boolean = true,
    empresaId: Long? = null
) = UsuarioDto(id = id, nombre = nombre, correo = correo, rol = rol.name, activo = activo, empresaId = empresaId)

fun empresaDto(id: Long? = 1, nombre: String = "Clinica Las Condes") =
    EmpresaDto(id = id, nombre = nombre, rut = "76123456-0", direccion = "Av. Apoquindo 1234", telefono = "+56912345678")

/** Error HTTP como lo lanza Retrofit, con el cuerpo JSON que manda tactech-backend. */
fun errorHttp(codigo: Int, json: String = "{}"): HttpException =
    HttpException(Response.error<Any>(codigo, json.toResponseBody("application/json".toMediaType())))

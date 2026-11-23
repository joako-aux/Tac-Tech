package cl.duoc.serviciotecnico.data.remote.dto

import cl.duoc.serviciotecnico.data.local.entity.Equipo
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.model.Usuario
import java.time.LocalDateTime

// Conversiones entre entidades de Room (local) y DTO (red).
// Lo que llega del backend se guarda con pendienteSync = false: ya esta en el servidor.

fun Equipo.aDto() = EquipoDto(id, nombre, modelo, serie, propiedad.name)

fun EquipoDto.aEntidad() = Equipo(
    id = id,
    nombre = nombre,
    modelo = modelo,
    serie = serie,
    propiedad = Propiedad.valueOf(propiedad),
    pendienteSync = false
)

fun Ticket.aDto() = TicketDto(
    id = id,
    numero = numero,
    titulo = titulo,
    descripcion = descripcion,
    cliente = cliente,
    direccion = direccion,
    prioridad = prioridad.name,
    estado = estado.name,
    equipoId = equipoId,
    tecnicoCorreo = tecnicoCorreo.ifBlank { null },
    fechaCreacion = fechaCreacion.toString(),
    fechaActualizacion = fechaActualizacion.toString(),
    comentarioEstado = comentarioEstado,
    fechaCierre = fechaCierre?.toString(),
    solucion = solucion,
    solicitudId = solicitudId
)

fun TicketDto.aEntidad() = Ticket(
    id = id,
    numero = numero,
    titulo = titulo,
    descripcion = descripcion,
    cliente = cliente,
    direccion = direccion,
    prioridad = Prioridad.valueOf(prioridad),
    estado = EstadoTicket.valueOf(estado),
    equipoId = equipoId,
    fechaCreacion = LocalDateTime.parse(fechaCreacion),
    fechaActualizacion = LocalDateTime.parse(fechaActualizacion),
    comentarioEstado = comentarioEstado,
    fechaCierre = fechaCierre?.let(LocalDateTime::parse),
    solucion = solucion,
    tecnicoCorreo = tecnicoCorreo.orEmpty(),
    solicitudId = solicitudId,
    pendienteSync = false
)

fun Guia.aDto(fotos: List<FotoGuia>) = GuiaDto(
    id = id,
    numero = numero,
    ticketId = ticketId,
    equipoId = equipoId,
    direccion = direccion,
    latitud = latitud,
    longitud = longitud,
    fecha = fecha.toString(),
    tecnico = tecnico,
    trabajoRealizado = trabajoRealizado,
    repuestos = repuestos,
    observaciones = observaciones,
    recibidoPor = recibidoPor,
    fotos = fotos.map { FotoGuiaDto(it.id, it.uri, it.fechaCaptura.toString()) }
)

fun GuiaDto.aEntidad() = Guia(
    id = id,
    numero = numero,
    ticketId = ticketId,
    equipoId = equipoId,
    direccion = direccion,
    latitud = latitud,
    longitud = longitud,
    fecha = LocalDateTime.parse(fecha),
    tecnico = tecnico,
    trabajoRealizado = trabajoRealizado,
    repuestos = repuestos,
    observaciones = observaciones,
    recibidoPor = recibidoPor,
    pendienteSync = false
)

fun GuiaDto.fotosAEntidad(): List<FotoGuia> = fotos.map {
    FotoGuia(id = it.id, guiaId = id, uri = it.uri, fechaCaptura = LocalDateTime.parse(it.fechaCaptura))
}

fun UsuarioDto.aUsuario() = Usuario(
    correo = correo,
    nombre = nombre,
    rol = Rol.valueOf(rol),
    empresaNombre = empresaNombre
)

fun Solicitud.aDto() = SolicitudDto(
    id = id,
    clienteCorreo = clienteCorreo,
    clienteNombre = clienteNombre,
    empresaNombre = empresaNombre,
    titulo = titulo,
    descripcion = descripcion,
    direccion = direccion,
    equipoNombre = equipoNombre,
    equipoModelo = equipoModelo,
    equipoSerie = equipoSerie,
    propiedad = propiedad.name,
    estado = estado.name,
    motivo = motivo,
    ticketId = ticketId,
    fechaCreacion = fechaCreacion.toString(),
    fechaActualizacion = fechaActualizacion.toString()
)

fun SolicitudDto.aEntidad() = Solicitud(
    id = id,
    clienteCorreo = clienteCorreo,
    clienteNombre = clienteNombre,
    empresaNombre = empresaNombre,
    titulo = titulo,
    descripcion = descripcion,
    direccion = direccion,
    equipoNombre = equipoNombre,
    equipoModelo = equipoModelo,
    equipoSerie = equipoSerie,
    propiedad = Propiedad.valueOf(propiedad),
    estado = EstadoSolicitud.valueOf(estado),
    motivo = motivo,
    ticketId = ticketId,
    fechaCreacion = LocalDateTime.parse(fechaCreacion),
    fechaActualizacion = LocalDateTime.parse(fechaActualizacion),
    pendienteSync = false
)

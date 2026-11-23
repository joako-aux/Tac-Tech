package cl.duoc.serviciotecnico.ui.theme

import androidx.compose.ui.graphics.Color
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad

// El dominio no conoce colores: la correspondencia vive aqui, en la capa visual.

fun colorDeEstado(estado: EstadoTicket): Color = when (estado) {
    EstadoTicket.ABIERTO -> EstadoAbierto
    EstadoTicket.COORDINACION_EN_PROCESO -> EstadoCoordinando
    EstadoTicket.COORDINADO -> EstadoCoordinado
    EstadoTicket.TRANSITO -> EstadoTransito
    EstadoTicket.TRABAJO_EN_PROCESO -> EstadoTrabajando
    EstadoTicket.PENDIENTE_POR_CLIENTE -> EstadoPendienteCliente
    EstadoTicket.PENDIENTE_POR_TACTECH -> EstadoPendienteEmpresa
    EstadoTicket.CERRADO -> EstadoCerrado
    EstadoTicket.CANCELADO -> EstadoCancelado
}

fun colorDePrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.BAJA -> Exito
    Prioridad.MEDIA -> EstadoPendienteCliente
    Prioridad.ALTA -> RojoError
}

fun colorDeSolicitud(estado: EstadoSolicitud): Color = when (estado) {
    EstadoSolicitud.POR_CONFIRMAR -> EstadoPendienteCliente
    EstadoSolicitud.CONFIRMADA -> EstadoTrabajando
    EstadoSolicitud.RECHAZADA -> RojoError
    EstadoSolicitud.CANCELADA -> EstadoCerrado
}

package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.validation.CampoCambioEstado
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.components.EtiquetaEstado
import cl.duoc.serviciotecnico.ui.components.EtiquetaPrioridad
import cl.duoc.serviciotecnico.ui.components.FilaDato
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.theme.colorDeEstado
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleTicketUiState
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleTicketViewModel
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

@Composable
fun DetalleTicketScreen(
    ticketId: String,
    onVolver: () -> Unit,
    onNuevaGuia: () -> Unit,
    onAbrirGuia: (String) -> Unit,
    vm: DetalleTicketViewModel = viewModel(factory = DetalleTicketViewModel.factory(ticketId))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    val ticket = estado.ticket

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = ticket?.let { "Ticket N° ${it.numero}" } ?: "Ticket",
                subtitulo = ticket?.cliente,
                onVolver = onVolver
            )
        },
        floatingActionButton = {
            if (estado.puedeEmitirReporte) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaGuia,
                    icon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
                    text = { Text("Reporte de visita") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ticket == null -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                Text("No se encontro el ticket")
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "cabecera") { Cabecera(ticket) }
                item(key = "equipo") { SeccionEquipo(estado) }
                item(key = "estado") {
                    if (estado.editable) {
                        FormularioCambioEstado(
                            estado = estado,
                            onSeleccionar = vm::seleccionarEstado,
                            onComentario = vm::cambiarComentario,
                            onEnviar = vm::enviarCambioDeEstado
                        )
                    } else {
                        SeccionFinalizado(ticket)
                    }
                }
                item(key = "tituloGuias") {
                    Text(
                        "Reportes de visita (${estado.guias.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (estado.guias.isEmpty()) {
                    item(key = "sinGuias") {
                        Text(
                            if (estado.esAdministrador) "El tecnico aun no registra reportes para este ticket."
                            else "Aun no hay reportes. Para cerrar el ticket necesitas registrar al menos uno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(estado.guias, key = { it.id }) { guia ->
                        TarjetaGuia(guia, onClick = { onAbrirGuia(guia.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun Cabecera(ticket: Ticket) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EtiquetaEstado(ticket.estado)
                EtiquetaPrioridad(ticket.prioridad)
            }
            Text(ticket.titulo, style = MaterialTheme.typography.titleLarge)
            Text(
                ticket.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilaDato(Icons.Outlined.Business, "Cliente", ticket.cliente)
            if (ticket.tecnicoCorreo.isNotBlank()) {
                FilaDato(Icons.Outlined.Person, "Tecnico asignado", ticket.tecnicoCorreo)
            }
            FilaDato(Icons.Outlined.Place, "Direccion", ticket.direccion)
            FilaDato(Icons.Outlined.Event, "Creado", ticket.fechaCreacion.format(formatoFecha))
            FilaDato(Icons.Outlined.Schedule, "Ultima actualizacion", ticket.fechaActualizacion.format(formatoFecha))
            ticket.comentarioEstado?.let {
                FilaDato(Icons.Outlined.ChatBubbleOutline, "Ultimo comentario", it)
            }
        }
    }
}

@Composable
private fun SeccionEquipo(estado: DetalleTicketUiState) {
    val equipo = estado.equipo ?: return
    TarjetaSeccion(titulo = "Equipo", icono = Icons.Filled.AcUnit, descripcion = equipo.nombre) {
        FilaDato(Icons.Outlined.Category, "Modelo", equipo.modelo)
        FilaDato(Icons.Outlined.QrCode2, "N° de serie", equipo.serie)
        FilaDato(
            if (equipo.propiedad == Propiedad.ARRENDADO) Icons.Outlined.Description else Icons.Outlined.Home,
            "Propiedad",
            equipo.propiedad.etiqueta
        )
    }
}

@Composable
private fun FormularioCambioEstado(
    estado: DetalleTicketUiState,
    onSeleccionar: (EstadoTicket) -> Unit,
    onComentario: (String) -> Unit,
    onEnviar: () -> Unit
) {
    val cambio = estado.cambio
    val errorEstado = estado.formulario.error(CampoCambioEstado.ESTADO)

    TarjetaSeccion(
        titulo = if (estado.esAdministrador) "Gestionar estado (administrador)" else "Cambiar estado",
        icono = Icons.Outlined.Sync,
        descripcion = "Estado actual: ${estado.ticket?.estado?.etiqueta.orEmpty()}"
    ) {
        SelectorDeEstado(
            opciones = estado.estadosDisponibles,
            seleccionado = cambio.seleccionado,
            conError = errorEstado != null,
            habilitado = !cambio.enviando,
            onSeleccionar = onSeleccionar
        )
        AnimatedVisibility(
            visible = errorEstado != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    errorEstado.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        CampoTextoValidado(
            valor = cambio.comentario,
            onValorCambia = onComentario,
            etiqueta = estado.etiquetaComentario,
            icono = Icons.Outlined.ChatBubbleOutline,
            obligatorio = true,
            lineasMaximas = 3,
            largoMaximo = 300,
            error = estado.formulario.error(CampoCambioEstado.COMENTARIO),
            valido = estado.formulario.correcto(CampoCambioEstado.COMENTARIO),
            habilitado = !cambio.enviando
        )
        BotonGuardar(
            onClick = onEnviar,
            texto = "Actualizar estado",
            icono = Icons.Outlined.Sync,
            guardando = cambio.enviando
        )
    }
}

@Composable
private fun SelectorDeEstado(
    opciones: List<EstadoTicket>,
    seleccionado: EstadoTicket?,
    conError: Boolean,
    habilitado: Boolean,
    onSeleccionar: (EstadoTicket) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val colorBorde = if (conError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Box {
        Surface(
            onClick = { abierto = true },
            enabled = habilitado,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, colorBorde),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (seleccionado != null) {
                    PuntoDeEstado(seleccionado)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    seleccionado?.etiqueta ?: "Selecciona el nuevo estado *",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (seleccionado == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Abrir lista de estados")
            }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    leadingIcon = { PuntoDeEstado(opcion) },
                    onClick = {
                        abierto = false
                        onSeleccionar(opcion)
                    }
                )
            }
        }
    }
}

@Composable
private fun PuntoDeEstado(estado: EstadoTicket) {
    Box(Modifier.size(10.dp).background(colorDeEstado(estado), CircleShape))
}

@Composable
private fun SeccionFinalizado(ticket: Ticket) {
    TarjetaSeccion(
        titulo = "Ticket ${ticket.estado.etiqueta.lowercase()}",
        icono = Icons.Outlined.TaskAlt,
        descripcion = "Solo lectura: ya no admite cambios"
    ) {
        ticket.solucion?.let { FilaDato(Icons.Outlined.TaskAlt, "Solucion", it) }
        ticket.fechaCierre?.let { FilaDato(Icons.Outlined.Event, "Fecha de cierre", it.format(formatoFecha)) }
    }
}

@Composable
private fun TarjetaGuia(guia: Guia, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Reporte N° ${guia.numero}", style = MaterialTheme.typography.titleSmall)
                Text(
                    guia.trabajoRealizado,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${guia.tecnico} · ${guia.fecha.format(formatoFecha)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Ver reporte")
        }
    }
}

package cl.duoc.serviciotecnico.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.FilaDato
import cl.duoc.serviciotecnico.ui.components.GaleriaDeFotos
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.components.DireccionGpsInfo
import cl.duoc.serviciotecnico.ui.viewmodel.DetalleGuiaViewModel
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

@Composable
fun DetalleGuiaScreen(
    guiaId: String,
    onVolver: () -> Unit,
    vm: DetalleGuiaViewModel = viewModel(factory = DetalleGuiaViewModel.factory(guiaId))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val guia = estado.guia

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = guia?.let { "Reporte N° ${it.numero}" } ?: "Reporte de visita",
                subtitulo = estado.ticket?.let { "Ticket N° ${it.numero} · ${it.cliente}" },
                onVolver = onVolver
            )
        }
    ) { relleno ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            guia == null -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                Text("No se encontro la guia")
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(relleno)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TarjetaSeccion(titulo = "Datos de la visita", icono = Icons.Outlined.Receipt) {
                    FilaDato(Icons.Outlined.Event, "Fecha de emision", guia.fecha.format(formatoFecha))
                    estado.ticket?.let { FilaDato(Icons.Outlined.ConfirmationNumber, "Ticket", "N° ${it.numero} · ${it.titulo}") }
                    FilaDato(Icons.Outlined.Place, "Direccion", guia.direccion)
                    FilaDato(Icons.Outlined.Engineering, "Tecnico", guia.tecnico)
                    FilaDato(Icons.Outlined.Badge, "Recibido por", guia.recibidoPor)
                }

                estado.equipo?.let { equipo ->
                    TarjetaSeccion(titulo = "Equipo atendido", icono = Icons.Filled.AcUnit, descripcion = equipo.nombre) {
                        FilaDato(Icons.Outlined.QrCode2, "Modelo y serie", "${equipo.modelo} · ${equipo.serie}")
                    }
                }

                TarjetaSeccion(titulo = "Trabajo realizado", icono = Icons.Outlined.Build) {
                    Text(guia.trabajoRealizado, style = MaterialTheme.typography.bodyMedium)
                    if (guia.repuestos.isNotBlank()) {
                        FilaDato(Icons.Outlined.Inventory2, "Repuestos", guia.repuestos)
                    }
                    if (guia.observaciones.isNotBlank()) {
                        FilaDato(Icons.Outlined.ChatBubbleOutline, "Observaciones", guia.observaciones)
                    }
                }

                TarjetaSeccion(
                    titulo = "Evidencia fotografica",
                    icono = Icons.Outlined.PhotoLibrary,
                    descripcion = if (estado.fotos.isEmpty()) "Sin fotografias" else "${estado.fotos.size} fotografias"
                ) {
                    // Sin onQuitarFoto: galeria de solo lectura
                    GaleriaDeFotos(fotos = estado.fotos.map(Uri::parse))
                }

                TarjetaSeccion(titulo = "Ubicacion registrada", icono = Icons.Outlined.LocationOn) {
                    val lat = guia.latitud
                    val lon = guia.longitud
                    Text(
                        if (lat != null && lon != null) "Lat %.5f · Lon %.5f".format(lat, lon)
                        else "La guia se emitio sin coordenadas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // API externa: direccion real del punto donde se registro la visita
                    DireccionGpsInfo(gps = estado.gps)
                }
            }
        }
    }
}

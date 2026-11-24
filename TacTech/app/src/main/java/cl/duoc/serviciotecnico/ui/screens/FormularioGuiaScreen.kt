package cl.duoc.serviciotecnico.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.domain.validation.CampoGuia
import cl.duoc.serviciotecnico.domain.validation.ValidadorGuia
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonDeFoto
import cl.duoc.serviciotecnico.ui.components.BotonDeUbicacion
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.components.DireccionGpsInfo
import cl.duoc.serviciotecnico.ui.components.GaleriaDeFotos
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.theme.Exito
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioGuiaViewModel
import cl.duoc.serviciotecnico.util.ArchivosDeFoto

@Composable
fun FormularioGuiaScreen(
    ticketId: String,
    onVolver: () -> Unit,
    vm: FormularioGuiaViewModel = viewModel(factory = FormularioGuiaViewModel.factory(ticketId))
) {
    val context = LocalContext.current
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onVolver()
    }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    val ticket = estado.ticket
    val habilitado = !estado.guardando

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Reporte de visita",
                subtitulo = ticket?.let { "Ticket N° ${it.numero} · ${it.cliente}" },
                onVolver = onVolver
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        if (ticket == null) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaSeccion(titulo = "Datos de la visita", icono = Icons.Outlined.Receipt) {
                CampoTextoValidado(
                    valor = estado.direccion,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.DIRECCION, it) },
                    etiqueta = "Direccion del servicio",
                    icono = Icons.Outlined.Place,
                    obligatorio = true,
                    ayuda = "Se copia desde el ticket; puedes corregirla o usar la del GPS",
                    error = estado.error(CampoGuia.DIRECCION),
                    valido = estado.valido(CampoGuia.DIRECCION),
                    habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.tecnico,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.TECNICO, it) },
                    etiqueta = "Nombre del tecnico",
                    icono = Icons.Outlined.Engineering,
                    obligatorio = true,
                    capitalizacion = KeyboardCapitalization.Words,
                    error = estado.error(CampoGuia.TECNICO),
                    valido = estado.valido(CampoGuia.TECNICO),
                    habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.recibidoPor,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.RECIBIDO_POR, it) },
                    etiqueta = "Recibido por",
                    icono = Icons.Outlined.Badge,
                    obligatorio = true,
                    ayuda = "Persona del cliente que recibe el trabajo",
                    capitalizacion = KeyboardCapitalization.Words,
                    error = estado.error(CampoGuia.RECIBIDO_POR),
                    valido = estado.valido(CampoGuia.RECIBIDO_POR),
                    habilitado = habilitado
                )
            }

            TarjetaSeccion(titulo = "Trabajo realizado", icono = Icons.Outlined.Build) {
                CampoTextoValidado(
                    valor = estado.trabajoRealizado,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.TRABAJO, it) },
                    etiqueta = "Detalle del trabajo",
                    icono = Icons.Outlined.Build,
                    obligatorio = true,
                    lineasMaximas = 5,
                    largoMaximo = 500,
                    error = estado.error(CampoGuia.TRABAJO),
                    valido = estado.valido(CampoGuia.TRABAJO),
                    habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.repuestos,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.REPUESTOS, it) },
                    etiqueta = "Repuestos utilizados",
                    icono = Icons.Outlined.Inventory2,
                    ayuda = "Opcional",
                    largoMaximo = 200,
                    error = estado.error(CampoGuia.REPUESTOS),
                    valido = estado.valido(CampoGuia.REPUESTOS),
                    habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.observaciones,
                    onValorCambia = { vm.cambiarCampo(CampoGuia.OBSERVACIONES, it) },
                    etiqueta = "Observaciones",
                    icono = Icons.Outlined.ChatBubbleOutline,
                    ayuda = "Opcional",
                    lineasMaximas = 3,
                    largoMaximo = 300,
                    accionDelTeclado = ImeAction.Done,
                    error = estado.error(CampoGuia.OBSERVACIONES),
                    valido = estado.valido(CampoGuia.OBSERVACIONES),
                    habilitado = habilitado
                )
            }

            // ---------- Recurso nativo 1: camara (evidencia obligatoria) ----------
            TarjetaSeccion(
                titulo = "Evidencia fotografica *",
                icono = Icons.Outlined.PhotoLibrary,
                descripcion = "${estado.fotos.size} de ${ValidadorGuia.MAX_FOTOS} fotografias · minimo 1"
            ) {
                BotonDeFoto(
                    onFotoTomada = { uri -> vm.agregarFoto(uri.toString()) },
                    habilitado = habilitado && estado.puedeAgregarFotos,
                    modifier = Modifier.fillMaxWidth()
                )
                GaleriaDeFotos(
                    fotos = estado.fotos.map(Uri::parse),
                    onQuitarFoto = { uri ->
                        vm.quitarFoto(uri.toString())
                        ArchivosDeFoto.eliminar(context, uri)
                    }
                )
                MensajeEvidencia(estado.error(CampoGuia.FOTOS))
            }

            // ---------- Recurso nativo 2: GPS ----------
            TarjetaSeccion(
                titulo = "Ubicacion de la visita *",
                icono = Icons.Outlined.LocationOn,
                descripcion = "Obligatoria: geolocalizacion exacta del lugar"
            ) {
                BotonDeUbicacion(
                    onUbicacionObtenida = vm::registrarUbicacion,
                    etiqueta = if (estado.tieneUbicacion) "Actualizar ubicacion" else "Registrar mi ubicacion",
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(
                    visible = estado.tieneUbicacion,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(color = Exito.copy(alpha = 0.10f), shape = MaterialTheme.shapes.small) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Exito)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Lat %.5f · Lon %.5f".format(estado.latitud ?: 0.0, estado.longitud ?: 0.0),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                // API externa: direccion de las coordenadas (OpenStreetMap)
                DireccionGpsInfo(
                    gps = estado.gps,
                    onUsarDireccion = if (habilitado && estado.puedeUsarDireccionGps) vm::usarDireccionGps else null,
                    onReintentar = if (habilitado) vm::reintentarDireccion else null
                )
                MensajeEvidencia(estado.error(CampoGuia.UBICACION))
            }

            BotonGuardar(
                onClick = vm::guardar,
                texto = "Enviar reporte",
                guardando = estado.guardando
            )
        }
    }
}

/** Mensaje de error animado para la evidencia (foto o ubicacion) que falta. */
@Composable
private fun MensajeEvidencia(error: String?) {
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

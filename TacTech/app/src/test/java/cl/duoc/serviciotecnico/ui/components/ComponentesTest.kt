package cl.duoc.serviciotecnico.ui.components

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import cl.duoc.serviciotecnico.data.repository.EstadoSync
import cl.duoc.serviciotecnico.domain.model.DireccionGps
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.ticket
import cl.duoc.serviciotecnico.ui.theme.ServicioTecnicoTheme
import cl.duoc.serviciotecnico.ui.viewmodel.DireccionGpsUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Componentes reutilizables de la interfaz (Compose), probados aislados de las pantallas. */
@RunWith(RobolectricTestRunner::class)
class ComponentesTest : PruebaDePantalla() {

    @Test
    fun bannerDeConexionEnCadaSituacion() {
        var sincronizar = 0
        mostrar {
            Column {
                BannerConexion(EstadoSync(enLinea = false), { sincronizar++ })
                BannerConexion(EstadoSync(enLinea = false, pendientes = 2), { sincronizar++ })
                BannerConexion(EstadoSync(enLinea = true, sincronizando = true), { sincronizar++ })
                BannerConexion(EstadoSync(enLinea = true, error = "El servidor respondio 500"), { sincronizar++ })
                BannerConexion(EstadoSync(enLinea = true, pendientes = 3), { sincronizar++ })
                BannerConexion(EstadoSync(enLinea = true, ultimaSync = FECHA_BASE), { sincronizar++ })
            }
        }
        existe("Sin conexion · trabajando sin internet")
        existe("Sin conexion · 2 cambios guardados en el telefono")
        existe("Sincronizando con el servidor...")
        existe("El servidor respondio 500")
        existe("3 cambios por enviar")
        existe("Todo sincronizado", parcial = true)

        tocar("Sincronizar")
        assertEquals(1, sincronizar)
    }

    @Test
    fun etiquetasDeEstadoPrioridadYSolicitudEnTemaClaroYOscuro() {
        compose.setContent {
            Column {
                ServicioTecnicoTheme(enOscuro = false) {
                    Column {
                        EstadoTicket.entries.forEach { EtiquetaEstado(it) }
                        EstadoSolicitud.entries.forEach { EtiquetaSolicitud(it) }
                        Prioridad.entries.forEach { EtiquetaPrioridad(it) }
                    }
                }
                ServicioTecnicoTheme(enOscuro = true) {
                    Column {
                        Text("Tema oscuro", color = MaterialTheme.colorScheme.onSurface)
                        EstadoTicket.entries.forEach { EtiquetaEstado(it) }
                        EstadoSolicitud.entries.forEach { EtiquetaSolicitud(it) }
                    }
                }
            }
        }
        EstadoTicket.entries.forEach { existe(it.etiqueta) }
        EstadoSolicitud.entries.forEach { existe(it.etiqueta) }
        existe("Prioridad alta")
        existe("Tema oscuro")
    }

    @Test
    fun campoDeTextoConErrorValidoYClave() {
        var texto = ""
        mostrar {
            Column {
                CampoTextoValidado(
                    valor = "abc", onValorCambia = { texto = it }, etiqueta = "Titulo", icono = Icons.Outlined.Person,
                    error = "El titulo es obligatorio", obligatorio = true, largoMaximo = 60
                )
                CampoTextoValidado(
                    valor = "Ana Rojas", onValorCambia = {}, etiqueta = "Nombre", icono = Icons.Outlined.Person,
                    valido = true, ayuda = "Nombre y apellido", lineasMaximas = 3
                )
                CampoTextoValidado(
                    valor = "secreta", onValorCambia = {}, etiqueta = "Clave", icono = Icons.Outlined.Person, esClave = true
                )
            }
        }
        existe("El titulo es obligatorio")
        existe("3/60")
        existe("Nombre y apellido")
        existeIcono("Campo correcto")
        existeIcono("Titulo, con error", parcial = true)

        tocarIcono("Mostrar contraseña")
        existeIcono("Ocultar contraseña")

        escribir("Titulo", "Equipo no enfria")
        assertEquals("Equipo no enfria", texto)
    }

    @Test
    fun selectorDesplegableConYSinOpciones() {
        var elegido: String? = null
        mostrar {
            Column {
                SelectorDesplegable(
                    etiqueta = "Soporte tecnico", opciones = listOf("Tecnico Uno", "Tecnico Dos"), seleccionado = null,
                    textoDe = { it }, onSeleccionar = { elegido = it }, icono = Icons.Outlined.Person,
                    error = "Elige un tecnico"
                )
                SelectorDesplegable(
                    etiqueta = "Empresa", opciones = emptyList<String>(), seleccionado = "Clinica",
                    textoDe = { it }, onSeleccionar = {}, icono = Icons.Outlined.Person, textoVacio = "Sin empresas"
                )
            }
        }
        existe("Elige un tecnico")
        existe("Selecciona una opcion")
        existeIcono("Seleccion correcta")

        tocar("Selecciona una opcion")
        tocar("Tecnico Dos")
        assertEquals("Tecnico Dos", elegido)

        tocar("Clinica")
        existe("Sin empresas")
    }

    @Test
    fun selectorDeOpcionesTarjetasYEstadoVacio() {
        var opcion = "Baja"
        var abierto = false
        var contador = 0
        mostrar {
            Column {
                SelectorDeOpciones(
                    opciones = listOf("Baja", "Alta"), seleccionada = opcion, etiqueta = { it },
                    onSeleccionar = { opcion = it }, icono = { Icons.Outlined.Person }
                )
                TarjetaTicket(ticket(), onClick = { abierto = true }, mostrarTecnico = true)
                Contador("Pendientes", 4, Color.Red, onClick = { contador++ })
                FilaDato(Icons.Outlined.Person, "Cliente", "Clinica Las Condes")
                TarjetaSeccion(titulo = "Seccion", icono = Icons.Outlined.Person, descripcion = "Descripcion") {
                    IconoCircular(Icons.Outlined.Person)
                }
                EstadoVacio(Icons.Outlined.Inbox, "Sin datos", "Todavia no hay nada") {
                    Button(onClick = {}) { Text("Accion vacia") }
                }
            }
        }
        tocar("Alta")
        assertEquals("Alta", opcion)
        tocar("Equipo no enfria")
        assertTrue(abierto)
        tocar("Pendientes")
        assertEquals(1, contador)
        existe("Clinica Las Condes", parcial = true)
        existe("Descripcion")
        existe("Todavia no hay nada")
        existe("Accion vacia")
    }

    @Test
    fun barraSuperiorYBotonGuardar() {
        var volvio = false
        var guardo = false
        mostrar {
            Column {
                BarraSuperior("Titulo de pantalla", subtitulo = "Subtitulo", onVolver = { volvio = true }) {
                    Text("Accion")
                }
                BotonGuardar(onClick = { guardo = true }, texto = "Guardar ahora")
                BotonGuardar(onClick = {}, texto = "Guardando", guardando = true)
                BotonGuardar(onClick = {}, texto = "Deshabilitado", habilitado = false)
            }
        }
        existe("Subtitulo")
        tocarIcono("Volver")
        assertTrue(volvio)
        tocar("Guardar ahora")
        assertTrue(guardo)
    }

    @Test
    fun dialogoDePermisoDenegado() {
        var cerrado = 0
        mostrar {
            DialogoPermisoDenegado(titulo = "Permiso de camara", explicacion = "Se usa para las fotos.", onCerrar = { cerrado++ })
        }
        existe("Permiso de camara")
        tocarEnDialogo("Continuar sin esto")
        tocarEnDialogo("Abrir ajustes")
        assertEquals(2, cerrado)
    }

    @Test
    fun direccionGpsEnCadaEstado() {
        var usada = false
        var reintento = false
        val direccion = DireccionGps("Avenida Apoquindo", "1234", "Las Condes", "RM", "1234, Avenida Apoquindo, Las Condes")
        mostrar {
            Column {
                DireccionGpsInfo(DireccionGpsUi.Buscando)
                DireccionGpsInfo(DireccionGpsUi(direccion = direccion), onUsarDireccion = { usada = true })
                DireccionGpsInfo(DireccionGpsUi(error = "Sin internet"), onReintentar = { reintento = true })
                DireccionGpsInfo(DireccionGpsUi())
            }
        }
        existe("Buscando la direccion en OpenStreetMap...")
        existe("Direccion segun GPS")
        existe("colaboradores de OpenStreetMap", parcial = true)
        tocar("Usar esta direccion en el reporte")
        assertTrue(usada)
        tocar("Reintentar")
        assertTrue(reintento)
    }

    @Test
    fun galeriaDeFotosPermiteQuitar() {
        var quitada: Uri? = null
        val foto = Uri.parse("content://cl.duoc.serviciotecnico.fileprovider/fotos_guias/guia_1.jpg")
        mostrar {
            GaleriaDeFotos(fotos = listOf(foto, Uri.parse("content://otra/foto2.jpg")), onQuitarFoto = { quitada = it })
        }
        existeIcono("Fotografia del trabajo realizado")
        tocarIcono("Quitar fotografia")
        assertEquals(foto, quitada)
    }

    @Test
    fun botonesDeCamaraYUbicacionPidenPermiso() {
        mostrar {
            Column {
                BotonDeFoto(onFotoTomada = {})
                BotonDeUbicacion(onUbicacionObtenida = { _, _ -> })
            }
        }
        // Sin permiso concedido: se lanza la solicitud del permiso al sistema
        tocar("Tomar fotografia")
        tocar("Registrar mi ubicacion")
        existe("Tomar fotografia")
    }

    @Test
    fun conPermisoDeCamaraSeAbreLaCamara() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        mostrar { BotonDeFoto(onFotoTomada = {}, etiqueta = "Foto de evidencia") }
        tocar("Foto de evidencia")
        existe("Foto de evidencia")
    }
}
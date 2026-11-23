package cl.duoc.serviciotecnico.data.remote

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraccion de "hay internet en el telefono".
 *
 * La implementacion real es [MonitorConexion] (usa ConnectivityManager de Android).
 * En las pruebas unitarias se reemplaza por un MutableStateFlow, asi la sincronizacion
 * se prueba en la JVM sin un dispositivo.
 */
interface EstadoRed {
    val enLinea: StateFlow<Boolean>
}

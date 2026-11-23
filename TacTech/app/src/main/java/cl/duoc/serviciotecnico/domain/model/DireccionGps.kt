package cl.duoc.serviciotecnico.domain.model

/**
 * Direccion obtenida desde las coordenadas GPS de la visita (API externa de geocodificacion inversa).
 *
 * @property completa texto completo que entrega el servicio de mapas.
 */
data class DireccionGps(
    val calle: String?,
    val numero: String?,
    val comuna: String?,
    val region: String?,
    val completa: String
) {
    /** Version corta para el reporte, por ejemplo "Avenida Apoquindo 1234, Las Condes". */
    val corta: String
        get() {
            val via = listOfNotNull(calle, numero).joinToString(" ")
            val partes = listOfNotNull(via.ifBlank { null }, comuna)
            return if (partes.isEmpty()) completa else partes.joinToString(", ")
        }
}

package cl.duoc.serviciotecnico.ui.viewmodel

/**
 * Estado de validacion de cualquier formulario, generico por su enum de campos.
 *
 * - [errores] lo calcula un validador de domain/validation.
 * - Un error solo se muestra si el usuario ya toco el campo o intento enviar,
 *   para no llenar de rojo un formulario recien abierto.
 */
data class EstadoFormulario<C : Enum<C>>(
    val errores: Map<C, String> = emptyMap(),
    val tocados: Set<C> = emptySet(),
    val intentoEnviar: Boolean = false
) {
    val esValido: Boolean get() = errores.isEmpty()

    private fun visible(campo: C): Boolean = intentoEnviar || campo in tocados

    /** Mensaje a mostrar bajo el campo, o null. */
    fun error(campo: C): String? = if (visible(campo)) errores[campo] else null

    /** true cuando el campo ya se reviso y no tiene error: la UI muestra el check verde. */
    fun correcto(campo: C): Boolean = visible(campo) && campo !in errores

    fun tocar(campo: C): EstadoFormulario<C> = copy(tocados = tocados + campo)

    fun conErrores(nuevos: Map<C, String>): EstadoFormulario<C> = copy(errores = nuevos)

    fun enviar(): EstadoFormulario<C> = copy(intentoEnviar = true)
}

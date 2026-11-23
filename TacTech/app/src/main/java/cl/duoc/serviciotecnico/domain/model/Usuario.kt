package cl.duoc.serviciotecnico.domain.model

/** Usuario con sesion iniciada. Se guarda en DataStore, nunca su contraseña. */
data class Usuario(
    val correo: String,
    val nombre: String,
    val rol: Rol,
    /** Solo para clientes: empresa a la que pertenecen. */
    val empresaNombre: String? = null
) {
    val esAdministrador: Boolean get() = rol == Rol.ADMINISTRADOR
    val esTecnico: Boolean get() = rol == Rol.TECNICO
    val esCliente: Boolean get() = rol == Rol.CLIENTE
}

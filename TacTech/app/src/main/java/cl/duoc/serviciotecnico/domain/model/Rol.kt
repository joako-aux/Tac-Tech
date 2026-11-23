package cl.duoc.serviciotecnico.domain.model

enum class Rol(val etiqueta: String) {
    /** Gestiona usuarios, empresas, solicitudes y tickets. */
    ADMINISTRADOR("Administrador"),
    /** Soporte tecnico en terreno: atiende los tickets asignados. */
    TECNICO("Tecnico"),
    /** Usuario de una empresa cliente: crea solicitudes de servicio. */
    CLIENTE("Cliente")
}

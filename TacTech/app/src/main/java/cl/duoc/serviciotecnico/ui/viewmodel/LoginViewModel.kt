package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.data.repository.ResultadoLogin
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoLogin
import cl.duoc.serviciotecnico.domain.validation.ValidadorLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val formulario: EstadoFormulario<CampoLogin> = EstadoFormulario(),
    val ingresando: Boolean = false,
    /** Rol del usuario que ingreso: la navegacion decide a que pantalla ir. */
    val rolIngresado: Rol? = null,
    val mensaje: String? = null
)

class LoginViewModel(
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    init {
        validar()
    }

    fun cambiarCorreo(valor: String) {
        _estado.update {
            it.copy(correo = valor.trim(), formulario = it.formulario.tocar(CampoLogin.CORREO), mensaje = null)
        }
        validar()
    }

    fun cambiarClave(valor: String) {
        _estado.update {
            it.copy(clave = valor, formulario = it.formulario.tocar(CampoLogin.CLAVE), mensaje = null)
        }
        validar()
    }

    // La regla vive en domain/validation; aqui solo se aplica al estado
    private fun validar() {
        _estado.update { it.copy(formulario = it.formulario.conErrores(ValidadorLogin.validar(it.correo, it.clave))) }
    }

    fun ingresar() {
        _estado.update { it.copy(formulario = it.formulario.enviar()) }
        val s = _estado.value
        if (!s.formulario.esValido || s.ingresando) return

        _estado.update { it.copy(ingresando = true, mensaje = null) }
        viewModelScope.launch {
            // Intenta el backend y, si no hay red, los usuarios locales (ver SesionRepository)
            when (val r = sesionRepository.iniciarSesion(s.correo, s.clave)) {
                is ResultadoLogin.Exito -> _estado.update {
                    it.copy(ingresando = false, rolIngresado = r.usuario.rol)
                }
                ResultadoLogin.CredencialesInvalidas -> _estado.update {
                    it.copy(ingresando = false, mensaje = "Correo o contraseña incorrectos")
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ServicioTecnicoApp
                LoginViewModel(app.sesionRepository)
            }
        }
    }
}

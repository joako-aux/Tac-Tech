package cl.duoc.serviciotecnico.domain.validation

/**
 * Reglas de validacion base, en Kotlin puro.
 *
 * Cada regla devuelve el mensaje de error o null si el valor es correcto.
 * No dependen de Android ni de Compose, por eso se prueban con JUnit
 * (ver app/src/test) y se reutilizan en todos los formularios.
 */
object Validaciones {

    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_NOMBRE = Regex("^[\\p{L} .'-]+$")
    private val REGEX_SERIE = Regex("^[A-Z0-9-]+$")

    fun obligatorio(valor: String, campo: String): String? =
        if (valor.isBlank()) "$campo es obligatorio" else null

    fun largoMinimo(valor: String, minimo: Int, campo: String): String? = when {
        valor.isBlank() -> "$campo es obligatorio"
        valor.trim().length < minimo -> "$campo debe tener al menos $minimo caracteres"
        else -> null
    }

    fun largoMaximo(valor: String, maximo: Int, campo: String): String? =
        if (valor.trim().length > maximo) "$campo admite hasta $maximo caracteres" else null

    fun rango(valor: String, minimo: Int, maximo: Int, campo: String): String? =
        largoMinimo(valor, minimo, campo) ?: largoMaximo(valor, maximo, campo)

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "El correo es obligatorio"
        !REGEX_CORREO.matches(valor.trim()) -> "Ingresa un correo valido (ej: nombre@empresa.cl)"
        else -> null
    }

    /** Nombres de personas: solo letras, espacios, punto, apostrofe y guion. */
    fun nombrePersona(valor: String, campo: String): String? = when {
        valor.isBlank() -> "$campo es obligatorio"
        valor.trim().length < 3 -> "$campo debe tener al menos 3 caracteres"
        !REGEX_NOMBRE.matches(valor.trim()) -> "$campo solo puede contener letras"
        else -> null
    }

    /** Numero de serie: letras mayusculas, numeros y guion, entre 4 y 20 caracteres. */
    fun serie(valor: String): String? = when {
        valor.isBlank() -> "El numero de serie es obligatorio"
        valor.trim().length !in 4..20 -> "La serie debe tener entre 4 y 20 caracteres"
        !REGEX_SERIE.matches(valor.trim()) -> "La serie solo admite letras, numeros y guion"
        else -> null
    }

    /** RUT chileno con digito verificador (modulo 11). Acepta 12.345.678-5 o 12345678-5. */
    fun rut(valor: String): String? {
        if (valor.isBlank()) return "El RUT es obligatorio"
        val limpio = valor.replace(".", "").replace(" ", "").uppercase()
        if (!Regex("^\\d{7,8}-[\\dK]$").matches(limpio)) return "Formato de RUT: 12345678-5"
        val cuerpo = limpio.substringBefore('-')
        val dv = limpio.last()
        return if (digitoVerificador(cuerpo) == dv) null else "El digito verificador del RUT no es correcto"
    }

    fun digitoVerificador(cuerpo: String): Char {
        var suma = 0
        var factor = 2
        for (c in cuerpo.reversed()) {
            suma += c.digitToInt() * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val resto = 11 - suma % 11) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + resto
        }
    }

    /** Telefono chileno opcional: +56 y 9 digitos. */
    fun telefonoOpcional(valor: String): String? = when {
        valor.isBlank() -> null
        !Regex("^\\+?56\\d{9}$").matches(valor.replace(" ", "")) -> "Formato: +56912345678"
        else -> null
    }
}

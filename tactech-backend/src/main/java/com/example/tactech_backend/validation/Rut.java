package com.example.tactech_backend.validation;

/** Utilidades del RUT chileno: normalizar y verificar el digito verificador (modulo 11). */
public final class Rut {

    private Rut() {
    }

    /** "76.123.456-0" -> "76123456-0" (mayuscula en K). Devuelve null si no tiene forma de RUT. */
    public static String normalizar(String rut) {
        if (rut == null) return null;
        String limpio = rut.replace(".", "").replace(" ", "").toUpperCase();
        if (!limpio.matches("^\\d{7,8}-[\\dK]$")) return null;
        return limpio;
    }

    public static boolean esValido(String rut) {
        String normal = normalizar(rut);
        if (normal == null) return false;
        String cuerpo = normal.substring(0, normal.indexOf('-'));
        char dv = normal.charAt(normal.length() - 1);
        return digitoVerificador(cuerpo) == dv;
    }

    static char digitoVerificador(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }
}

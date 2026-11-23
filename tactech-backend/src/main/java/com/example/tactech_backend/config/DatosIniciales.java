package com.example.tactech_backend.config;

import com.example.tactech_backend.entity.Empresa;
import com.example.tactech_backend.entity.Rol;
import com.example.tactech_backend.entity.Usuario;
import com.example.tactech_backend.repository.EmpresaRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea una empresa y los usuarios de prueba la primera vez que arranca el servicio.
 * Son los mismos que la app acepta sin conexion (SesionRepository).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Empresa clinica = empresaRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getRut().equals("76123456-0"))
                .findFirst()
                .orElseGet(() -> empresaRepository.save(Empresa.builder()
                        .nombre("Clinica Las Condes")
                        .rut("76123456-0")
                        .direccion("Estoril 450, Las Condes")
                        .telefono("+56222104000")
                        .build()));

        crearSiNoExiste("Administrador", "admin@tactech.cl", "admin123", Rol.ADMINISTRADOR, null);
        crearSiNoExiste("Tecnico Demo", "tecnico@tactech.cl", "tecnico123", Rol.TECNICO, null);
        crearSiNoExiste("Tecnico Dos", "tecnico2@tactech.cl", "tecnico123", Rol.TECNICO, null);
        crearSiNoExiste("Cliente Demo", "cliente@tactech.cl", "cliente123", Rol.CLIENTE, clinica);
    }

    private void crearSiNoExiste(String nombre, String correo, String clave, Rol rol, Empresa empresa) {
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) return;
        usuarioRepository.save(Usuario.builder()
                .nombre(nombre)
                .correo(correo)
                .clave(passwordEncoder.encode(clave))
                .rol(rol)
                .activo(true)
                .empresa(empresa)
                .build());
        log.info("Usuario de prueba creado: {} ({})", correo, rol);
    }
}

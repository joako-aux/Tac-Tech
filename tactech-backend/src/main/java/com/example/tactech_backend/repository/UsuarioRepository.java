package com.example.tactech_backend.repository;

import com.example.tactech_backend.entity.Rol;
import com.example.tactech_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByEmpresa_Id(Long empresaId);
}

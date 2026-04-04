package com.celfinder.Procesos;

import com.celfinder.Model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UsuarioService implements UserDetailsService {

    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(MongoTemplate mongoTemplate, PasswordEncoder passwordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Query query = new Query(Criteria.where("nombreUsuario").is(nombreUsuario));
        Usuario usuario = mongoTemplate.findOne(query, Usuario.class, "usuarios");

        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + nombreUsuario);
        }

        if (usuario.getEstadoCuenta() == null) {
            usuario.setEstadoCuenta("activa");
            mongoTemplate.save(usuario, "usuarios");
        }

        if (!usuario.getRoles().contains("ROLE_USER")) {
            throw new UsernameNotFoundException("El usuario " + nombreUsuario + " no tiene el rol necesario (ROLE_USER).");
        }

        if (!"activa".equals(usuario.getEstadoCuenta())) {
            throw new UsernameNotFoundException("La cuenta de " + nombreUsuario + " no está activa.");
        }

        System.out.println("loadUserByUsername: nombreUsuario=" + nombreUsuario + ", imgperfil=" + (usuario.getImagenPerfil() == null ? "null" : "present (" + usuario.getImagenPerfil().length() + " chars)"));
        return usuario;
    }

    public boolean nombreUsuarioExiste(String nombreUsuario) {
        Query query = new Query(Criteria.where("nombreUsuario").is(nombreUsuario));
        return mongoTemplate.exists(query, Usuario.class, "usuarios");
    }

    public boolean verificarUsuario(String nombreUsuario, String contrasenaPlano) {
        Query query = new Query(Criteria.where("nombreUsuario").is(nombreUsuario));
        Usuario usuario = mongoTemplate.findOne(query, Usuario.class, "usuarios");

        if (usuario == null) {
            return false;
        }

        if (!usuario.getRoles().contains("ROLE_USER")) {
            return false;
        }

        if (!"activa".equals(usuario.getEstadoCuenta())) {
            return false;
        }

        return passwordEncoder.matches(contrasenaPlano, usuario.getContrasena());
    }

    public void registrarUsuario(Usuario usuario) {
        if (usuario.getNombreUsuario() == null || usuario.getNombreUsuario().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }

        if (usuario.getContrasena() == null || usuario.getContrasena().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        if (nombreUsuarioExiste(usuario.getNombreUsuario())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        usuario.getRoles().add("ROLE_USER");
        usuario.setEstadoCuenta("activa");
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setFechaCreacion(LocalDateTime.now());

        try {
            mongoTemplate.save(usuario, "usuarios");
            System.out.println("Usuario registrado correctamente.");
        } catch (Exception e) {
            System.err.println("Error al registrar el usuario: " + e.getMessage());
            throw new RuntimeException("Error al guardar el usuario en la base de datos", e);
        }
    }

    public String obtenerNombreUsuarioPorId(String id) {
        Usuario usuario = mongoTemplate.findById(id, Usuario.class, "usuarios");
        return usuario != null ? usuario.getNombreUsuario() : "Vendedor desconocido";
    }

    // Nuevo método para obtener usuario por ID
    public Usuario obtenerUsuarioPorId(String id) {
        return mongoTemplate.findById(id, Usuario.class, "usuarios");
    }

    public Usuario obtenerUsuarioPorNombre(String nombreUsuario) {
        Query query = new Query(Criteria.where("nombreUsuario").is(nombreUsuario));
        Usuario usuario = mongoTemplate.findOne(query, Usuario.class, "usuarios");
        System.out.println("obtenerUsuarioPorNombre: nombreUsuario=" + nombreUsuario + ", imgperfil=" + (usuario == null ? "null" : (usuario.getImagenPerfil() == null ? "null" : "present (" + usuario.getImagenPerfil().length() + " chars)")));
        return usuario;
    }

    public void actualizarPerfil(Usuario usuario, String email, String ciudad, String departamento, String telefono, String imgperfil, String fondoperfil) {
        if (usuario == null) {
            System.err.println("actualizarPerfil: El objeto usuario es NULL");
            return;
        }
        
        System.out.println("actualizarPerfil: Iniciando guardado para " + usuario.getNombreUsuario());
        System.out.println("  ID documento: " + usuario.getId());
        
        usuario.setEmail(email);
        usuario.setCiudad(ciudad);
        usuario.setDepartamento(departamento);
        usuario.setTelefono(telefono);
        usuario.setImagenPerfil(imgperfil);
        usuario.setFondoPerfil(fondoperfil);
        
        try {
            mongoTemplate.save(usuario, "usuarios");
            System.out.println("actualizarPerfil: mongoTemplate.save completado satisfactoriamente.");
            System.out.println("  Confirmo -> Email set: " + usuario.getEmail() + ", ImgPerfil len: " + (imgperfil == null ? 0 : imgperfil.length()));
        } catch (Exception e) {
            System.err.println("actualizarPerfil: ERROR al guardar en MongoDB: " + e.getMessage());
            throw e;
        }
    }
}
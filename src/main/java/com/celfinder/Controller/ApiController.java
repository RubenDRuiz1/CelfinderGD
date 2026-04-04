package com.celfinder.Controller;

import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final UsuarioService usuarioService;

    @Autowired
    public ApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/user")
    public Map<String, String> getCurrentUser(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            String nombreUsuario = authentication.getName();
            Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
            if (usuario != null) {
                response.put("nombreUsuario", usuario.getNombreUsuario());
                String imgperfil = usuario.getImagenPerfil() != null ? usuario.getImagenPerfil() : "";
                response.put("imgperfil", imgperfil);
                System.out.println("API /user: nombreUsuario=" + usuario.getNombreUsuario() + ", imgperfil=" + (imgperfil.isEmpty() ? "empty" : "present (" + imgperfil.length() + " chars)"));
            } else {
                response.put("nombreUsuario", "Invitado");
                response.put("imgperfil", "");
                System.out.println("API /user: Usuario not found for nombreUsuario=" + nombreUsuario);
            }
        } else {
            response.put("nombreUsuario", "Invitado");
            response.put("imgperfil", "");
            System.out.println("API /user: No authenticated user");
        }
        return response;
    }
}
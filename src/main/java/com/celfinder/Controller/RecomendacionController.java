package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Procesos.RecomendacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RecomendacionController {

    @Autowired
    private RecomendacionService recomendacionService;

    @GetMapping("/recomendaciones")
    public String mostrarRecomendaciones(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/usuarios/login";
        }

        String usuarioId = ((com.celfinder.Model.Usuario) auth.getPrincipal()).getId();
        List<Producto> recomendados = recomendacionService.recomendarParaUsuario(usuarioId);
        model.addAttribute("recomendados", recomendados);
        return "recomendaciones";
    }
}
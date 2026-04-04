package com.celfinder.Controller;

import com.celfinder.Model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Set;

@Controller
public class HomeController {

    @GetMapping("/menu")
    public String mostrarRegistro(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Usuario) {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            Set<String> roles = usuario.getRoles();
            model.addAttribute("userRoles", roles);
        } else {
            model.addAttribute("userRoles", new ArrayList<>());
        }
        return "menu"; 
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index"; 
    }
}
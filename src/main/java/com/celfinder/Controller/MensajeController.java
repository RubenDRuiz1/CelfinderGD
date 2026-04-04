package com.celfinder.Controller;

import com.celfinder.Model.Mensaje;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.MensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/mensajes")
public class MensajeController {

    @Autowired
    private MensajeService mensajeService;

    @GetMapping("/chat/{solicitudId}")
    public String verChat(@PathVariable String solicitudId, Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/usuarios/login";
        }

        String usuarioId = ((Usuario) auth.getPrincipal()).getId();
        List<Mensaje> mensajes = mensajeService.obtenerMensajesPorSolicitud(solicitudId);

        model.addAttribute("mensajes", mensajes);
        model.addAttribute("solicitudId", solicitudId);
        model.addAttribute("usuarioId", usuarioId);

        return "chat";
    }

    @PostMapping("/enviar")
    public String enviarMensaje(@RequestParam String solicitudId,
                               @RequestParam String contenido,
                               Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/usuarios/login";
        }

        String emisorId = ((Usuario) auth.getPrincipal()).getId();
        mensajeService.enviarMensaje(solicitudId, emisorId, contenido.trim());

        return "redirect:/mensajes/chat/" + solicitudId;
    }
}
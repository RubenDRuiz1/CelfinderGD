package com.celfinder.Controller;

import com.celfinder.Procesos.AsistenteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AsistenteController {

    private final AsistenteService asistenteService;

    public AsistenteController(AsistenteService asistenteService) {
        this.asistenteService = asistenteService;
    }

    @GetMapping("/asistente")
    public String cargarChat(Model model, HttpSession session) {
        model.addAttribute("historial", asistenteService.getHistorial(session));
        return "asistente";
    }

    @PostMapping("/asistente")
    public String enviarMensaje(@RequestParam String mensaje, Model model, HttpSession session) {
        String respuesta = asistenteService.procesarMensaje(mensaje, session);
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("respuesta", respuesta);
        model.addAttribute("historial", asistenteService.getHistorial(session));
        return "asistente";
    }
}

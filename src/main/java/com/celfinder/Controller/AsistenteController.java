package com.celfinder.Controller;

import com.celfinder.Model.ChatThread;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.AsistenteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
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
    public String cargarChat(@RequestParam(required = false) String threadId, 
                            @RequestParam(required = false, defaultValue = "false") boolean compact,
                            Model model, 
                            Authentication authentication,
                            HttpSession session) {
        
        Usuario user = (Usuario) authentication.getPrincipal();
        String usuarioId = user.getId();
        
        ChatThread thread = asistenteService.obtenerConversacion(threadId, usuarioId);
        session.setAttribute("CURRENT_THREAD_ID", thread.getId());
        
        model.addAttribute("historial", thread.getMensajes());
        model.addAttribute("threadId", thread.getId());
        model.addAttribute("titulo", thread.getTitulo());
        model.addAttribute("misChats", asistenteService.obtenerMisChats(usuarioId));
        model.addAttribute("compact", compact);
        return "asistente";
    }

    @PostMapping("/asistente")
    public String enviarMensaje(@RequestParam String mensaje,
                                @RequestParam(required = false) String threadId,
                                @RequestParam(required = false, defaultValue = "false") boolean compact,
                                Model model,
                                Authentication authentication,
                                HttpSession session) {
        
        Usuario user = (Usuario) authentication.getPrincipal();
        String usuarioId = user.getId();
        
        if (threadId == null || threadId.isEmpty())
            threadId = (String) session.getAttribute("CURRENT_THREAD_ID");
            
        String respuesta = asistenteService.procesarMensaje(mensaje, threadId, usuarioId);
        ChatThread thread = asistenteService.obtenerConversacion(threadId, usuarioId);
        
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("respuesta", respuesta);
        model.addAttribute("historial", thread.getMensajes());
        model.addAttribute("threadId", thread.getId());
        model.addAttribute("titulo", thread.getTitulo());
        model.addAttribute("misChats", asistenteService.obtenerMisChats(usuarioId));
        model.addAttribute("compact", compact);
        return "asistente";
    }
}

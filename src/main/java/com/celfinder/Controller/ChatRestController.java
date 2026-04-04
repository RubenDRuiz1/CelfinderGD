package com.celfinder.Controller;

import com.celfinder.Model.ChatMensaje;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.ChatService;
import com.celfinder.Procesos.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;
    private final UsuarioService usuarioService;

    public ChatRestController(ChatService chatService, UsuarioService usuarioService) {
        this.chatService = chatService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ChatMensaje>> getHistorial(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(authentication.getName());
        if (usuario == null) return ResponseEntity.status(404).build();

        return ResponseEntity.ok(chatService.obtenerHistorial(usuario.getId()));
    }

    @PostMapping("/preguntar")
    public ResponseEntity<Map<String, String>> preguntar(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String textoUsuario = payload.get("mensaje");
        if (textoUsuario == null || textoUsuario.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(authentication.getName());
        if (usuario == null) return ResponseEntity.status(404).build();

        String respuesta = chatService.procesarPregunta(usuario.getId(), textoUsuario);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }
}

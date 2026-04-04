package com.celfinder.Controller;

import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionRestController {

    private final AdminService adminService;

    public NotificacionRestController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/conteo-no-leidas")
    public Map<String, Object> obtenerConteoNoLeidas(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            long count = adminService.contarNotificacionesNoLeidas(usuario.getId());
            response.put("conteo", count);
            response.put("success", true);
        } else {
            response.put("conteo", 0);
            response.put("success", false);
        }
        return response;
    }

    @PostMapping("/marcar-leida/{id}")
    public Map<String, Object> marcarComoLeida(@PathVariable String id, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            adminService.marcarComoLeida(id);
            response.put("success", true);
        } else {
            response.put("success", false);
        }
        return response;
    }

    @PostMapping("/marcar-todas-leidas")
    public Map<String, Object> marcarTodasComoLeidas(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            adminService.marcarTodasComoLeidas(usuario.getId());
            response.put("success", true);
        } else {
            response.put("success", false);
        }
        return response;
    }
}

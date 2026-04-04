package com.celfinder.Controller;

import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.AdminService;
import com.celfinder.Procesos.PurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador de historiales y notificaciones.
 * Rutas: /historial-compras, /historial-ventas, /historial-solicitudes-vendedor, /notificaciones
 */
@Controller
@RequestMapping("/ventas")
public class VentaHistorialController {

    private static final Logger logger = LoggerFactory.getLogger(VentaHistorialController.class);

    private final PurchaseService purchaseService;
    private final AdminService adminService;

    public VentaHistorialController(PurchaseService purchaseService,
                                    AdminService adminService) {
        this.purchaseService = purchaseService;
        this.adminService = adminService;
    }

    // ---------------------------------------------------------------
    // HISTORIAL DE COMPRAS
    // ---------------------------------------------------------------

    @GetMapping("/historial-compras")
    public String mostrarHistorialCompras(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           Model model,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error",
                    "Debes iniciar sesión para ver tu historial de compras.");
            return "redirect:/ventas/listar";
        }
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Map<String, Object>> allHistorial =
                    purchaseService.obtenerHistorialComprasCompleto(usuario.getId());
            aplicarPaginacionHistorial(model, allHistorial, page, size);
            return "historialCompras";
        } catch (Exception e) {
            logger.error("Error en historial de compras para usuario {}", authentication.getName(), e);
            model.addAttribute("error", "Error al cargar el historial: " + e.getMessage());
            model.addAttribute("historial", new ArrayList<>());
            return "historialCompras";
        }
    }

    // ---------------------------------------------------------------
    // HISTORIAL DE VENTAS
    // ---------------------------------------------------------------

    @GetMapping("/historial-ventas")
    public String mostrarHistorialVentas(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          Model model,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error",
                    "Debes iniciar sesión para ver tu historial de ventas.");
            return "redirect:/ventas/listar";
        }
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Map<String, Object>> allHistorial =
                    purchaseService.obtenerHistorialVentasCompleto(usuario.getId());
            aplicarPaginacionHistorial(model, allHistorial, page, size);
            return "historialVentas";
        } catch (Exception e) {
            logger.error("Error en historial de ventas para usuario {}", authentication.getName(), e);
            model.addAttribute("error", "Error al cargar el historial de ventas: " + e.getMessage());
            model.addAttribute("historial", new ArrayList<>());
            return "historialVentas";
        }
    }

    // ---------------------------------------------------------------
    // HISTORIAL DE SOLICITUDES VENDEDOR
    // ---------------------------------------------------------------

    @GetMapping("/historial-solicitudes-vendedor")
    public String mostrarHistorialSolicitudesVendedor(Model model,
                                                       Authentication authentication,
                                                       RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            List<Solicitud> historial = adminService.obtenerHistorialSolicitudesVendedor(usuario.getId());
            model.addAttribute("solicitudes", historial != null ? historial : new ArrayList<>());
            return "historialSolicitudesVendedor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al cargar el historial de solicitudes: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    // ---------------------------------------------------------------
    // NOTIFICACIONES
    // ---------------------------------------------------------------

    @GetMapping("/notificaciones")
    public String mostrarNotificaciones(Model model,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            List<Notificacion> notificaciones =
                    adminService.obtenerNotificacionesPorUsuario(usuario.getId());
            model.addAttribute("notificaciones", notificaciones != null ? notificaciones : new ArrayList<>());
            return "notificaciones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al cargar las notificaciones: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    private Usuario requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Debes estar autenticado para realizar esta acción.");
        }
        return (Usuario) authentication.getPrincipal();
    }

    private void aplicarPaginacionHistorial(Model model, List<Map<String, Object>> allHistorial,
                                             int page, int size) {
        int totalItems = allHistorial.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int start = page * size;
        int end = Math.min(start + size, totalItems);
        List<Map<String, Object>> historial =
                totalItems > 0 ? allHistorial.subList(start, end) : new ArrayList<>();

        model.addAttribute("historial", historial);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", size);
    }
}

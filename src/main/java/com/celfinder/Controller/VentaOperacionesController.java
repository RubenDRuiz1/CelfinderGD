package com.celfinder.Controller;

import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.NotificationService;
import com.celfinder.Procesos.ProductQueryService;
import com.celfinder.Procesos.PurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.List;

/**
 * Controlador de transacciones y solicitudes de compra.
 * Rutas: /solicitar/{id}, /procesar-compra, /gestionar-solicitudes, /gestionar-solicitud/{id}
 */
@Controller
@RequestMapping("/ventas")
public class VentaOperacionesController {

    private static final Logger logger = LoggerFactory.getLogger(VentaOperacionesController.class);

    private final PurchaseService purchaseService;
    private final ProductQueryService productQueryService;
    private final NotificationService notificationService;

    public VentaOperacionesController(PurchaseService purchaseService,
                                      ProductQueryService productQueryService,
                                      NotificationService notificationService) {
        this.purchaseService = purchaseService;
        this.productQueryService = productQueryService;
        this.notificationService = notificationService;
    }

    // ---------------------------------------------------------------
    // SOLICITAR COMPRA
    // ---------------------------------------------------------------

    @GetMapping("/solicitar/{id}")
    public String mostrarSolicitudCompra(@PathVariable String id,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes,
                                         Model model) {
        try {
            Usuario comprador = requireAuthenticated(authentication);
            purchaseService.validarCompra(comprador.getId(), id);

            model.addAttribute("producto", productQueryService.obtenerProductoPorId(id));
            model.addAttribute("comprador", comprador);
            return "solicitarCompra";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al iniciar la compra: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // ---------------------------------------------------------------
    // PROCESAR COMPRA
    // ---------------------------------------------------------------

    @PostMapping("/procesar-compra")
    public String procesarCompraSimple(@RequestParam String productoId,
                                       @RequestParam String direccion,
                                       @RequestParam String correo,
                                       @RequestParam String contacto,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Usuario comprador = requireAuthenticated(authentication);

            Solicitud solicitud = purchaseService.crearSolicitudDesdeCompra(
                    comprador, productoId, direccion, correo, contacto);

            notificationService.notificarCompraSolicitada(
                    comprador, productQueryService.obtenerProductoPorId(solicitud.getProductoId()));

            redirectAttributes.addFlashAttribute("mensaje",
                    "¡Solicitud enviada con éxito! El vendedor la revisará pronto.");
            return "redirect:/ventas/detalle/" + productoId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/comprar/" + productoId;
        }
    }

    // ---------------------------------------------------------------
    // GESTIONAR SOLICITUDES (vista del vendedor)
    // ---------------------------------------------------------------

    @GetMapping("/gestionar-solicitudes")
    public String gestionarSolicitudes(Model model,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            List<Solicitud> todas = purchaseService.obtenerSolicitudesPorVendedor(usuario.getId());
            
            List<Solicitud> pendientes = todas.stream()
                    .filter(s -> "pendiente".equals(s.getEstado()))
                    .toList();
            List<Solicitud> autorizadas = todas.stream()
                    .filter(s -> "autorizada".equals(s.getEstado()))
                    .toList();

            model.addAttribute("solicitudesPendientes", pendientes);
            model.addAttribute("pedidosEnCurso", autorizadas);
            return "gestionarSolicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al cargar las solicitudes: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @PostMapping("/gestionar-solicitud/{id}")
    public String gestionarSolicitud(@PathVariable String id,
                                      @RequestParam String accion,
                                      @RequestParam(required = false) String descripcionVendedor,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            Solicitud solicitud = purchaseService.obtenerSolicitudPorId(id);

            if (solicitud == null || !solicitud.getTipoSolicitud().equals("compra")) {
                throw new IllegalArgumentException(
                        "La solicitud no existe o no es una solicitud de compra.");
            }
            if (!usuario.getId().equals(solicitud.getVendedorId())) {
                throw new IllegalStateException(
                        "Solo el vendedor asignado puede gestionar esta solicitud.");
            }

            purchaseService.gestionarSolicitudCompra(id, accion, descripcionVendedor);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Solicitud " + ("autorizar".equals(accion) ? "aprobada" : "rechazada") + " correctamente.");
            return "redirect:/ventas/gestionar-solicitudes";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/gestionar-solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al gestionar la solicitud: " + e.getMessage());
            return "redirect:/ventas/gestionar-solicitudes";
        }
    }

    @PostMapping("/actualizar-rastreo/{id}")
    public String actualizarRastreo(@PathVariable String id,
                                     @RequestParam String nuevoEstado,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            Usuario vendedor = requireAuthenticated(authentication);
            Solicitud solicitud = purchaseService.obtenerSolicitudPorId(id);

            if (solicitud == null || !vendedor.getId().equals(solicitud.getVendedorId())) {
                throw new IllegalStateException("No tienes permiso para actualizar este pedido.");
            }

            solicitud.setEstadoSeguimiento(nuevoEstado);
            purchaseService.guardarSolicitud(solicitud);

            notificationService.notificarCambioEstadoRastreo(solicitud, nuevoEstado);

            redirectAttributes.addFlashAttribute("mensaje", "Estado de envío actualizado a: " + nuevoEstado);
            return "redirect:/ventas/gestionar-solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar rastreo: " + e.getMessage());
            return "redirect:/ventas/gestionar-solicitudes";
        }
    }

    // ---------------------------------------------------------------
    // HELPER PRIVADO
    // ---------------------------------------------------------------

    private Usuario requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Debes estar autenticado para realizar esta acción.");
        }
        return (Usuario) authentication.getPrincipal();
    }
}

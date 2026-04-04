package com.celfinder.Controller;

import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ventas")
public class RastreoController {

    private final PurchaseService purchaseService;

    public RastreoController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/rastreo-pedido/{solicitudId}")
    public String verRastreoPedido(@PathVariable String solicitudId,
                                    Model model,
                                    Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Solicitud solicitud = purchaseService.obtenerSolicitudPorId(solicitudId);

        if (solicitud == null) return "redirect:/menu";

        // Verificar que el usuario sea el comprador o el vendedor o administrador
        if (!usuario.getId().equals(solicitud.getUsuarioId()) && 
            !usuario.getId().equals(solicitud.getVendedorId()) &&
            !usuario.getRoles().contains("ROLE_ADMIN")) {
            return "redirect:/menu";
        }

        model.addAttribute("solicitud", solicitud);
        return "rastreoPedido";
    }
}

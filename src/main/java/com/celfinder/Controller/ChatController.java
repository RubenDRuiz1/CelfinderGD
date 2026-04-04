package com.celfinder.Controller;

import com.celfinder.Model.ChatPedido;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.ChatPedidoService;
import com.celfinder.Procesos.NotificationService;
import com.celfinder.Procesos.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ventas")
public class ChatController {

    private final ChatPedidoService chatPedidoService;
    private final PurchaseService purchaseService;
    private final NotificationService notificationService;

    public ChatController(ChatPedidoService chatPedidoService, 
                          PurchaseService purchaseService,
                          NotificationService notificationService) {
        this.chatPedidoService = chatPedidoService;
        this.purchaseService = purchaseService;
        this.notificationService = notificationService;
    }

    @GetMapping("/chat-pedido/{solicitudId}")
    public String verChatPedido(@PathVariable String solicitudId, 
                                 Model model, 
                                 Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        Solicitud solicitud = purchaseService.obtenerSolicitudPorId(solicitudId);

        if (solicitud == null) return "redirect:/menu";

        // Verificar que el usuario sea parte del chat
        if (!usuario.getId().equals(solicitud.getUsuarioId()) && !usuario.getId().equals(solicitud.getVendedorId())) {
            return "redirect:/menu";
        }

        ChatPedido chat = chatPedidoService.obtenerOCrearChat(
                solicitudId, 
                solicitud.getUsuarioId(), 
                solicitud.getVendedorId(), 
                solicitud.getProductoId()
        );

        model.addAttribute("chat", chat);
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("usuarioActual", usuario);
        return "chatPedido";
    }

    @PostMapping("/chat-pedido/{chatId}/enviar")
    public String enviarMensaje(@PathVariable String chatId, 
                                 @RequestParam String contenido, 
                                 @RequestParam String solicitudId,
                                 Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        chatPedidoService.agregarMensaje(chatId, usuario.getId(), contenido);

        // Si el comprador envía el mensaje, notificar opcionalmente al vendedor (regla: "El cliente tiene una duda")
        Solicitud solicitud = purchaseService.obtenerSolicitudPorId(solicitudId);
        if (solicitud != null && usuario.getId().equals(solicitud.getUsuarioId())) {
            // Podríamos limitar esto para no saturar, pero el requerimiento dice "Al hacer clic, se debe enviar una Notificacion automática"
            // Suponemos que el "clic" es el inicio de la duda.
             notificationService.notificarDudaPedido(solicitud.getVendedorId(), usuario.getNombreUsuario(), solicitudId);
        }

        return "redirect:/ventas/chat-pedido/" + solicitudId;
    }
}

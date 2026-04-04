package com.celfinder.Procesos;

import com.celfinder.Model.ChatMensaje;
import com.celfinder.Model.Producto;
import com.celfinder.Model.CarritoItem;
import com.celfinder.Repository.ChatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final AIService aiService;
    private final ProductQueryService productQueryService;
    private final VentaService ventaService;
    private final CarritoService carritoService;

    public ChatService(ChatRepository chatRepository, 
                       AIService aiService, 
                       ProductQueryService productQueryService,
                       VentaService ventaService,
                       CarritoService carritoService) {
        this.chatRepository = chatRepository;
        this.aiService = aiService;
        this.productQueryService = productQueryService;
        this.ventaService = ventaService;
        this.carritoService = carritoService;
    }

    public List<ChatMensaje> obtenerHistorial(String usuarioId) {
        return chatRepository.findByUsuarioIdOrderByTimestampAsc(usuarioId);
    }

    public String procesarPregunta(String usuarioId, String textoUsuario) {
        // 1. Guardar mensaje del usuario
        ChatMensaje mensajeUsuario = new ChatMensaje(usuarioId, textoUsuario, "user");
        chatRepository.save(mensajeUsuario);

        // 2. Construir contexto RAG
        String contexto = construirContexto(usuarioId);

        // 3. Crear prompt final para la IA
        String promptFinal = "Contexto del sistema (CelFinder):\n" + contexto + 
                             "\n\nPregunta del usuario: " + textoUsuario;

        // 4. Obtener respuesta de la IA
        String respuestaIA = aiService.generateText(promptFinal);

        // 5. Guardar respuesta del asistente
        ChatMensaje mensajeAsistente = new ChatMensaje(usuarioId, respuestaIA, "assistant");
        chatRepository.save(mensajeAsistente);

        return respuestaIA;
    }

    private String construirContexto(String usuarioId) {
        StringBuilder sb = new StringBuilder();
        
        // Información de productos disponibles
        List<Producto> productos = productQueryService.obtenerProductosEnVenta();
        sb.append("Productos disponibles en la tienda:\n");
        for (Producto p : productos) {
            sb.append("- ").append(p.getNombre()).append(": $").append(p.getPrecio())
              .append(" (Estado: ").append(p.getEstado()).append(")\n");
        }

        // Información del carrito del usuario
        List<CarritoItem> itemsCarrito = carritoService.obtenerItems();
        if (itemsCarrito.isEmpty()) {
            sb.append("\nEl usuario actualmente no tiene productos en su carrito.\n");
        } else {
            sb.append("\nProductos en el carrito del usuario:\n");
            for (CarritoItem item : itemsCarrito) {
                sb.append("- ").append(item.getNombre()).append(" (ID: ").append(item.getProductoId()).append(")\n");
            }
            sb.append("Total del carrito: $").append(carritoService.obtenerTotal()).append("\n");
        }

        // Información de ventas/compras del usuario (VentaService)
        try {
            var historial = ventaService.obtenerHistorialCompras(usuarioId);
            if (!historial.isEmpty()) {
                sb.append("\nHistorial de compras reciente del usuario (").append(historial.size()).append(" pedidos):\n");
                for (int i = 0; i < Math.min(historial.size(), 3); i++) {
                    sb.append("- Pedido ID: ").append(historial.get(i).getId()).append(" (Estado: ").append(historial.get(i).getEstado()).append(")\n");
                }
            }
        } catch (Exception e) {
            // Silently fail if something goes wrong with the history
        }

        sb.append("\nInstrucciones: Responde de forma amable y profesional.");
        
        return sb.toString();
    }
}

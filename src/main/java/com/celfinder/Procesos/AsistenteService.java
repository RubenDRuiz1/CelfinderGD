package com.celfinder.Procesos;

import com.celfinder.Model.MensajeIA;
import com.celfinder.Model.Producto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Comparator;

@Service
public class AsistenteService {

    private final AIService aiService;
    private final ConsultaMongoService consultaMongoService;
    private final Map<String, List<MensajeIA>> chats = new HashMap<>();

    private static final String SYSTEM_PROMPT_PRODUCTOS = """
        Eres Gorge Droyd, asistente profesional de Celfinder.
        Usa SOLO los productos que te paso abajo. Nunca inventes existencias ni precios.
        Sé claro, formal pero cercano. Responde siempre en español correcto.
        """;

    private static final String SYSTEM_PROMPT_GENERAL = """
        Eres Gorge Droyd, asistente experto en tecnología de la tienda Celfinder.
        Explica conceptos técnicos de forma sencilla, clara y amable, como si le hablaras a una persona mayor o a alguien que recién está entrando al mundo de los celulares y consolas.
        Usa ejemplos cotidianos y siempre termina ofreciendo ayuda para elegir un producto.
        Responde siempre en español correcto y con tono cálido.
        """;

    public AsistenteService(AIService aiService, ConsultaMongoService consultaMongoService) {
        this.aiService = aiService;
        this.consultaMongoService = consultaMongoService;
    }

    public List<MensajeIA> getHistorial(HttpSession session) {
        return chats.computeIfAbsent(session.getId(), k -> new ArrayList<>());
    }

    public String procesarMensaje(String mensajeUsuario, HttpSession session) {
        List<MensajeIA> historial = getHistorial(session);
        historial.add(new MensajeIA("user", mensajeUsuario));

        String msgLower = mensajeUsuario.toLowerCase().trim();

        // ===================================================================
        // 1. SALUDOS – PRIORIDAD MÁXIMA
        // ===================================================================
        if (msgLower.matches(".*\\b(hola|buenos días|buenas tardes|buenas noches|qué tal|como estas|como estás)\\b.*")) {
            String resp = "¡Hola! 😊 Bienvenido a Celfinder. ¿En qué puedo ayudarte hoy?";
            historial.add(new MensajeIA("bot", resp));
            return resp;
        }

        // ===================================================================
        // 2. PREGUNTAS SOBRE LA TIENDA – ANTES DE BUSCAR EN MONGO
        // ===================================================================
        if (msgLower.contains("celfinder") || msgLower.contains("qué es esta página") || msgLower.contains("qué es esta app") || 
            msgLower.contains("qué venden") || msgLower.contains("qué vende") || msgLower.contains("qué es esto") || 
            msgLower.contains("en qué consiste") || msgLower.contains("qué hace")) {
            String resp = "¡Hola! Somos **Celfinder**, el comparador de precios número 1 de Colombia en tecnología.\n\n" +
                          "Aquí puedes comparar celulares, consolas, portátiles, drones, audífonos y miles de productos más de cientos de tiendas reales, siempre con el precio más bajo y stock actualizado al instante.\n\n" +
                          "¿Qué estás buscando hoy? Dime y te encuentro la mejor oferta en segundos 😊";
            historial.add(new MensajeIA("bot", resp));
            return resp;
        }

        if (msgLower.contains("fiable") || msgLower.contains("confiable") || msgLower.contains("seguro") || msgLower.contains("estafa") || msgLower.contains("real")) {
            String resp = "¡100% fiable y seguro! 😊\n\n" +
                          "Llevamos años ayudando a miles de personas a ahorrar. No vendemos nada directamente: solo mostramos precios reales y actualizados de tiendas serias en Colombia. Si ves un precio, es porque el producto está disponible de verdad.\n\n" +
                          "¿Quieres que te busque algo para que lo compruebes tú mismo?";
            historial.add(new MensajeIA("bot", resp));
            return resp;
        }

        // ===================================================================
        // 3. PREGUNTAS TÉCNICAS – IA libre
        // ===================================================================
        String[] tecnicas = {"qué es", "que es", "qué significa", "que significa", "qué son", "que son", "para qué sirve", 
                             "cuál es mejor", "diferencia", "megapíxeles", "ram", "rom", "procesador", "ghz", "batería mah", 
                             "oled", "amoled", "ips", "hercios", "hertz", "refresco", "cámara", "zoom", "carga rápida", "5g", "nfc", "ip68"};
        boolean esTecnica = false;
        for (String t : tecnicas) {
            if (msgLower.contains(t)) {
                esTecnica = true;
                break;
            }
        }

        if (esTecnica) {
            String contexto = SYSTEM_PROMPT_GENERAL + "\n\nPregunta del usuario: " + mensajeUsuario;
            String respuesta = aiService.generateText(contexto);
            historial.add(new MensajeIA("bot", respuesta));
            return respuesta;
        }

        // ===================================================================
        // 4. AHORA SÍ: BÚSQUEDA DE PRODUCTO EN MONGODB
        // ===================================================================
        List<Producto> productos = consultaMongoService.buscarProductosPorPalabras(mensajeUsuario);

        boolean esCantidad = msgLower.matches(".*\\b(cuántos|cuántas|cuantos|cuantas|varios|muchos)\\b.*");
        boolean esMasBarato = msgLower.contains("más barato") || msgLower.contains("mas barato") || (msgLower.contains("barato") && (msgLower.contains("cuál") || msgLower.contains("cual")));
        boolean esPrecio = msgLower.matches(".*\\b(precio|cuesta|cuánto|cuanto|vale)\\b.*");
        boolean esDisponibilidad = msgLower.matches(".*\\b(tienen|tenés|hay|disponible|stock)\\b.*");
        boolean esOpinion = msgLower.matches(".*\\b(bueno|vale la pena|recomienda|conviene|qué tal|opinión|mejor)\\b.*");

        String respuesta;

        if (productos.isEmpty()) {
            respuesta = "Lo siento, no encontré productos que coincidan exactamente con tu búsqueda.\n\n" +
                       "¿Me das más detalles o quieres que te recomiende algo popular en celulares, consolas o portátiles? 😊";
        }
        else if (esMasBarato) {
            Producto barato = productos.stream().min(Comparator.comparingDouble(Producto::getPrecio)).get();
            respuesta = "El más económico disponible es:\n\n" +
                       barato.getNombre() + "\nPrecio: $" + String.format("%,.0f", barato.getPrecio()) + "\n\n¿Te interesa o buscas algo con más características?";
        }
        else if (esPrecio) {
            respuesta = generarListaPrecios(productos);
        }
        else if (esCantidad && !esPrecio && !esOpinion) {
            respuesta = "Contamos con " + productos.size() + " opciones diferentes.";
        }
        else if (esDisponibilidad) {
            respuesta = generarRespuestaDisponibilidad(productos);
        }
        else if (esOpinion || productos.size() <= 6) {
            respuesta = usarIAConProductosReales(mensajeUsuario, historial, productos);
        }
        else {
            respuesta = generarRespuestaGeneral(productos);
        }

        historial.add(new MensajeIA("bot", respuesta));
        return respuesta;
    }

    // ======================= MÉTODOS DE RESPUESTA =======================
    private String generarListaPrecios(List<Producto> productos) {
        StringBuilder sb = new StringBuilder("Estos son algunos precios encontrados:\n\n");
        productos.sort(Comparator.comparingDouble(Producto::getPrecio));
        for (int i = 0; i < Math.min(5, productos.size()); i++) {
            Producto p = productos.get(i);
            sb.append((i+1)).append(". ").append(p.getNombre())
              .append(" → $").append(String.format("%,.0f", p.getPrecio())).append("\n");
        }
        if (productos.size() > 5) sb.append("\n... y ").append(productos.size()-5).append(" más.");
        sb.append("\n\n¿Quieres el más barato o filtramos?");
        return sb.toString();
    }

    private String generarRespuestaDisponibilidad(List<Producto> productos) {
        if (productos.size() == 1) {
            Producto p = productos.get(0);
            return "¡Sí, lo tenemos!\n\n" + p.getNombre() + "\nPrecio: $" + String.format("%,.0f", p.getPrecio());
        } else {
            double min = productos.stream().min(Comparator.comparingDouble(Producto::getPrecio)).get().getPrecio();
            return "Sí, tenemos " + productos.size() + " opciones.\nEl más barato sale en $" + String.format("%,.0f", min) +
                   ".\n\n¿Quieres la lista completa o te paso el más económico directo?";
        }
    }

    private String generarRespuestaGeneral(List<Producto> productos) {
        StringBuilder sb = new StringBuilder("Encontré " + productos.size() + " productos:\n\n");
        productos.sort(Comparator.comparingDouble(Producto::getPrecio));
        int lim = Math.min(4, productos.size());
        for (int i = 0; i < lim; i++) {
            Producto p = productos.get(i);
            sb.append("• ").append(p.getNombre()).append("\n  Precio: $").append(String.format("%,.0f", p.getPrecio())).append("\n\n");
        }
        if (productos.size() > 4) sb.append("... y ").append(productos.size()-4).append(" más.\n\n");
        sb.append("¿Cuál te interesa? Puedo comparar o mostrarte el más barato.");
        return sb.toString();
    }

    private String usarIAConProductosReales(String mensajeUsuario, List<MensajeIA> historial, List<Producto> productos) {
        StringBuilder ctx = new StringBuilder(SYSTEM_PROMPT_PRODUCTOS + "\n\nPRODUCTOS DISPONIBLES:\n");
        for (Producto p : productos) {
            ctx.append("• ").append(p.getNombre())
               .append(" | Precio: $").append(String.format("%,.0f", p.getPrecio()))
               .append(" | Marca: ").append(p.getMarca() != null ? p.getMarca() : "Sin marca").append("\n");
        }
        ctx.append("\nConversación:\n");
        for (MensajeIA m : historial) ctx.append(m.getRol()).append(": ").append(m.getTexto()).append("\n");
        return aiService.generateText(ctx.toString());
    }
}
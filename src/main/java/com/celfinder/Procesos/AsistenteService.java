package com.celfinder.Procesos;

import com.celfinder.Model.ChatThread;
import com.celfinder.Model.MensajeIA;
import com.celfinder.Model.Usuario;
import com.celfinder.Repository.ChatThreadRepository;
import com.celfinder.mcp.model.McpToolRequest;
import com.celfinder.mcp.model.McpToolResponse;
import com.celfinder.mcp.service.McpDispatcherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio del Asistente IA (Gorge Droyd) con soporte para hilos de
 * conversación persistentes.
 */
@Service
public class AsistenteService {

    private final AIService aiService;
    private final McpDispatcherService mcpDispatcherService;
    private final ChatThreadRepository chatThreadRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AsistenteService(AIService aiService,
            McpDispatcherService mcpDispatcherService,
            ChatThreadRepository chatThreadRepository) {
        this.aiService = aiService;
        this.mcpDispatcherService = mcpDispatcherService;
        this.chatThreadRepository = chatThreadRepository;
    }

    /**
     * Obtiene todos los hilos de conversación de un usuario.
     */
    public List<ChatThread> obtenerMisChats(String usuarioId) {
        return chatThreadRepository.findByUsuarioIdOrderByFechaActualizacionDesc(usuarioId);
    }

    /**
     * Inicia una conversación nueva o recupera una existente.
     */
    public ChatThread obtenerConversacion(String threadId, String usuarioId) {
        if (threadId != null && !threadId.isBlank()) {
            return chatThreadRepository.findById(threadId).orElse(new ChatThread(usuarioId, "Nuevo Chat"));
        }
        return new ChatThread(usuarioId, "Nuevo Chat");
    }

    /**
     * Procesa un mensaje dentro de un hilo específico.
     */
    public String procesarMensaje(String mensajeUsuario, String threadId, String usuarioId) {
        ChatThread thread = obtenerConversacion(threadId, usuarioId);
        List<MensajeIA> historial = thread.getMensajes();
        historial.add(new MensajeIA("user", mensajeUsuario));

        String msgLower = mensajeUsuario.toLowerCase().trim();

        // Generar título automático si es el inicio
        if (historial.size() == 1) {
            try {
                String promptTitulo = "Genera un título de máximo 4 palabras para: " + mensajeUsuario;
                String titulo = aiService.generateText(promptTitulo).replaceAll("\"", "").trim();
                thread.setTitulo(titulo.isEmpty() ? "Consulta Celfinder" : titulo);
            } catch (Exception e) {
                thread.setTitulo("Nueva Consulta");
            }
        }

        // --- DETECTAR ROL Y NOMBRE ---
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        String nombreReal = "Usuario";
        boolean esAdmin = false;

        if (auth != null && auth.getPrincipal() instanceof Usuario) {
            Usuario u = (Usuario) auth.getPrincipal();
            nombreReal = u.getNombreUsuario();
            esAdmin = u.getRoles().contains("ROLE_ADMIN");
        }

        // --- LÓGICA MCP ---
        String toolName = "buscarProductos";
        Map<String, String> params = new HashMap<>();

        if (esAdmin && (msgLower.contains("estadística") || msgLower.contains("estadistica"))) {
            toolName = "estadisticasVentas";
            params.put("tipo", "general");
        } else if (esAdmin && (msgLower.contains("aprobar") || msgLower.contains("rechazar"))) {
            toolName = "gestionarSolicitud";
            params.put("accion", msgLower.contains("aprobar") ? "aprobar" : "rechazar");
            String idExtraido = msgLower.replaceAll("[^0-9a-fA-F]", " ").trim().split("\\s+")[0];
            if (idExtraido.length() > 10)
                params.put("solicitudId", idExtraido);
        } else if (msgLower.contains("categorías") || msgLower.contains("categorias")) {
            toolName = "listarCategorias";
        } else {
            toolName = "buscarProductos";
            params.put("query", mensajeUsuario);
            params.put("limit", "5");
        }

        McpToolResponse mcpResponse = mcpDispatcherService.dispatch(new McpToolRequest(toolName, params));

        String datosMcp = "";
        try {
            datosMcp = mapper.writeValueAsString(mcpResponse.getData());
        } catch (Exception e) {
            datosMcp = "{}";
        }

        // --- GENERAR RESPUESTA ---
        String rolTexto = esAdmin ? "ADMINISTRADOR (Acceso Total a Gestión)" : "CLIENTE (Consultas y Compras)";

        String promptMcp = String.format(
                """
                        # IDENTIDAD
                        Eres Gorge Droyd, el asistente inteligente de CelFinder.

                        # CONTEXTO ACTUAL
                        - Hablas con: %s
                        - Su Rol: %s
                        - Historial: %d mensajes previos.

                        # DATOS DE LA BASE DE DATOS (MCP)
                        %s

                        # INSTRUCCIONES DE COMPORTAMIENTO
                        1. RECONOCIMIENTO: Debes actuar según el rol. Si es ADMIN, ayúdale a gestionar. Si es CLIENTE, ayúdale a comprar.
                        2. NO HALLUCINAR: Nunca digas que no tienes acceso a roles. Se te ha informado explícitamente que hablas con %s (%s).
                        3. HERRAMIENTAS: Tienes acceso a herramientas MCP para consultar productos y gestionar ventas. Úsalas para dar respuestas reales.
                        4. ESTILO: Responde de forma concisa pero amable. Usa emojis de forma moderada.

                        # PREGUNTA DEL USUARIO
                        "%s"
                        """,
                nombreReal, rolTexto, historial.size(), datosMcp, nombreReal, rolTexto, mensajeUsuario);

        String respuestaIA = aiService.generateText(promptMcp);

        // --- GUARDAR ---
        historial.add(new MensajeIA("bot", respuestaIA));
        thread.setFechaActualizacion(java.time.LocalDateTime.now());
        chatThreadRepository.save(thread);

        return respuestaIA;
    }
}
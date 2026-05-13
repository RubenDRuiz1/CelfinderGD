package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un hilo de conversación único (como en ChatGPT).
 * Permite tener múltiples chats guardados por usuario.
 */
@Document(collection = "chat_threads")
public class ChatThread {

    @Id
    private String id;
    private String usuarioId;
    private String titulo;
    private List<MensajeIA> mensajes = new ArrayList<>();
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    public ChatThread() {}

    public ChatThread(String usuarioId, String titulo) {
        this.usuarioId = usuarioId;
        this.titulo = titulo;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public List<MensajeIA> getMensajes() { return mensajes; }
    public void setMensajes(List<MensajeIA> mensajes) { this.mensajes = mensajes; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}

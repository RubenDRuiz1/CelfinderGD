package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_mensajes")
public class ChatMensaje {

    @Id
    private String id;
    private String usuarioId;
    private String contenido;
    private String rol; // "user" o "assistant"
    private LocalDateTime timestamp;

    public ChatMensaje() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMensaje(String usuarioId, String contenido, String rol) {
        this.usuarioId = usuarioId;
        this.contenido = contenido;
        this.rol = rol;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

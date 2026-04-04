package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "chats_pedidos")
public class ChatPedido {

    @Id
    private String id;
    private String solicitudId;
    private String compradorId;
    private String vendedorId;
    private String productoId;
    private List<MensajeChat> mensajes;

    public ChatPedido() {
        this.id = UUID.randomUUID().toString();
        this.mensajes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getCompradorId() {
        return compradorId;
    }

    public void setCompradorId(String compradorId) {
        this.compradorId = compradorId;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(String vendedorId) {
        this.vendedorId = vendedorId;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public List<MensajeChat> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<MensajeChat> mensajes) {
        this.mensajes = mensajes;
    }

    public void agregarMensaje(String remitenteId, String contenido) {
        this.mensajes.add(new MensajeChat(remitenteId, contenido));
    }

    public static class MensajeChat {
        private String remitenteId;
        private String contenido;
        private LocalDateTime fecha;

        public MensajeChat() {
            this.fecha = LocalDateTime.now();
        }

        public MensajeChat(String remitenteId, String contenido) {
            this.remitenteId = remitenteId;
            this.contenido = contenido;
            this.fecha = LocalDateTime.now();
        }

        public String getRemitenteId() {
            return remitenteId;
        }

        public void setRemitenteId(String remitenteId) {
            this.remitenteId = remitenteId;
        }

        public String getContenido() {
            return contenido;
        }

        public void setContenido(String contenido) {
            this.contenido = contenido;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }

        public void setFecha(LocalDateTime fecha) {
            this.fecha = fecha;
        }
    }
}

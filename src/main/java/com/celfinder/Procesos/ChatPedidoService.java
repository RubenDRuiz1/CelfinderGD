package com.celfinder.Procesos;

import com.celfinder.Model.ChatPedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatPedidoService {

    private final MongoTemplate mongoTemplate;

    public ChatPedidoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public ChatPedido obtenerOCrearChat(String solicitudId, String compradorId, String vendedorId, String productoId) {
        Query query = new Query(Criteria.where("solicitudId").is(solicitudId));
        ChatPedido chat = mongoTemplate.findOne(query, ChatPedido.class, "chats_pedidos");

        if (chat == null) {
            chat = new ChatPedido();
            chat.setSolicitudId(solicitudId);
            chat.setCompradorId(compradorId);
            chat.setVendedorId(vendedorId);
            chat.setProductoId(productoId);
            mongoTemplate.save(chat, "chats_pedidos");
        }
        return chat;
    }

    public ChatPedido obtenerChatPorSolicitud(String solicitudId) {
        Query query = new Query(Criteria.where("solicitudId").is(solicitudId));
        return mongoTemplate.findOne(query, ChatPedido.class, "chats_pedidos");
    }

    public ChatPedido obtenerChatPorId(String chatId) {
        return mongoTemplate.findById(chatId, ChatPedido.class, "chats_pedidos");
    }

    public void agregarMensaje(String chatId, String remitenteId, String contenido) {
        ChatPedido chat = obtenerChatPorId(chatId);
        if (chat != null) {
            chat.agregarMensaje(remitenteId, contenido);
            mongoTemplate.save(chat, "chats_pedidos");
        }
    }

    public List<ChatPedido> obtenerChatsUsuario(String usuarioId) {
        Query query = new Query(new Criteria().orOperator(
            Criteria.where("compradorId").is(usuarioId),
            Criteria.where("vendedorId").is(usuarioId)
        ));
        return mongoTemplate.find(query, ChatPedido.class, "chats_pedidos");
    }
}

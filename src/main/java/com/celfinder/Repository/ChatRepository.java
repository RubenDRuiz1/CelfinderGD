package com.celfinder.Repository;

import com.celfinder.Model.ChatMensaje;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatRepository extends MongoRepository<ChatMensaje, String> {
    List<ChatMensaje> findByUsuarioIdOrderByTimestampAsc(String usuarioId);
}

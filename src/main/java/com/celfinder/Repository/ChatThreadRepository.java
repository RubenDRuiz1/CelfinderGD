package com.celfinder.Repository;

import com.celfinder.Model.ChatThread;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatThreadRepository extends MongoRepository<ChatThread, String> {
    List<ChatThread> findByUsuarioIdOrderByFechaActualizacionDesc(String usuarioId);
}

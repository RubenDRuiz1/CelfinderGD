package com.celfinder.Procesos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class Inventario {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public Inventario(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // Ya no necesitamos métodos aquí porque el controlador lo hace directamente
    // Pero lo dejamos por si otros controladores lo usan
}
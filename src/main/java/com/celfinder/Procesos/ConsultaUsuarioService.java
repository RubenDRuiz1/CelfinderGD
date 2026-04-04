package com.celfinder.Procesos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.celfinder.Model.Usuario;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service
public class ConsultaUsuarioService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Verifica si un vendedorId existe y tiene rol 'VENDEDOR'
     */
    public boolean existeVendedor(String vendedorId) {
        if (vendedorId == null || vendedorId.isEmpty()) return false;

        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(vendedorId)
                                  .and("roles").in("VENDEDOR"));
        return mongoTemplate.exists(query, Usuario.class);
    }
}

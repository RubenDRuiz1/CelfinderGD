package com.celfinder.Repository;

import com.celfinder.Model.Combo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComboRepository extends MongoRepository<Combo, String> {
}

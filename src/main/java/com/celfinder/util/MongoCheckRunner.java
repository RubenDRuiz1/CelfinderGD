package com.celfinder.util;

import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MongoCheckRunner implements CommandLineRunner {

    private final UsuarioService usuarioService;

    public MongoCheckRunner(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- CHECKING MONGODB USERS ---");
        // This is a placeholder for actual check if needed
    }
}

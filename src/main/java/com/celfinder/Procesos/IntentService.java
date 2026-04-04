package com.celfinder.Procesos;

import org.springframework.stereotype.Service;

@Service
public class IntentService {

    public enum Intento {
        CONSULTA_DISPONIBILIDAD,
        CONSULTA_POR_VENDEDOR,
        RECOMENDACION,
        PRECIO_BUENO_MALO,
        NINGUNO
    }

    public Intento detectarIntento(String mensaje) {
        mensaje = mensaje.toLowerCase();

        if (mensaje.contains("hay") || mensaje.contains("disponible") || mensaje.contains("tienes"))
            return Intento.CONSULTA_DISPONIBILIDAD;

        if (mensaje.contains("vendedor") || mensaje.contains("vende"))
            return Intento.CONSULTA_POR_VENDEDOR;

        if (mensaje.contains("es bueno") || mensaje.contains("vale la pena"))
            return Intento.RECOMENDACION;

        if (mensaje.contains("caro") || mensaje.contains("barato") || mensaje.contains("precio"))
            return Intento.PRECIO_BUENO_MALO;

        return Intento.NINGUNO;
    }
}

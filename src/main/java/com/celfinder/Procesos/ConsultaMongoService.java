package com.celfinder.Procesos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.celfinder.Model.Producto;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConsultaMongoService {

    @Autowired
    private MongoTemplate mongoTemplate;

    // Cache de marcas reales (se actualiza cada 30 minutos automáticamente)
    private Set<String> marcasCache = null;
    private long ultimaActualizacionCache = 0;
    private static final long CACHE_TIEMPO = 1000 * 60 * 30; // 30 minutos

    public List<Producto> buscarProductosPorPalabras(String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Limpieza agresiva de palabras vacías para que no rompa nunca
        String textoLimpio = mensaje.toLowerCase()
                .replaceAll("(?i)\\b(tienen|tenés|tenes|hay|disponible|stock|consolas?|conzolaz|producto|productos|otros|otro|para|con|de|del|la|los|las|un|una|unos|unas|me|das|muestra|muestrame|busca|buscame|quiero|necesito|vendes|vende|mostrame|mostrarme|ver|veo)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (textoLimpio.isEmpty()) {
            return new ArrayList<>();
        }

        // Intentamos detectar una marca real que exista en la base
        String marcaDetectada = detectarMarcaReal(textoLimpio);

        if (marcaDetectada != null) {
            Query query = new Query(Criteria.where("marca").regex("^" + Pattern.quote(marcaDetectada) + "$", "i"));
            return limitar(mongoTemplate.find(query, Producto.class));
        }

        // Búsqueda normal por palabras clave
        String[] palabras = textoLimpio.split("\\s+");
        List<Criteria> criterios = new ArrayList<>();

        for (String palabra : palabras) {
            if (palabra.length() < 3) continue;
            String regex = Pattern.quote(palabra);
            criterios.add(Criteria.where("marca").regex(regex, "i"));
            criterios.add(Criteria.where("nombre").regex(regex, "i"));
        }

        if (criterios.isEmpty()) return new ArrayList<>();

        Query query = new Query(new Criteria().orOperator(criterios.toArray(new Criteria[0])));
        return limitar(mongoTemplate.find(query, Producto.class));
    }

    private String detectarMarcaReal(String texto) {
        Map<String, String> alias = new HashMap<>();
        alias.put("moto", "motorola");
        alias.put("motoro", "motorola");
        alias.put("galaxy", "samsung");
        alias.put("samsum", "samsung");
        alias.put("iphone", "apple");
        alias.put("macbook", "apple");
        alias.put("airpods", "apple");
        alias.put("ipad", "apple");
        alias.put("redmi", "xiaomi");
        alias.put("poco", "xiaomi");
        alias.put("switch", "nintendo");
        alias.put("playstation", "sony");
        alias.put("ps5", "sony");
        alias.put("ps4", "sony");
        alias.put("xbox", "microsoft");
        alias.put("dji", "dji");

        // 1. Chequeamos alias conocidos
        for (Map.Entry<String, String> entry : alias.entrySet()) {
            if (texto.contains(entry.getKey()) && marcaExiste(entry.getValue())) {
                return entry.getValue();
            }
        }

        // 2. Probamos cada palabra como posible marca
        for (String palabra : texto.split("\\s+")) {
            if (palabra.length() < 3) continue;

            if (marcaExiste(palabra)) return palabra;

            String capitalizada = palabra.substring(0,1).toUpperCase() + palabra.substring(1);
            if (marcaExiste(capitalizada)) return capitalizada;
        }

        return null;
    }

    private boolean marcaExiste(String marca) {
        long ahora = System.currentTimeMillis();
        if (marcasCache == null || (ahora - ultimaActualizacionCache) > CACHE_TIEMPO) {
            marcasCache = mongoTemplate.findDistinct(new Query(), "marca", Producto.class, String.class)
                    .stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            ultimaActualizacionCache = ahora;
        }
        return marcasCache.contains(marca.toLowerCase());
    }

    private List<Producto> limitar(List<Producto> lista) {
        return lista.size() > 50 ? lista.subList(0, 50) : lista;
    }

    public boolean hayProductos(String mensaje) {
        return !buscarProductosPorPalabras(mensaje).isEmpty();
    }
}
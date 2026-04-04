package com.celfinder.Procesos;

import com.celfinder.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecomendacionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Producto> recomendarParaUsuario(String usuarioId) {
        Set<String> vistos = getVistosPorUsuario(usuarioId);
        Set<String> enCarrito = getEnCarritoPorUsuario(usuarioId);
        Set<String> descartar = new HashSet<>();
        descartar.addAll(vistos);
        descartar.addAll(enCarrito);

        Map<String, Double> puntuaciones = new HashMap<>();

        // 1. Similitud con otros usuarios
        Map<String, Double> similitudes = calcularSimilitudes(usuarioId);
        for (Map.Entry<String, Double> entry : similitudes.entrySet()) {
            String otroUsuario = entry.getKey();
            double sim = entry.getValue();

            Set<String> productosVistosPorOtro = getVistosPorUsuario(otroUsuario);
            Set<String> carritosOtro = getEnCarritoPorUsuario(otroUsuario);

            for (String pid : productosVistosPorOtro) {
                if (!descartar.contains(pid)) {
                    puntuaciones.merge(pid, sim * 1.0, Double::sum);
                }
            }
            for (String pid : carritosOtro) {
                if (!descartar.contains(pid)) {
                    puntuaciones.merge(pid, sim * 3.0, Double::sum);
                }
            }
        }

        // 2. Similitud de contenido con expansión
        Map<String, Double> preferencias = expandirPreferencias(getPreferenciasUsuario(usuarioId));
        List<Producto> productos = mongoTemplate.findAll(Producto.class);

        for (Producto p : productos) {
            if (descartar.contains(p.getId())) continue;

            double scoreContenido = 0.0;

            if (preferencias.containsKey("cat:" + p.getCategoria())) {
                scoreContenido += preferencias.get("cat:" + p.getCategoria());
            }

            if (preferencias.containsKey("marca:" + p.getMarca())) {
                scoreContenido += preferencias.get("marca:" + p.getMarca());
            }

            double precioPromedio = preferencias.getOrDefault("precio_promedio", 0.0);
            double diff = Math.abs(p.getPrecio() - precioPromedio);
            scoreContenido += Math.max(0, 1 - diff / 1000000);

            puntuaciones.merge(p.getId(), scoreContenido * 0.5, Double::sum);
        }

        // 3. Ordenar y diversificar
        List<Map.Entry<String, Double>> lista = new ArrayList<>(puntuaciones.entrySet());
        lista.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        Set<String> nombresYaAgregados = new HashSet<>();
        Set<String> marcasYaAgregadas = new HashSet<>();
        List<Producto> resultado = new ArrayList<>();

        for (Map.Entry<String, Double> e : lista) {
            Producto p = mongoTemplate.findById(e.getKey(), Producto.class);
            if (p == null) continue;

            String nombreNormalizado = p.getNombre().toLowerCase().replaceAll("\\s+", " ").trim();
            String marca = p.getMarca().toLowerCase();

            // Diversificación: máx 3 por nombre, máx 4 por marca
            long countNombre = resultado.stream()
                    .filter(prod -> prod.getNombre().toLowerCase().replaceAll("\\s+", " ").trim().equals(nombreNormalizado))
                    .count();
            long countMarca = resultado.stream()
                    .filter(prod -> prod.getMarca().toLowerCase().equals(marca))
                    .count();

            if (countNombre < 3 && countMarca < 4) {
                nombresYaAgregados.add(nombreNormalizado);
                marcasYaAgregadas.add(marca);
                resultado.add(p);
                if (resultado.size() >= 15) break;
            }
        }

        return resultado;
    }

    // ---------- EXPANSIÓN DE PREFERENCIAS ----------

    private Map<String, Double> expandirPreferencias(Map<String, Double> prefs) {
        Map<String, Double> expandido = new HashMap<>(prefs);

        double appleScore = prefs.getOrDefault("marca:Apple", 0.0) + prefs.getOrDefault("marca:apple", 0.0);
        if (appleScore > 5.0) {
            expandido.merge("categoria:tablet", appleScore * 0.6, Double::sum);
            expandido.merge("categoria:auriculares", appleScore * 0.5, Double::sum);
            expandido.merge("categoria:smartwatch", appleScore * 0.4, Double::sum);
            expandido.merge("marca:Samsung", appleScore * 0.3, Double::sum);
            expandido.merge("marca:Xiaomi", appleScore * 0.2, Double::sum);
            expandido.merge("marca:Motorola", appleScore * 0.2, Double::sum);
        }

        return expandido;
    }

    // ---------- HELPERS ----------

    private Set<String> getVistosPorUsuario(String usuarioId) {
        return mongoTemplate.find(
                new Query(Criteria.where("usuarioId").is(usuarioId)), Visualizacion.class)
                .stream().map(Visualizacion::getProductoId).collect(Collectors.toSet());
    }

    private Set<String> getEnCarritoPorUsuario(String usuarioId) {
        Set<String> productos = new HashSet<>();
        List<Carrito> carritos = mongoTemplate.find(
                new Query(Criteria.where("usuarioId").is(usuarioId)), Carrito.class);
        for (Carrito c : carritos) {
            if (c.getProductoIds() != null) {
                productos.addAll(c.getProductoIds());
            }
        }
        return productos;
    }

    private Map<String, Double> calcularSimilitudes(String usuarioId) {
        Map<String, Double> similitudes = new HashMap<>();
        Set<String> productosUsuario = getVistosPorUsuario(usuarioId);
        productosUsuario.addAll(getEnCarritoPorUsuario(usuarioId));

        Map<String, Set<String>> usuariosProductos = cargarInteraccionesDeTodos();

        for (Map.Entry<String, Set<String>> entry : usuariosProductos.entrySet()) {
            String otro = entry.getKey();
            if (otro.equals(usuarioId)) continue;

            Set<String> interseccion = new HashSet<>(productosUsuario);
            interseccion.retainAll(entry.getValue());

            double sim = interseccion.size() /
                    (Math.sqrt(productosUsuario.size()) * Math.sqrt(entry.getValue().size()) + 1e-6);
            if (sim > 0.1) similitudes.put(otro, sim);
        }

        return similitudes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Set<String>> cargarInteraccionesDeTodos() {
        Map<String, Set<String>> map = new HashMap<>();

        for (Visualizacion v : mongoTemplate.findAll(Visualizacion.class)) {
            map.computeIfAbsent(v.getUsuarioId(), k -> new HashSet<>()).add(v.getProductoId());
        }

        for (Carrito c : mongoTemplate.findAll(Carrito.class)) {
            if (c.getProductoIds() != null) {
                map.computeIfAbsent(c.getUsuarioId(), k -> new HashSet<>()).addAll(c.getProductoIds());
            }
        }

        return map;
    }

    private Map<String, Double> getPreferenciasUsuario(String usuarioId) {
        Map<String, Double> prefs = new HashMap<>();
        List<Visualizacion> vistas = mongoTemplate.find(
                new Query(Criteria.where("usuarioId").is(usuarioId)), Visualizacion.class);
        List<Carrito> carritos = mongoTemplate.find(
                new Query(Criteria.where("usuarioId").is(usuarioId)), Carrito.class);

        double sumaPrecios = 0;
        int contador = 0;

        for (Visualizacion v : vistas) {
            Producto p = mongoTemplate.findById(v.getProductoId(), Producto.class);
            if (p == null) continue;
            prefs.merge("cat:" + p.getCategoria(), 1.0, Double::sum);
            prefs.merge("marca:" + p.getMarca(), 1.0, Double::sum);
            sumaPrecios += p.getPrecio();
            contador++;
        }

        for (Carrito c : carritos) {
            if (c.getProductoIds() != null) {
                for (String pid : c.getProductoIds()) {
                    Producto p = mongoTemplate.findById(pid, Producto.class);
                    if (p == null) continue;
                    prefs.merge("cat:" + p.getCategoria(), 3.0, Double::sum);
                    prefs.merge("marca:" + p.getMarca(), 3.0, Double::sum);
                    sumaPrecios += p.getPrecio();
                    contador++;
                }
            }
        }

        if (contador > 0) {
            prefs.put("precio_promedio", sumaPrecios / contador);
        }

        return prefs;
    }
}
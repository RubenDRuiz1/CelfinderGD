package com.celfinder.Procesos;

import com.celfinder.Model.Usuario;
import com.celfinder.Model.Visualizacion;
import com.celfinder.Model.HistorialCarrito;
import com.celfinder.Model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimplexService {

    private static final Logger log = LoggerFactory.getLogger(SimplexService.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductQueryService productQueryService;

    @Value("${simplex.python-script-path:simplex_celfinder.py}")
    private String pythonScriptPath;

    @Value("${simplex.cost-margin:0.70}")
    private double costMargin;

    @Value("${simplex.tracking-days:30}")
    private int trackingDays;

    public String generarDashboardSimplex(Usuario usuario) {
        try {
            double presupuesto = usuario.getPresupuestoMensual() != null ? usuario.getPresupuestoMensual() : 1000000.0;

            if (presupuesto <= 0) {
                return errorHtml("El presupuesto mensual del usuario debe ser mayor a 0.");
            }

            // Validar intereses de categorías
            Map<String, Double> intereses = usuario.getInteresesCategorias();
            if (intereses == null || intereses.size() < 2) {
                return errorHtml("El usuario necesita al menos 2 categorías de interés registradas para el motor Simplex. Registradas: " + (intereses != null ? intereses.size() : 0));
            }

            // Filtrar tracking por fecha (últimos N días)
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(trackingDays);
            Criteria fechaCriteria = Criteria.where("usuarioId").is(usuario.getId()).and("fecha").gte(fechaLimite);

            List<Visualizacion> visualizaciones = mongoTemplate.find(
                    new Query(fechaCriteria), Visualizacion.class);
            List<HistorialCarrito> carritos = mongoTemplate.find(
                    new Query(fechaCriteria), HistorialCarrito.class);

            int totalVistas = visualizaciones.size();
            int totalCarts = carritos.size();

            // Mapear productos vistos por categoría
            Map<String, List<Producto>> productosVistosPorCategoria = new HashMap<>();
            for (Visualizacion v : visualizaciones) {
                Producto p = productQueryService.obtenerProductoPorId(v.getProductoId());
                if (p != null && p.getCategoria() != null) {
                    productosVistosPorCategoria.computeIfAbsent(p.getCategoria(), k -> new ArrayList<>()).add(p);
                }
            }

            // Construir categorías con precios promediados del tracking real
            List<Map<String, Object>> categoriasData = new ArrayList<>();
            double fallbackPrice = presupuesto * 0.8;

            for (Map.Entry<String, Double> entry : intereses.entrySet()) {
                String catName = entry.getKey();
                double interes = Math.max(0.01, Math.min(1.0, entry.getValue() != null ? entry.getValue() : 0.5));
                double precioCalc = fallbackPrice;
                double costoCalc = fallbackPrice * costMargin;

                List<Producto> vistos = productosVistosPorCategoria.get(catName);
                if (vistos != null && !vistos.isEmpty()) {
                    double sumPrecio = 0;
                    for (Producto p : vistos) {
                        sumPrecio += p.getPrecio();
                    }
                    precioCalc = sumPrecio / vistos.size();
                    costoCalc = precioCalc * costMargin;
                }

                Map<String, Object> catMap = new HashMap<>();
                catMap.put("nombre", catName);
                catMap.put("interes", interes);
                catMap.put("precio", Math.max(precioCalc, 10000.0));
                catMap.put("costo", Math.max(costoCalc, 5000.0));
                categoriasData.add(catMap);
                fallbackPrice = Math.max(100000.0, fallbackPrice * 0.8);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("categorias", categoriasData);
            Map<String, Object> trackingData = new HashMap<>();
            trackingData.put("totalVisualizaciones", totalVistas);
            trackingData.put("totalCarritos", totalCarts);
            payload.put("trackingData", trackingData);

            ObjectMapper mapper = new ObjectMapper();
            String jsonCategoriasBase64 = Base64.getEncoder().encodeToString(
                    mapper.writeValueAsString(payload).getBytes("UTF-8"));

            // Resolver ruta del script Python
            String scriptPath = pythonScriptPath;
            File scriptFile = new File(scriptPath);
            if (!scriptFile.isAbsolute()) {
                scriptFile = new File(System.getProperty("user.dir"), scriptPath);
            }
            if (!scriptFile.exists()) {
                log.error("Script simplex no encontrado: {}", scriptFile.getAbsolutePath());
                return errorHtml("Script simplex_celfinder.py no encontrado en: " + scriptFile.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder("py", scriptFile.getAbsolutePath(),
                    String.valueOf(presupuesto), jsonCategoriasBase64);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            String resultHtml = reader.lines().collect(Collectors.joining("\n"));
            process.waitFor();

            if (process.exitValue() != 0) {
                log.warn("Script simplex terminó con código {}", process.exitValue());
            }

            if (resultHtml.isBlank()) {
                return errorHtml("El motor Simplex no generó resultados. Verifica que Python 3 y PuLP estén instalados.");
            }

            return resultHtml;

        } catch (Exception e) {
            log.error("Error al invocar motor Simplex", e);
            return errorHtml("Error al invocar motor Simplex: " + e.getMessage() +
                    ". Verifica que Python 3, PuLP y matplotlib estén instalados correctamente.");
        }
    }

    private String errorHtml(String mensaje) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>" +
                "<title>MathMatch Simplex - Error</title>" +
                "<style>body{font-family:'Segoe UI',sans-serif;background:#0f172a;color:#f8fafc;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0}" +
                ".card{background:#1e293b;border-radius:16px;padding:40px;max-width:600px;border:1px solid #334155;text-align:center}" +
                ".icon{font-size:48px;margin-bottom:20px}" +
                "h2{color:#f87171;margin-bottom:16px}" +
                "p{color:#94a3b8;line-height:1.6}" +
                ".btn{display:inline-block;margin-top:24px;padding:10px 24px;background:#334155;color:white;text-decoration:none;border-radius:8px}" +
                "</style></head><body>" +
                "<div class='card'>" +
                "<div class='icon'>&#9888;&#65039;</div>" +
                "<h2>Error en Motor Simplex</h2>" +
                "<p>" + mensaje + "</p>" +
                "<a href='/admin/sesiones-activas' class='btn'>&larr; Volver a Sesiones</a>" +
                "</div></body></html>";
    }
}

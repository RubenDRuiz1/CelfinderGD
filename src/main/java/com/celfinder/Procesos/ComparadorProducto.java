
package com.celfinder.Procesos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.celfinder.Model.Producto;

import java.util.ArrayList;
import java.util.List;

@Component
public class ComparadorProducto {

    private final MongoTemplate mongoTemplate;
    private final AIService aiService;
    private Producto producto1;
    private Producto producto2;
    private List<String> resultadosComparacion;

    @Autowired
    public ComparadorProducto(MongoTemplate mongoTemplate, AIService aiService) {
        this.mongoTemplate = mongoTemplate;
        this.aiService = aiService;
        this.resultadosComparacion = new ArrayList<>();
    }

    public List<String> compararConAI(Producto p1, Producto p2) {
        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Ambos productos deben ser válidos para la comparación.");
        }

        String prompt = crearPromptComparacion(p1, p2);

        List<String> results = new ArrayList<>();
        try {
            String aiResponse = aiService.generateText(prompt);

            String summary = "No disponible.";
            String detailed = "No disponible.";

            int summaryStart = aiResponse.indexOf("[[INICIO_RESUMEN_SIMPLE]]");
            int summaryEnd = aiResponse.indexOf("[[FIN_RESUMEN_SIMPLE]]");
            int detailedStart = aiResponse.indexOf("[[INICIO_EXPLICACION_DETALLADA]]");
            int detailedEnd = aiResponse.indexOf("[[FIN_EXPLICACION_DETALLADA]]");

            if (summaryStart != -1 && summaryEnd != -1) {
                summary = aiResponse.substring(summaryStart + "[[INICIO_RESUMEN_SIMPLE]]".length(), summaryEnd).trim();
            }
            if (detailedStart != -1 && detailedEnd != -1) {
                detailed = aiResponse.substring(detailedStart + "[[INICIO_EXPLICACION_DETALLADA]]".length(), detailedEnd).trim();
            } else if (summaryStart == -1 && detailedStart == -1) {
                summary = aiResponse.trim();
            }

            results.add(summary);
            results.add(detailed);

        } catch (Exception e) {
            System.err.println("Error al obtener comparación de AI: " + e.getMessage());
            e.printStackTrace();
            results.add("Error: No se pudo obtener respuesta de la IA.");
            results.add("No se pudo generar la explicación detallada debido a un error al comunicarse con la IA.");
        }
        return results;
    }

    private String crearPromptComparacion(Producto p1, Producto p2) {
        String prompt = "Compara los siguientes dos productos. " +
                        "Quiero la respuesta en dos secciones CLARAMENTE DELIMITADAS por marcadores específicos. " +
                        "No incluyas los títulos de las secciones ('Resumen Sencillo', 'Explicación Detallada') DENTRO del texto generado para esas secciones." +
                        "Usa Markdown para negritas (**) y listas (- ).\n\n" +
                        "[[INICIO_RESUMEN_SIMPLE]]\n" +
                        "**Resumen Sencillo:** Proporciona una explicación breve y fácil de entender, ideal para alguien sin conocimientos técnicos, indicando cuál es mejor en general y por qué. Enfócate en el valor general, puntos fuertes y débiles de cada uno para un usuario promedio. Utiliza un lenguaje natural y conversacional. No uses listas en este resumen.\n" +
                        "[[FIN_RESUMEN_SIMPLE]]\n\n" +
                        "[[INICIO_EXPLICACION_DETALLADA]]\n" +
                        "**Explicación Detallada:** Un análisis punto por punto de las características clave comparadas. Para cada característica, indica la **Comparación** (ej. 'Superior', 'Inferior', 'Similar', 'Depende del uso') y una **Explicación** concisa. Cada característica debe ser un ítem de lista Markdown. Si un dato no está especificado, indícalo como 'N/A'.\n" +
                        "Ejemplo de formato por característica:\n" +
                        "- **Característica:** [Comparación]\n" +
                        "  [Explicación concisa y técnica sobre por qué es superior/inferior/similar]\n\n" +
                        "Considera la fecha actual (29 de septiembre de 2025) para el contexto del mercado y menciona si alguna especificación está desactualizada para este año.\n\n" +
                        "--- DATOS PRODUCTOS ---\n\n" +
                        "**Producto 1:**\n" +
                        "  - Nombre: " + p1.getNombre() + "\n" +
                        "  - Marca: " + p1.getMarca() + "\n" +
                        "  - Categoría: " + p1.getCategoria() + "\n" +
                        "  - Precio: $" + String.format("%.2f", p1.getPrecio()) + "\n" +
                        "  - Estado: " + p1.getEstadoVenta() + "\n" +
                        "  - Descripción: " + p1.getDescripcion() + "\n\n" +
                        "**Producto 2:**\n" +
                        "  - Nombre: " + p2.getNombre() + "\n" +
                        "  - Marca: " + p2.getMarca() + "\n" +
                        "  - Categoría: " + p2.getCategoria() + "\n" +
                        "  - Precio: $" + String.format("%.2f", p2.getPrecio()) + "\n" +
                        "  - Estado: " + p2.getEstadoVenta() + "\n" +
                        "  - Descripción: " + p2.getDescripcion() + "\n" +
                        "[[FIN_EXPLICACION_DETALLADA]]";

        return prompt;
    }

    public List<Producto> obtenerProductos() {
        List<Producto> productos = new ArrayList<>();
        try {
            productos = mongoTemplate.findAll(Producto.class, "productos");
        } catch (Exception e) {
            System.err.println("Error al obtener productos: " + e.getMessage());
            e.printStackTrace();
        }
        return productos;
    }

    public void registrarProductos(Producto p1, Producto p2) {
        this.producto1 = p1;
        this.producto2 = p2;
        this.resultadosComparacion = compararConAI(p1, p2);
    }

    public List<String> getResultadosComparacion() {
        return resultadosComparacion;
    }
}

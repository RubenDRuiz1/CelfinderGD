package com.celfinder.Controller;

import com.celfinder.Procesos.AIService; // Asegúrate de importar la clase
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
// @RequestMapping("/gemini") // Podrías crear un nuevo RequestMapping
public class GeminiController { // O añadirlo a un controlador existente

    private final AIService geminiAIService;

    @Autowired
    public GeminiController(AIService geminiAIService) {
        this.geminiAIService = geminiAIService;
    }

    @GetMapping("/preguntar-gemini")
    public String mostrarFormularioGemini(Model model) {
        return "askGemini"; // Una vista Thymeleaf para el formulario
    }

    @PostMapping("/preguntar-gemini")
    public String enviarPreguntaAGemini(@RequestParam("pregunta") String pregunta, Model model) {
        try {
            String respuesta = geminiAIService.generateText(pregunta);
            model.addAttribute("pregunta", pregunta);
            model.addAttribute("respuesta", respuesta);
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al procesar tu pregunta: " + e.getMessage());
            e.printStackTrace();
        }
        return "askGemini"; // Vuelve a la misma vista con la respuesta
    }

    // Podrías crear métodos para que Gemini te ayude con tu código
    @PostMapping("/gemini/analizar-codigo")
    public String analizarCodigo(@RequestParam("codigo") String codigo, Model model) {
        String prompt = "Analiza el siguiente código Java y detecta posibles errores o mejoras: " + codigo;
        String analisis = geminiAIService.generateText(prompt);
        model.addAttribute("analisisCodigo", analisis);
        return "resultadoAnalisisCodigo"; // Vista para mostrar el análisis
    }

    @PostMapping("/gemini/explicar-codigo")
    public String explicarCodigo(@RequestParam("codigo") String codigo, Model model) {
        String prompt = "Explica el siguiente código Java paso a paso: " + codigo;
        String explicacion = geminiAIService.generateText(prompt);
        model.addAttribute("explicacionCodigo", explicacion);
        return "resultadoExplicacionCodigo"; // Vista para mostrar la explicación
    }
}
package com.celfinder.Controller;

import com.celfinder.Procesos.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DeepSeekController {

    private final AIService deepSeekAIService;

    @Autowired
    public DeepSeekController(AIService deepSeekAIService) {
        this.deepSeekAIService = deepSeekAIService;
    }

    @GetMapping("/preguntar-deepseek")
    public String mostrarFormularioDeepSeek(Model model) {
        return "askDeepSeek"; // Recuerda crear o renombrar esta vista
    }

    @PostMapping("/preguntar-deepseek")
    public String enviarPreguntaADeepSeek(@RequestParam("pregunta") String pregunta, Model model) {
        try {
            String respuesta = deepSeekAIService.generateText(pregunta);
            model.addAttribute("pregunta", pregunta);
            model.addAttribute("respuesta", respuesta);
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al procesar tu pregunta: " + e.getMessage());
            e.printStackTrace();
        }
        return "askDeepSeek";
    }

    @PostMapping("/deepseek/analizar-codigo")
    public String analizarCodigo(@RequestParam("codigo") String codigo, Model model) {
        String prompt = "Analiza el siguiente código Java y detecta posibles errores o mejoras: " + codigo;
        String analisis = deepSeekAIService.generateText(prompt);
        model.addAttribute("analisisCodigo", analisis);
        return "resultadoAnalisisCodigo";
    }

    @PostMapping("/deepseek/explicar-codigo")
    public String explicarCodigo(@RequestParam("codigo") String codigo, Model model) {
        String prompt = "Explica el siguiente código Java paso a paso: " + codigo;
        String explicacion = deepSeekAIService.generateText(prompt);
        model.addAttribute("explicacionCodigo", explicacion);
        return "resultadoExplicacionCodigo";
    }
}

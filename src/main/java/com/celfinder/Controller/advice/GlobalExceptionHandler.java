package com.celfinder.Controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manejador global de excepciones para todos los controladores
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Maneja excepciones de argumentos inválidos
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, 
                                                 RedirectAttributes redirectAttributes) {
        logger.warn("Argumento inválido: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/menu";
    }
    
    /**
     * Maneja excepciones de estado ilegal
     */
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, 
                                              RedirectAttributes redirectAttributes) {
        logger.warn("Estado ilegal: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/menu";
    }
    
    /**
     * Maneja excepciones generales no capturadas
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, 
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        logger.error("Error no esperado: ", ex);
        redirectAttributes.addFlashAttribute("error", "Ha ocurrido un error inesperado. Por favor, intenta nuevamente.");
        return "redirect:/menu";
    }
}

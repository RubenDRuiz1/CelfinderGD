package com.celfinder.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * Utilitario de imágenes.
 * Responsabilidad única: convertir MultipartFile → cadena Base64.
 */
@Component
public class ImagenUtil {

    /**
     * Convierte un MultipartFile a su representación Base64.
     *
     * @param imagen archivo recibido del formulario HTML
     * @return cadena Base64 lista para almacenar en MongoDB
     * @throws IOException si la lectura del archivo falla
     * @throws IllegalArgumentException si el archivo es nulo o vacío
     */
    public String convertirABase64(MultipartFile imagen) throws IOException {
        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("La imagen no puede estar vacía.");
        }
        return Base64.getEncoder().encodeToString(imagen.getBytes());
    }

    /**
     * Retorna true si el archivo es no-nulo y tiene contenido.
     */
    public boolean tieneContenido(MultipartFile archivo) {
        return archivo != null && !archivo.isEmpty();
    }
}

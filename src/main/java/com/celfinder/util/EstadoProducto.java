package com.celfinder.util;

/**
 * Constantes para los estados de productos en el sistema
 */
public final class EstadoProducto {
    
    private EstadoProducto() {
        // Constructor privado para evitar instanciación
    }
    
    public static final String DISPONIBLE = "disponible";
    public static final String VENDIDO = "vendido";
    public static final String RESERVADO = "reservado";
    public static final String AGOTADO = "agotado";
}

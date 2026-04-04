package com.celfinder.util;

/**
 * Constantes para los estados de solicitudes en el sistema
 */
public final class EstadoSolicitud {
    
    private EstadoSolicitud() {
        // Constructor privado para evitar instanciación
    }
    
    public static final String PENDIENTE = "pendiente";
    public static final String AUTORIZADA = "autorizada";
    public static final String RECHAZADA = "rechazada";
    public static final String APROBADA = "aprobada";
}

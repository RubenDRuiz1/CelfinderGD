package com.celfinder.util;

/**
 * Constantes de configuración del sistema
 */
public final class ConfiguracionApp {
    
    private ConfiguracionApp() {
        // Constructor privado para evitar instanciación
    }
    
    // Paginación
    public static final int TAMANIO_PAGINA_DEFECTO = 10;
    public static final int TAMANIO_PAGINA_MAXIMO = 100;
    
    // Validaciones
    public static final int LONGITUD_MINIMA_PASSWORD = 6;
    public static final int LONGITUD_TELEFONO = 10;
    
    // Estados de cuenta
    public static final String CUENTA_ACTIVA = "activa";
    public static final String CUENTA_INACTIVA = "inactiva";
    public static final String CUENTA_SUSPENDIDA = "suspendida";
}

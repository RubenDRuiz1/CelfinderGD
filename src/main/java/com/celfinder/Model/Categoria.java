package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo para categorías dinámicas con especificaciones personalizadas
 */
@Document(collection = "categorias")
public class Categoria implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    
    private String nombre; // Nombre de la categoría (ej: "Smartphones")
    
    private String grupo; // Grupo al que pertenece (ej: "Smartphones y Tablets")
    
    private List<EspecificacionCategoria> especificaciones; // Especificaciones que deben tener productos de esta categoría
    
    private boolean activa; // Si está activa para usar
    
    private LocalDateTime fechaCreacion;
    
    private String creadaPor; // ID del usuario que la creó
    
    private String icono; // Emoji o icono para mostrar (ej: "📱")

    public Categoria() {
        this.especificaciones = new ArrayList<>();
        this.activa = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Clase interna para las especificaciones
    public static class EspecificacionCategoria implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String nombre; // Nombre del campo (ej: "RAM")
        private String tipo; // Tipo de dato: "texto", "numero", "seleccion", "checkbox"
        private boolean obligatorio; // Si es obligatorio llenar este campo
        private String unidad; // Unidad de medida (ej: "GB", "MP", "pulgadas")
        private List<String> opciones; // Opciones si tipo es "seleccion"
        private String placeholder; // Texto de ayuda

        public EspecificacionCategoria() {
            this.opciones = new ArrayList<>();
        }

        public EspecificacionCategoria(String nombre, String tipo, boolean obligatorio, String unidad) {
            this();
            this.nombre = nombre;
            this.tipo = tipo;
            this.obligatorio = obligatorio;
            this.unidad = unidad;
        }

        // Getters y Setters
        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public boolean isObligatorio() {
            return obligatorio;
        }

        public void setObligatorio(boolean obligatorio) {
            this.obligatorio = obligatorio;
        }

        public String getUnidad() {
            return unidad;
        }

        public void setUnidad(String unidad) {
            this.unidad = unidad;
        }

        public List<String> getOpciones() {
            return opciones;
        }

        public void setOpciones(List<String> opciones) {
            this.opciones = opciones;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public List<EspecificacionCategoria> getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(List<EspecificacionCategoria> especificaciones) {
        this.especificaciones = especificaciones;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getCreadaPor() {
        return creadaPor;
    }

    public void setCreadaPor(String creadaPor) {
        this.creadaPor = creadaPor;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }
}

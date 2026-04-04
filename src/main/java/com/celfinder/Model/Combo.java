package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Document(collection = "combos")
public class Combo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String nombre;
    private List<String> productoIds;
    private double precioOriginal;
    private double precioCombo;

    public Combo() {}

    public Combo(String nombre, List<String> productoIds, double precioOriginal, double precioCombo) {
        this.nombre = nombre;
        this.productoIds = productoIds;
        this.precioOriginal = precioOriginal;
        this.precioCombo = precioCombo;
    }

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

    public List<String> getProductoIds() {
        return productoIds;
    }

    public void setProductoIds(List<String> productoIds) {
        this.productoIds = productoIds;
    }

    public double getPrecioOriginal() {
        return precioOriginal;
    }

    public void setPrecioOriginal(double precioOriginal) {
        this.precioOriginal = precioOriginal;
    }

    public double getPrecioCombo() {
        return precioCombo;
    }

    public void setPrecioCombo(double precioCombo) {
        this.precioCombo = precioCombo;
    }
}

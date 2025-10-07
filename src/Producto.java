import java.io.Serializable;

/**
 * Representa un producto del catálogo.
 */
public class Producto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nombre;
    private final int precio;       // precio por unidad (entero)
    private int cantidadVendida;    // acumulado durante el procesamiento

    public Producto(int id, String nombre, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidadVendida = 0;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPrecio() { return precio; }
    public int getCantidadVendida() { return cantidadVendida; }

    public void agregarCantidadVendida(int cantidad) {
        this.cantidadVendida += cantidad;
    }
}

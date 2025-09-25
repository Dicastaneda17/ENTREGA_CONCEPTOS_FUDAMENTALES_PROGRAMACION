
public class Producto {
    private int id;
    private String nombre;
    private int precio;
    private int cantidadVendida;

    public Producto(int id, String nombre, int precio) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidadVendida = 0;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getPrecio() { return precio; }
    public int getCantidadVendida() { return cantidadVendida; }

    public void agregarCantidadVendida(int cantidad) {
        this.cantidadVendida += cantidad;
    }
}

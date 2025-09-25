
public class Vendedor {
    private long doc;
    private String nombreCompleto;
    private long totalVendido;

    public Vendedor(long doc, String nombreCompleto) {
        this.doc = doc;
        this.nombreCompleto = nombreCompleto;
        this.totalVendido = 0L;
    }

    // Getters y Setters
    public long getDoc() { return doc; }
    public String getNombreCompleto() { return nombreCompleto; }
    public long getTotalVendido() { return totalVendido; }

    public void agregarVenta(long monto) {
        this.totalVendido += monto;
    }
}

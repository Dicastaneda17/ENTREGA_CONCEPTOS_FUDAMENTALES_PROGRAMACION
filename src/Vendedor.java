import java.io.Serializable;

/**
 * Representa un vendedor (identificado por su documento).
 */
public class Vendedor implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long doc;
    private final String nombreCompleto;
    private long totalVendido;  // acumulado durante el procesamiento

    public Vendedor(long doc, String nombreCompleto) {
        this.doc = doc;
        this.nombreCompleto = nombreCompleto;
        this.totalVendido = 0L;
    }

    public long getDoc() { return doc; }
    public String getNombreCompleto() { return nombreCompleto; }
    public long getTotalVendido() { return totalVendido; }

    public void agregarVenta(long monto) {
        this.totalVendido += monto;
    }
}

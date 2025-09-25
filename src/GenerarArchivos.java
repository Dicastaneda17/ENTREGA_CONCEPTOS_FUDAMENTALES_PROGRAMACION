import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class GenerarArchivos {

    private static final String DATA_DIR = "datos";
    private static final String VENDEDORES = DATA_DIR + File.separator + "vendedores.csv";
    private static final String PRODUCTOS  = DATA_DIR + File.separator + "productos.csv";

    // Listas base
    private static final String[] NOMBRES   = {"Carlos", "Maria", "Andres", "Sofia", "Laura", "Jorge"};
    private static final String[] APELLIDOS = {"Perez", "Gomez", "Rodriguez", "Martinez", "Hernandez", "Lopez"};

    private static final Random R = new Random();

    public static void main(String[] args) {
        try {
            ensureDataDir();

            //5 vendedores, 10 productos, y ventas para cada vendedor
            crearArchivoVendedores(5);   // vendedores.csv
            crearArchivoProductos(10);   // productos.csv

            for (int i = 0; i < 5; i++) {
                int productosVendidos = numeroAleatorio(3, 7);
                crearArchivoVentas(i, productosVendidos, 10); // ventas_CC_<numero>.csv
            }

            System.out.println("🎉 Archivos de prueba generados correctamente en la carpeta '" + DATA_DIR + "'.");
        } catch (Exception e) {
            System.err.println("❌ Error generando archivos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // crearArchivoVendedores
    private static void crearArchivoVendedores(int cantidad) throws IOException {
        try (BufferedWriter bw = writer(VENDEDORES)) {
            for (int i = 0; i < cantidad; i++) {
                String tipoDocumento = "CC";
                long numeroDocumento = 1000L + i;
                String nombre  = NOMBRES[numeroAleatorio(0, NOMBRES.length - 1)];
                String apellido = APELLIDOS[numeroAleatorio(0, APELLIDOS.length - 1)];

                // TipoDocumento;NumeroDocumento;Nombres;Apellidos
                bw.write(tipoDocumento + ";" + numeroDocumento + ";" + nombre + ";" + apellido);
                bw.newLine();
            }
        }
        System.out.println("✅ Archivo vendedores.csv generado");
    }

    // crearArchivoProductos
    private static void crearArchivoProductos(int cantidad) throws IOException {
        try (BufferedWriter bw = writer(PRODUCTOS)) {
            for (int i = 1; i <= cantidad; i++) {
                int idProducto = i;
                String nombreProducto = "Producto" + i;
                int precio = numeroAleatorio(1000, 10000);

                // IDProducto;NombreProducto;PrecioPorUnidad
                bw.write(idProducto + ";" + nombreProducto + ";" + precio);
                bw.newLine();
            }
        }
        System.out.println("✅ Archivo productos.csv generado");
    }

    // crearArchivoVentas(idVendedor, cantidadProductos, productosTotales)
    private static void crearArchivoVentas(int idVendedor, int cantidadProductos, int productosTotales) throws IOException {
        String tipoDocumento = "CC";
        long numeroDocumento = 1000L + idVendedor;

        String fileName = DATA_DIR + File.separator + ("ventas_" + tipoDocumento + "_" + numeroDocumento + ".csv");
        try (BufferedWriter bw = writer(fileName)) {
            // Línea 1: identificación del vendedor (sin encabezados, como la guía)
            bw.write(tipoDocumento + ";" + numeroDocumento);
            bw.newLine();

            // Siguientes líneas: IDProducto;Cantidad
            for (int i = 0; i < cantidadProductos; i++) {
                int idProducto = numeroAleatorio(1, productosTotales);
                int cantidadVendida = numeroAleatorio(1, 10);
                bw.write(idProducto + ";" + cantidadVendida);
                bw.newLine();
            }
        }
        System.out.println("✅ Archivo " + ("ventas_" + tipoDocumento + "_" + numeroDocumento + ".csv") + " generado");
    }

    // Utilidades 

    private static void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static BufferedWriter writer(String path) throws IOException {
        return new BufferedWriter(new FileWriter(path, StandardCharsets.UTF_8));
    }

    private static int numeroAleatorio(int min, int max) {
        return min + R.nextInt((max - min) + 1);
    }
}

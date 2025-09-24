import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class main {

    private static final String DATA_DIR = "datos";
    private static final String VENDEDORES = DATA_DIR + File.separator + "vendedores.csv";
    private static final String PRODUCTOS  = DATA_DIR + File.separator + "productos.csv";

    public static void main(String[] args) {
        try {
            procesarVentas();
            System.out.println("🎉 Reportes generados correctamente en la carpeta '" + DATA_DIR + "'.");
        } catch (Exception e) {
            System.err.println("❌ Error procesando archivos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void procesarVentas() throws IOException {
        // ---- Leer base de productos
        Map<Integer, Producto> mapaProductos = new HashMap<>();
        for (String linea : leerLineas(PRODUCTOS)) {
            if (linea.trim().isEmpty()) continue;
            String[] p = linea.split(";");
            if (p.length < 3) continue;
            int id = Integer.parseInt(p[0]);
            String nombre = p[1];
            int precio = parseEnteroSeguro(p[2]);
            mapaProductos.put(id, new Producto(id, nombre, precio));
        }

        // ---- Leer vendedores
        Map<Long, Vendedor> mapaVendedores = new HashMap<>();
        for (String linea : leerLineas(VENDEDORES)) {
            if (linea.trim().isEmpty()) continue;
            String[] p = linea.split(";");
            if (p.length < 4) continue;
            // String tipoDoc = p[0];
            long numDoc = Long.parseLong(p[1]);
            String nombreCompleto = p[2] + " " + p[3];
            mapaVendedores.put(numDoc, new Vendedor(numDoc, nombreCompleto));
        }

        // ---- Procesar cada archivo de ventas_*.csv
        File dir = new File(DATA_DIR);
        String[] archivosVentas = dir.list((d, name) -> name.startsWith("ventas_") && name.endsWith(".csv"));
        if (archivosVentas == null) archivosVentas = new String[0];

        for (String nombreArchivo : archivosVentas) {
            List<String> lineas = leerLineas(DATA_DIR + File.separator + nombreArchivo);
            if (lineas.isEmpty()) continue;

            // Primera línea: TipoDoc;NumeroDoc
            String[] cab = lineas.get(0).split(";");
            if (cab.length < 2) continue;
            long numDoc = Long.parseLong(cab[1]);

            for (int i = 1; i < lineas.size(); i++) {
                String l = lineas.get(i).trim();
                if (l.isEmpty()) continue;
                String[] p = l.split(";");
                if (p.length < 2) continue;

                int idProducto = Integer.parseInt(p[0]);
                int cantidad = parseEnteroSeguro(p[1]);

                Producto prod = mapaProductos.get(idProducto);
                Vendedor vend = mapaVendedores.get(numDoc);
                if (prod != null && vend != null) {
                    long venta = (long) prod.precio * (long) cantidad;
                    vend.totalVendido += venta;
                    prod.cantidadVendida += cantidad;
                }
            }
        }

        // ---- Reporte vendedores (desc por total)
        String reporteVendedores = mapaVendedores.values().stream()
                .sorted((a, b) -> Long.compare(b.totalVendido, a.totalVendido))
                .map(v -> v.nombreCompleto + ";" + v.totalVendido)
                .collect(Collectors.joining("\n"));
        escribir(DATA_DIR + File.separator + "reporte_vendedores.csv", reporteVendedores);

        // ---- Reporte productos (desc por cantidadVendida)
        String reporteProductos = mapaProductos.values().stream()
                .sorted((a, b) -> Integer.compare(b.cantidadVendida, a.cantidadVendida))
                .map(p -> p.nombre + ";" + p.precio + ";" + p.cantidadVendida)
                .collect(Collectors.joining("\n"));
        escribir(DATA_DIR + File.separator + "reporte_productos.csv", reporteProductos);
    }

    //Utilidades

    private static List<String> leerLineas(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.exists()) return Collections.emptyList();
        return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static void escribir(String ruta, String contenido) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ruta, StandardCharsets.UTF_8))) {
            bw.write(contenido == null ? "" : contenido);
        }
    }

    private static int parseEnteroSeguro(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // POJOs 
    private static class Producto {
        int id;
        String nombre;
        int precio; // unidad monetaria entera
        int cantidadVendida = 0;
        Producto(int id, String nombre, int precio) {
            this.id = id; this.nombre = nombre; this.precio = precio;
        }
    }
    private static class Vendedor {
        long numDoc;
        String nombreCompleto;
        long totalVendido = 0L;
        Vendedor(long numDoc, String nombreCompleto) {
            this.numDoc = numDoc; this.nombreCompleto = nombreCompleto;
        }
    }
}

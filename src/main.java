import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class main {

    private static final String DATA_DIR = "datos";
    private static final String VENDEDORES_CSV = DATA_DIR + File.separator + "vendedores.csv";
    private static final String PRODUCTOS_CSV  = DATA_DIR + File.separator + "productos.csv";

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
        // ---- Cargar productos
        Map<Integer, Producto> productos = new HashMap<>();
        for (String linea : leerLineas(PRODUCTOS_CSV)) {
            String[] p = linea.split(";");
            if (p.length < 3) continue;
            int id = toInt(p[0]);
            String nombre = p[1];
            int precio = toInt(p[2]);
            if (id > 0 && precio >= 0) {
                productos.put(id, new Producto(id, nombre, precio));
            }
        }

        // ---- Cargar vendedores
        Map<Long, Vendedor> vendedores = new HashMap<>();
        for (String linea : leerLineas(VENDEDORES_CSV)) {
            String[] v = linea.split(";");
            if (v.length < 4) continue;
            long doc = toLong(v[1]);
            String nombreCompleto = v[2] + " " + v[3];
            if (doc > 0) {
                vendedores.put(doc, new Vendedor(doc, nombreCompleto));
            }
        }

        // ---- Procesar archivos de ventas
        File dir = new File(DATA_DIR);
        String[] archivosVentas = dir.list((d, name) -> name.startsWith("ventas_") && name.endsWith(".csv"));
        if (archivosVentas == null) archivosVentas = new String[0];

        for (String archivo : archivosVentas) {
            List<String> lineas = leerLineas(DATA_DIR + File.separator + archivo);
            if (lineas.isEmpty()) continue;

            String[] cab = lineas.get(0).split(";");
            if (cab.length < 2) continue;
            long doc = toLong(cab[1]);
            Vendedor vend = vendedores.get(doc);
            if (vend == null) continue;

            for (int i = 1; i < lineas.size(); i++) {
                String[] s = lineas.get(i).split(";");
                if (s.length < 2) continue;
                int idProd = toInt(s[0]);
                int cant   = toInt(s[1]);

                Producto prod = productos.get(idProd);
                if (prod != null && cant > 0) {
                    vend.agregarVenta((long) prod.getPrecio() * cant);
                    prod.agregarCantidadVendida(cant);
                }
            }
        }

        // ---- Generar reportes
        String repVend = vendedores.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalVendido(), a.getTotalVendido()))
                .map(v -> v.getNombreCompleto() + ";" + v.getTotalVendido())
                .collect(Collectors.joining("\n"));
        escribir(DATA_DIR + File.separator + "reporte_vendedores.csv", repVend);

        String repProd = productos.values().stream()
                .sorted((a, b) -> Integer.compare(b.getCantidadVendida(), a.getCantidadVendida()))
                .map(p -> p.getNombre() + ";" + p.getPrecio() + ";" + p.getCantidadVendida())
                .collect(Collectors.joining("\n"));
        escribir(DATA_DIR + File.separator + "reporte_productos.csv", repProd);
    }

    // ===== Utilidades =====
    private static List<String> leerLineas(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.exists()) return Collections.emptyList();
        return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
    }

    private static void escribir(String ruta, String contenido) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(new File(ruta).toPath()), StandardCharsets.UTF_8))) {
            bw.write(contenido == null ? "" : contenido);
        }
    }

    private static int toInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static long toLong(String s){ try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; } }
}

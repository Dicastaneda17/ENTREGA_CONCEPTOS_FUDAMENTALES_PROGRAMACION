import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase principal (Entrega 3 - Final).
 *
 * Lee:
 *   - datos/vendedores.csv  -> TipoDocumento;NumeroDocumento;Nombres;Apellidos
 *   - datos/productos.csv   -> IDProducto;NombreProducto;PrecioPorUnidad
 *   - datos/ventas_*.csv    -> 1a línea: TipoDoc;NumeroDoc  |  restantes: IDProducto;Cantidad
 *
 * Genera:
 *   - datos/reporte_vendedores.csv -> NombreCompleto;TotalRecaudado (desc)
 *   - datos/reporte_productos.csv  -> NombreProducto;Precio;CantidadVendida (desc)
 *
 * Extras Entrega 3:
 *   - Validaciones robustas con logs de advertencia.
 *   - Consolidación cuando hay múltiples archivos por vendedor.
 *   - Serialización de mapas a binario para auditoría (opcional).
 *   - Sin interacción del usuario.
 */
public class main {

    private static final String DATA_DIR        = "datos";
    private static final String VENDEDORES_CSV  = DATA_DIR + File.separator + "vendedores.csv";
    private static final String PRODUCTOS_CSV   = DATA_DIR + File.separator + "productos.csv";
    private static final String REP_VENDEDORES  = DATA_DIR + File.separator + "reporte_vendedores.csv";
    private static final String REP_PRODUCTOS   = DATA_DIR + File.separator + "reporte_productos.csv";

    // Cambia a true si quieres que al primer error el programa termine.
    private static final boolean STRICT_MODE = true;

    public static void main(String[] args) {
        try {
            validarExistenciaArchivosBase();
            Map<Integer, Producto> productos  = cargarProductos(PRODUCTOS_CSV);
            Map<Long, Vendedor> vendedores    = cargarVendedores(VENDEDORES_CSV);

            procesarVentas(DATA_DIR, productos, vendedores);

            generarReporteVendedores(REP_VENDEDORES, vendedores);
            generarReporteProductos(REP_PRODUCTOS, productos);

            // (Extra) Guardar snapshot del estado a binario para auditoría
            serializarEstado(DATA_DIR + File.separator + "snapshot_productos.bin", productos);
            serializarEstado(DATA_DIR + File.separator + "snapshot_vendedores.bin", vendedores);

            System.out.println("✅ Entrega 3 completada. Reportes generados en '" + DATA_DIR + "'.");
        } catch (Exception e) {
            System.err.println("❌ Error crítico: " + e.getMessage());
            e.printStackTrace();
            if (STRICT_MODE) System.exit(1);
        }
    }

    // ========================= CARGA DE MAESTROS =========================

    private static Map<Integer, Producto> cargarProductos(String ruta) throws IOException {
        Map<Integer, Producto> mapa = new HashMap<>();
        List<String> lineas = leerLineas(ruta);

        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            String[] p = linea.split(";");
            if (p.length < 3) { warn(ruta, i+1, "Línea incompleta (se esperan 3 campos)."); continue; }

            Integer id = toInt(p[0]);
            String nombre = p[1].trim();
            Integer precio = toInt(p[2]);

            if (id == null || id <= 0)               { warn(ruta, i+1, "ID de producto inválido: " + p[0]); continue; }
            if (nombre.isEmpty())                     { warn(ruta, i+1, "Nombre de producto vacío."); continue; }
            if (precio == null || precio < 0)        { warn(ruta, i+1, "Precio inválido (negativo o no numérico): " + p[2]); continue; }

            if (mapa.containsKey(id)) {
                warn(ruta, i+1, "ID de producto duplicado (" + id + "). Se conservará el primero.");
                continue;
            }
            mapa.put(id, new Producto(id, nombre, precio));
        }

        if (mapa.isEmpty()) throw new IllegalStateException("productos.csv no cargó ningún producto válido.");
        return mapa;
    }

    private static Map<Long, Vendedor> cargarVendedores(String ruta) throws IOException {
        Map<Long, Vendedor> mapa = new HashMap<>();
        List<String> lineas = leerLineas(ruta);

        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            String[] v = linea.split(";");
            if (v.length < 4) { warn(ruta, i+1, "Línea incompleta (se esperan 4 campos)."); continue; }

            Long doc = toLong(v[1]);
            String nombre = v[2].trim();
            String apellido = v[3].trim();

            if (doc == null || doc <= 0)             { warn(ruta, i+1, "Documento inválido: " + v[1]); continue; }
            if (nombre.isEmpty() || apellido.isEmpty()){ warn(ruta, i+1, "Nombre o apellido vacío."); continue; }

            mapa.put(doc, new Vendedor(doc, nombre + " " + apellido));
        }

        if (mapa.isEmpty()) throw new IllegalStateException("vendedores.csv no cargó ningún vendedor válido.");
        return mapa;
    }

    // ========================= PROCESAMIENTO DE VENTAS =========================

    private static void procesarVentas(String carpeta, Map<Integer, Producto> productos, Map<Long, Vendedor> vendedores) throws IOException {
        File dir = new File(carpeta);
        String[] archivosVentas = dir.list((d, name) -> name.startsWith("ventas_") && name.endsWith(".csv"));
        if (archivosVentas == null || archivosVentas.length == 0) {
            warn(carpeta, 0, "No se encontraron archivos ventas_*.csv. Se generarán reportes vacíos.");
            return;
        }

        Arrays.sort(archivosVentas); // determinismo

        for (String archivo : archivosVentas) {
            String ruta = carpeta + File.separator + archivo;
            List<String> lineas = leerLineas(ruta);
            if (lineas.isEmpty()) { warn(ruta, 1, "Archivo vacío."); continue; }

            // Primera línea: TipoDoc;NumeroDoc
            String[] cab = lineas.get(0).split(";");
            if (cab.length < 2) { warn(ruta, 1, "Cabecera inválida (esperado TipoDoc;NumeroDoc)."); continue; }
            Long doc = toLong(cab[1]);
            if (doc == null || doc <= 0) { warn(ruta, 1, "Número de documento inválido en cabecera: " + cab[1]); continue; }

            Vendedor vendedor = vendedores.get(doc);
            if (vendedor == null) {
                warn(ruta, 1, "Vendedor con doc " + doc + " no existe en maestro. Se ignora archivo.");
                continue;
            }

            // Resto de líneas: IDProducto;Cantidad
            for (int i = 1; i < lineas.size(); i++) {
                String l = lineas.get(i).trim();
                if (l.isEmpty()) continue;

                String[] s = l.split(";");
                if (s.length < 2) { warn(ruta, i+1, "Línea de venta incompleta (esperado IDProducto;Cantidad)."); continue; }

                Integer idProd = toInt(s[0]);
                Integer cant   = toInt(s[1]);

                if (idProd == null || idProd <= 0)  { warn(ruta, i+1, "ID de producto inválido: " + s[0]); continue; }
                if (cant == null || cant <= 0)      { warn(ruta, i+1, "Cantidad inválida (<=0 o no numérica): " + s[1]); continue; }

                Producto prod = productos.get(idProd);
                if (prod == null) {
                    warn(ruta, i+1, "ID de producto inexistente en maestro: " + idProd + ". Línea ignorada.");
                    continue;
                }
                if (prod.getPrecio() < 0) {
                    warn(ruta, i+1, "Precio negativo para producto " + idProd + ". Línea ignorada.");
                    continue;
                }

                long totalLinea = (long) prod.getPrecio() * (long) cant;
                vendedor.agregarVenta(totalLinea);
                prod.agregarCantidadVendida(cant);
            }
        }
    }

    // ========================= REPORTES =========================

    private static void generarReporteVendedores(String ruta, Map<Long, Vendedor> vendedores) throws IOException {
        String contenido = vendedores.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalVendido(), a.getTotalVendido()))
                .map(v -> v.getNombreCompleto() + ";" + v.getTotalVendido())
                .collect(Collectors.joining("\n"));
        escribir(ruta, contenido);
        System.out.println("📄 Generado: " + ruta);
    }

    private static void generarReporteProductos(String ruta, Map<Integer, Producto> productos) throws IOException {
        String contenido = productos.values().stream()
                .sorted((a, b) -> Integer.compare(b.getCantidadVendida(), a.getCantidadVendida()))
                .map(p -> p.getNombre() + ";" + p.getPrecio() + ";" + p.getCantidadVendida())
                .collect(Collectors.joining("\n"));
        escribir(ruta, contenido);
        System.out.println("📄 Generado: " + ruta);
    }

    // ========================= UTILIDADES =========================

    private static void validarExistenciaArchivosBase() {
        if (!new File(VENDEDORES_CSV).exists())
            throw new IllegalStateException("No existe " + VENDEDORES_CSV + ". Ejecuta primero GenerateInfoFiles.");
        if (!new File(PRODUCTOS_CSV).exists())
            throw new IllegalStateException("No existe " + PRODUCTOS_CSV + ". Ejecuta primero GenerateInfoFiles.");
    }

    private static List<String> leerLineas(String ruta) throws IOException {
        File f = new File(ruta);
        if (!f.exists()) return Collections.emptyList();
        return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)
                .stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private static void escribir(String ruta, String contenido) throws IOException {
        File out = new File(ruta);
        out.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8))) {
            bw.write(contenido == null ? "" : contenido);
        }
    }

    private static Integer toInt(String s) {
        try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; }
    }
    private static Long toLong(String s) {
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    private static void warn(String origen, int linea, String msg) {
        String at = (linea > 0) ? (origen + ":" + linea) : origen;
        System.err.println("⚠️  " + at + " -> " + msg);
        if (STRICT_MODE) throw new IllegalStateException(at + " -> " + msg);
    }

    // ====== Extra: serialización de estado (auditoría / carga posterior) ======
    private static void serializarEstado(String ruta, Object objeto) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ruta))) {
            oos.writeObject(objeto);
        } catch (IOException e) {
            System.err.println("⚠️  No se pudo serializar en " + ruta + ": " + e.getMessage());
        }
    }
}

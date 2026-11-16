package com.laptopstudio.service;

import com.laptopstudio.exception.NegocioException;
import com.laptopstudio.model.Laptop;
import com.laptopstudio.viewer.ExternalModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Almacen local de modelos 3D externos (formato OBJ), uno por laptop, en
 * {@code ~/.laptop-comparator/models3d/<id>/model.obj} junto a su MTL y texturas. Una vez importado, el modelo
 * se ve sin conexion. Las operaciones leen y escriben disco o red: llamarlas fuera del hilo de la interfaz.
 */
@Service
@Slf4j
public class ModelStore {

    private static final long MAX_BYTES = 120L * 1024 * 1024;
    /** Tope de cada archivo que acompana al OBJ en una descarga (MTL o textura) y numero maximo de texturas. */
    private static final long MAX_SECONDARY_BYTES = 40L * 1024 * 1024;
    private static final int MAX_TEXTURES = 48;
    private static final int MAX_REDIRECTS = 5;

    /** Resultado de una importacion. {@code warning} es nulo si las proporciones cuadran con la ficha. */
    public record Report(ExternalModel.Info info, String warning) {
    }

    /** Carpeta del modelo de un laptop. Estatico para que el visor lo consulte sin depender de Spring. */
    public static Path dirFor(Long laptopId) {
        return Path.of(System.getProperty("user.home"), ".laptop-comparator", "models3d", String.valueOf(laptopId));
    }

    /** Archivo OBJ importado para el laptop, o null si no tiene modelo externo. */
    public static Path fileFor(Laptop l) {
        if (l == null || l.getId() == null) {
            return null;
        }
        Path f = dirFor(l.getId()).resolve("model.obj");
        return Files.isRegularFile(f) ? f : null;
    }

    public boolean has(Laptop l) {
        return fileFor(l) != null;
    }

    /** Importa un OBJ del disco, con el MTL y las texturas que tenga al lado. */
    public Report importFile(Laptop l, Path source) {
        requireSaved(l);
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new NegocioException("El archivo supera 120 MB.");
            }
            ExternalModel.Info info = ExternalModel.inspect(source);
            Path dir = dirFor(l.getId());
            remove(l);
            Files.createDirectories(dir);
            Files.copy(source, dir.resolve("model.obj"), StandardCopyOption.REPLACE_EXISTING);
            for (String mtl : references(source, "mtllib ")) {
                Path src = source.resolveSibling(mtl);
                if (Files.isRegularFile(src)) {
                    Files.copy(src, dir.resolve(fileName(mtl)), StandardCopyOption.REPLACE_EXISTING);
                    for (String tex : references(src, "map_Kd ")) {
                        Path t = src.resolveSibling(tex);
                        if (!Files.isRegularFile(t)) {
                            t = src.resolveSibling(fileName(tex));
                        }
                        if (Files.isRegularFile(t)) {
                            Files.copy(t, dir.resolve(fileName(tex)), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            log.info("Modelo 3D importado para {}: {} triangulos", l.getDisplayName(), info.triangles());
            return new Report(info, proportionWarning(l, info));
        } catch (IOException e) {
            throw new NegocioException("No se pudo importar el modelo: " + e.getMessage());
        }
    }

    /**
     * Descarga un OBJ desde un enlace directo (y su MTL y texturas si estan en el mismo servidor) y lo importa.
     * El enlace puede venir de una pagina web y no del usuario, asi que solo se acepta https hacia servidores
     * publicos, y esa misma comprobacion se repite en cada redireccion.
     */
    public Report importUrl(Laptop l, String url) {
        requireSaved(l);
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("El enlace no es válido.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new NegocioException("El enlace debe empezar por https://");
        }
        if (uri.getPath() == null || !uri.getPath().toLowerCase(Locale.ROOT).endsWith(".obj")) {
            throw new NegocioException("El enlace debe apuntar directamente a un archivo .obj");
        }
        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("laptopstudio-obj");
            // las redirecciones se siguen a mano en download(): asi se puede comprobar cada destino
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            Path obj = tmp.resolve("model.obj");
            URI base = download(http, uri, obj, MAX_BYTES, true);
            int texturas = 0;
            for (String mtl : references(obj, "mtllib ")) {
                Path mtlFile = tmp.resolve(fileName(mtl));
                URI mtlUri = sameServer(base, mtl);
                if (mtlUri == null || download(http, mtlUri, mtlFile, MAX_SECONDARY_BYTES, false) == null) {
                    continue;
                }
                for (String tex : references(mtlFile, "map_Kd ")) {
                    URI texUri = sameServer(base, tex);
                    if (texUri != null && texturas < MAX_TEXTURES) {
                        texturas++;
                        download(http, texUri, tmp.resolve(fileName(tex)), MAX_SECONDARY_BYTES, false);
                    }
                }
            }
            return importFile(l, obj);
        } catch (IOException e) {
            throw new NegocioException("No se pudo descargar el modelo: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NegocioException("Descarga cancelada.");
        } finally {
            deleteTree(tmp);
        }
    }

    public void remove(Laptop l) {
        if (l != null && l.getId() != null) {
            deleteTree(dirFor(l.getId()));
        }
    }

    /* ------------------------------------------------------------------ apoyo */

    private static void requireSaved(Laptop l) {
        if (l == null || l.getId() == null) {
            throw new NegocioException("Guarda el laptop en el catálogo antes de asignarle un modelo 3D.");
        }
    }

    /**
     * Compara la huella del modelo (lado mayor / lado menor en planta) con ancho / fondo de la ficha.
     * No demuestra que el modelo sea de ese equipo, solo avisa cuando las proporciones no cuadran.
     */
    private static String proportionWarning(Laptop l, ExternalModel.Info i) {
        if (l.getWidthMm() == null || l.getDepthMm() == null || l.getDepthMm() <= 0) {
            return "La ficha no tiene ancho y fondo, así que no se pudieron comprobar las proporciones del modelo.";
        }
        double ficha = l.getWidthMm() / l.getDepthMm();
        double a = Math.max(i.sizeX(), i.sizeZ()), b = Math.max(1e-9, Math.min(i.sizeX(), i.sizeZ()));
        double alt = Math.max(i.sizeX(), i.sizeY()) / Math.max(1e-9, Math.min(i.sizeX(), i.sizeY()));
        double mejor = Math.min(Math.abs(a / b - ficha), Math.abs(alt - ficha)) / ficha;
        return mejor <= 0.30 ? null : String.format(Locale.US,
                "Las proporciones del modelo (%.2f : 1) no coinciden con las de la ficha (%.2f : 1). "
                        + "Puede ser otro equipo, o estar orientado de otra forma. Compruébalo en el visor.", a / b, ficha);
    }

    /**
     * Comprueba que la direccion es https y que su servidor no es este equipo ni uno de la red local. Un enlace
     * sacado de una pagina web no debe servir para hacer peticiones a servicios internos del usuario.
     */
    private static void requirePublicHttps(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new NegocioException("Solo se descargan modelos por https (" + uri + ").");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new NegocioException("No se encontró el servidor " + uri.getHost() + ".");
        }
        for (InetAddress a : addresses) {
            if (isLocal(a)) {
                throw new NegocioException("El enlace apunta a este equipo o a la red local (" + uri.getHost()
                        + "). Solo se descargan modelos de servidores públicos; si tienes el archivo, impórtalo desde el disco.");
            }
        }
    }

    private static boolean isLocal(InetAddress a) {
        if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        boolean ipv6UniqueLocal = b.length == 16 && (b[0] & 0xfe) == 0xfc;                          // fc00::/7
        boolean ipv4SharedSpace = b.length == 4 && (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 64;     // 100.64.0.0/10
        return ipv6UniqueLocal || ipv4SharedSpace;
    }

    /**
     * Direccion de un archivo citado dentro del modelo (MTL o textura), o null si no esta en el mismo servidor que
     * el OBJ. El nombre sale del contenido descargado: sin este limite, el archivo decidiria a que otros sitios
     * se conecta la aplicacion.
     */
    private static URI sameServer(URI base, String ref) {
        try {
            URI u = base.resolve(encode(ref));
            boolean same = u.getHost() != null && u.getHost().equalsIgnoreCase(base.getHost())
                    && "https".equalsIgnoreCase(u.getScheme()) && u.getPort() == base.getPort();
            return same ? u : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Descarga {@code uri} en {@code target}, con un tope de tamano. Sigue las redirecciones a mano para aplicar a
     * cada destino la misma comprobacion que al enlace original. Devuelve la direccion final, o null si el archivo
     * no se pudo obtener y no era obligatorio.
     */
    private static URI download(HttpClient http, URI uri, Path target, long maxBytes, boolean required)
            throws IOException, InterruptedException {
        URI current = uri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            try {
                requirePublicHttps(current);
            } catch (NegocioException e) {
                if (required) {
                    throw e;
                }
                return null;
            }
            HttpRequest req = HttpRequest.newBuilder(current).timeout(Duration.ofSeconds(90))
                    .header("User-Agent", "LaptopExplorer").GET().build();
            HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = res.body()) {
                int status = res.statusCode();
                if (status >= 300 && status < 400) {
                    String location = res.headers().firstValue("Location").orElse(null);
                    if (location == null) {
                        throw new IOException("el servidor redirigió sin indicar destino");
                    }
                    try {
                        current = current.resolve(location.trim());
                    } catch (IllegalArgumentException e) {
                        throw new IOException("el servidor redirigió a una dirección no válida");
                    }
                    continue;
                }
                if (status != 200) {
                    if (required) {
                        throw new IOException("el servidor respondió " + status);
                    }
                    return null;
                }
                long total = 0;
                byte[] buf = new byte[1 << 16];
                try (var out = Files.newOutputStream(target)) {
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > maxBytes) {
                            throw new IOException("el archivo supera " + maxBytes / (1024 * 1024) + " MB");
                        }
                        out.write(buf, 0, n);
                    }
                }
                return current;
            }
        }
        throw new IOException("demasiadas redirecciones");
    }

    /** Nombres de archivo citados en lineas que empiezan por {@code prefix} (mtllib, map_Kd). */
    private static Set<String> references(Path file, String prefix) throws IOException {
        Set<String> out = new LinkedHashSet<>();
        try (Stream<String> lines = Files.lines(file, StandardCharsets.ISO_8859_1)) {
            lines.map(String::trim).filter(s -> s.startsWith(prefix))
                    .forEach(s -> out.add(s.substring(prefix.length()).trim().replace('\\', '/')));
        }
        return out;
    }

    private static String fileName(String ref) {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private static String encode(String ref) {
        return ref.replace(" ", "%20");
    }

    private static void deleteTree(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> all = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : all) {
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            log.warn("No se pudo borrar {}: {}", dir, e.getMessage());
        }
    }
}

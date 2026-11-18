package com.laptopstudio.viewer;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector de modelos Wavefront OBJ: los que importa el usuario desde el disco y los de la biblioteca de componentes
 * empaquetada con la aplicacion. Admite varios objetos y materiales, normales propias del archivo, y color,
 * brillo y textura (Kd, Ks, Ns, map_Kd) de un MTL situado junto al OBJ.
 * El resultado queda centrado y apoyado sobre y = 0, y si se pide, escalado para que su lado mayor en planta
 * mida un ancho dado. Se puede llamar fuera del hilo de JavaFX: solo crea nodos, no los adjunta a ninguna escena.
 */
public final class ExternalModel {

    /** Limite de triangulos: por encima el visor se vuelve lento en equipos modestos. */
    public static final int MAX_TRIANGLES = 600_000;

    /** Eje que apunta hacia arriba en el archivo. Los OBJ suelen usar Y; los modelos CAD de la biblioteca usan Z. */
    public enum Up { Y, Z }

    /**
     * Una pieza del modelo: las caras de un objeto que comparten material. {@code min} y {@code max} son su caja
     * envolvente en las unidades del visor, con el modelo ya centrado y apoyado en y = 0 (hacia arriba es -Y).
     */
    public record Part(String object, String material, MeshView view, PhongMaterial phong, Color color, int triangles,
                       double[] min, double[] max) {
    }

    /**
     * Modelo cargado. {@code width/height/depth} estan en unidades del visor; {@code scale} es cuantas unidades
     * del visor ocupa una unidad del archivo (1 si no se pidio escalar).
     */
    public record Loaded(Group root, int triangles, double width, double height, double depth, List<Part> parts,
                         double scale) {
    }

    /** Medidas originales del archivo, para comprobar proporciones antes de aceptarlo. */
    public record Info(int vertices, int triangles, double sizeX, double sizeY, double sizeZ) {
    }

    /** De donde se leen el OBJ y los archivos que cita (MTL y texturas), que deben estar a su lado. */
    public interface Source {
        BufferedReader open() throws IOException;

        /** Lector de un archivo vecino, o null si no existe. */
        BufferedReader sibling(String name) throws IOException;

        /** Direccion de una imagen vecina, lista para {@link Image}, o null si no existe. */
        String image(String name);
    }

    private ExternalModel() {
    }

    /** Modelo guardado en el disco. */
    public static Source file(Path obj) {
        return new Source() {
            @Override
            public BufferedReader open() throws IOException {
                return Files.newBufferedReader(obj, StandardCharsets.ISO_8859_1);
            }

            @Override
            public BufferedReader sibling(String name) throws IOException {
                Path f = obj.resolveSibling(name);
                return Files.isRegularFile(f) ? Files.newBufferedReader(f, StandardCharsets.ISO_8859_1) : null;
            }

            @Override
            public String image(String name) {
                // solo el nombre del archivo: la textura debe estar en la misma carpeta que el modelo
                Path f = obj.resolveSibling(baseName(name));
                return Files.isRegularFile(f) ? f.toUri().toString() : null;
            }
        };
    }

    /** Modelo empaquetado con la aplicacion; {@code objPath} es la ruta del recurso, por ejemplo "/components/CPU/X/X.obj". */
    public static Source resource(String objPath) {
        String dir = objPath.substring(0, objPath.lastIndexOf('/') + 1);
        return new Source() {
            @Override
            public BufferedReader open() throws IOException {
                BufferedReader r = sibling(baseName(objPath));
                if (r == null) {
                    throw new IOException("No se encontró el modelo " + objPath);
                }
                return r;
            }

            @Override
            public BufferedReader sibling(String name) {
                InputStream in = ExternalModel.class.getResourceAsStream(dir + baseName(name));
                return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
            }

            @Override
            public String image(String name) {
                // "@carpeta/foto.jpg": imagen de los recursos compartida por varios modelos, sin copiarla a cada uno
                URL u = ExternalModel.class.getResource(name.startsWith("@") ? "/" + name.substring(1) : dir + baseName(name));
                return u == null ? null : u.toExternalForm();
            }
        };
    }

    private static String baseName(String ref) {
        String s = ref.replace('\\', '/');
        return s.substring(s.lastIndexOf('/') + 1);
    }

    /** Lee solo geometria y devuelve sus medidas. Lanza IOException si el archivo no es un OBJ utilizable. */
    public static Info inspect(Path obj) throws IOException {
        Parsed p = parse(file(obj));
        return new Info(p.vs.size(), p.tris, p.max[0] - p.min[0], p.max[1] - p.min[1], p.max[2] - p.min[2]);
    }

    /** Modelo del disco con +Y hacia arriba, escalado para que su lado mayor en planta mida {@code targetWidth}. */
    public static Loaded load(Path obj, double targetWidth) throws IOException {
        return load(file(obj), Up.Y, targetWidth);
    }

    /**
     * @param up          eje vertical del archivo
     * @param targetWidth medida que debe tener el lado mayor en planta; 0 o negativo deja el modelo a su escala
     */
    public static Loaded load(Source src, Up up, double targetWidth) throws IOException {
        return load(src, up, targetWidth, false);
    }

    /** Modelo tal como esta en el archivo, sin centrarlo ni escalarlo: (x, y, z) del OBJ con +Z arriba pasa a (x, -z, y). */
    public static Loaded loadInPlace(Source src) throws IOException {
        return load(src, Up.Z, 0, true);
    }

    private static Loaded load(Source src, Up up, double targetWidth, boolean inPlace) throws IOException {
        Parsed p = parse(src);
        // caja envolvente con Y hacia arriba (la convencion de trabajo de este lector)
        double[] mn = up == Up.Z ? new double[]{p.min[0], p.min[2], -p.max[1]} : p.min.clone();
        double[] mx = up == Up.Z ? new double[]{p.max[0], p.max[2], -p.min[1]} : p.max.clone();
        double sx = mx[0] - mn[0], sy = mx[1] - mn[1], sz = mx[2] - mn[2];
        double k = targetWidth > 0 ? targetWidth / Math.max(sx, Math.max(sz, 1e-9)) : 1;
        double cx = inPlace ? 0 : (mn[0] + mx[0]) / 2, cz = inPlace ? 0 : (mn[2] + mx[2]) / 2, baseY = inPlace ? 0 : mn[1];
        Map<String, Mtl> mtls = p.mtlLib == null ? Map.of() : readMtl(src, p.mtlLib);

        Group root = new Group();
        List<Part> parts = new ArrayList<>();
        for (Chunk c : p.chunks.values()) {
            boolean normals = c.normals && !p.vns.isEmpty();
            TriangleMesh mesh = new TriangleMesh(normals ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);
            Floats points = new Floats(), norms = new Floats(), uvs = new Floats();
            Ints faces = new Ints();
            Map<Integer, Integer> pointIdx = new HashMap<>(), normIdx = new HashMap<>(), uvIdx = new HashMap<>();
            double[] bmin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            double[] bmax = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            int tris = 0;
            for (int[][] f : c.faces) {
                for (int i = 1; i + 1 < f.length; i++) {
                    tris++;
                    for (int[] corner : new int[][]{f[0], f[i], f[i + 1]}) {
                        Integer a = pointIdx.get(corner[0]);
                        if (a == null) {
                            double[] v = yUp(p.vs.get(corner[0]), up);
                            // El OBJ es diestro con +Y arriba y +Z hacia el observador; en JavaFX +Y baja y +Z se
                            // aleja. Invertir Y y Z es un giro (no un reflejo): el modelo no queda en espejo.
                            float fx = (float) ((v[0] - cx) * k), fy = (float) (-(v[1] - baseY) * k), fz = (float) (-(v[2] - cz) * k);
                            points.add(fx, fy, fz);
                            grow(bmin, bmax, fx, fy, fz);
                            a = pointIdx.size();
                            pointIdx.put(corner[0], a);
                        }
                        faces.add(a);
                        if (normals) {
                            Integer n = normIdx.get(corner[2]);
                            if (n == null) {
                                double[] v = yUp(p.vns.get(corner[2]), up);
                                norms.add((float) v[0], (float) -v[1], (float) -v[2]);
                                n = normIdx.size();
                                normIdx.put(corner[2], n);
                            }
                            faces.add(n);
                        }
                        int tKey = corner[1] >= 0 && corner[1] < p.vts.size() ? corner[1] : -1;
                        Integer t = uvIdx.get(tKey);
                        if (t == null) {
                            double[] uv = tKey >= 0 ? p.vts.get(tKey) : new double[]{0, 0};
                            uvs.add((float) uv[0], (float) (1 - uv[1]));
                            t = uvIdx.size();
                            uvIdx.put(tKey, t);
                        }
                        faces.add(t);
                    }
                }
            }
            mesh.getPoints().setAll(points.toArray());
            if (normals) {
                mesh.getNormals().setAll(norms.toArray());
            }
            mesh.getTexCoords().setAll(uvs.toArray());
            mesh.getFaces().setAll(faces.toArray());

            Color base = Color.gray(0.72);
            PhongMaterial mat = new PhongMaterial(base);
            mat.setSpecularColor(Color.gray(0.2));
            mat.setSpecularPower(28);
            Mtl m = mtls.get(c.material);
            if (m != null) {
                base = m.kd;
                mat.setDiffuseColor(base);
                String map = m.map == null ? null : src.image(m.map);
                if (map != null) {
                    mat.setDiffuseMap(new Image(map, 0, 0, true, true, false));
                    mat.setDiffuseColor(Color.WHITE);
                }
                if (m.ks != null) {
                    // el brillo del MTL se atenua: con las tres luces del visor, a su valor integro quema los metales
                    mat.setSpecularColor(m.ks.deriveColor(0, 1, 0.55, 1));
                    mat.setSpecularPower(Math.max(8, m.ns));
                }
            }
            MeshView mv = new MeshView(mesh);
            mv.setMaterial(mat);
            mv.setCullFace(CullFace.NONE);
            mv.setUserData("base");
            root.getChildren().add(mv);
            parts.add(new Part(c.object, c.material, mv, mat, base, tris, bmin, bmax));
        }
        return new Loaded(root, p.tris, sx * k, sy * k, sz * k, List.copyOf(parts), k);
    }

    /** Pasa un punto o una normal del archivo a la convencion de trabajo (+Y arriba), sin reflejar. */
    private static double[] yUp(double[] v, Up up) {
        return up == Up.Z ? new double[]{v[0], v[2], -v[1]} : v;
    }

    private static void grow(double[] min, double[] max, double x, double y, double z) {
        min[0] = Math.min(min[0], x);
        min[1] = Math.min(min[1], y);
        min[2] = Math.min(min[2], z);
        max[0] = Math.max(max[0], x);
        max[1] = Math.max(max[1], y);
        max[2] = Math.max(max[2], z);
    }

    /* ------------------------------------------------------------------ lectura */

    /** Caras de un objeto con un mismo material. Cada cara: n esquinas x {vertice, textura, normal} (o -1). */
    private static final class Chunk {
        final String object;
        final String material;
        final List<int[][]> faces = new ArrayList<>();
        boolean normals = true;      // falso en cuanto una esquina llega sin normal

        Chunk(String object, String material) {
            this.object = object;
            this.material = material;
        }
    }

    private static final class Parsed {
        final List<double[]> vs = new ArrayList<>();
        final List<double[]> vts = new ArrayList<>();
        final List<double[]> vns = new ArrayList<>();
        final Map<String, Chunk> chunks = new LinkedHashMap<>();
        final double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        final double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        String mtlLib;
        int tris;
    }

    private static Parsed parse(Source src) throws IOException {
        Parsed p = new Parsed();
        String object = "";
        String material = "";
        boolean named = false;       // "o" manda sobre "g" cuando el archivo trae los dos
        Chunk current = null;
        try (BufferedReader r = src.open()) {
            String ln;
            boolean first = true;
            while ((ln = r.readLine()) != null) {
                ln = ln.trim();
                if (first) {
                    first = false;
                    // marca de orden de bytes de un archivo UTF-8, leida aqui como tres caracteres sueltos
                    if (ln.startsWith("ï»¿")) {
                        ln = ln.substring(3).trim();
                    }
                }
                if (ln.isEmpty() || ln.charAt(0) == '#') {
                    continue;
                }
                int sp = firstSpace(ln);
                String key = sp < 0 ? ln : ln.substring(0, sp);
                String rest = sp < 0 ? "" : ln.substring(sp + 1).trim();
                switch (key) {
                    case "v" -> {
                        String[] t = rest.split("\\s+");
                        double[] v = {Double.parseDouble(t[0]), Double.parseDouble(t[1]), Double.parseDouble(t[2])};
                        grow(p.min, p.max, v[0], v[1], v[2]);
                        p.vs.add(v);
                    }
                    case "vt" -> {
                        String[] t = rest.split("\\s+");
                        p.vts.add(new double[]{Double.parseDouble(t[0]), t.length > 1 ? Double.parseDouble(t[1]) : 0});
                    }
                    case "vn" -> {
                        String[] t = rest.split("\\s+");
                        p.vns.add(new double[]{Double.parseDouble(t[0]), Double.parseDouble(t[1]), Double.parseDouble(t[2])});
                    }
                    case "f" -> {
                        String[] t = rest.split("\\s+");
                        if (t.length < 3) {
                            continue;
                        }
                        if (current == null) {
                            String id = object + "\n" + material;
                            String o = object, m = material;
                            current = p.chunks.computeIfAbsent(id, x -> new Chunk(o, m));
                        }
                        int[][] f = new int[t.length][3];
                        for (int i = 0; i < t.length; i++) {
                            String[] c = t[i].split("/");
                            f[i][0] = index(c[0], p.vs.size());
                            f[i][1] = c.length > 1 && !c[1].isEmpty() ? index(c[1], p.vts.size()) : -1;
                            f[i][2] = c.length > 2 && !c[2].isEmpty() ? index(c[2], p.vns.size()) : -1;
                            if (f[i][2] < 0) {
                                current.normals = false;
                            }
                        }
                        current.faces.add(f);
                        p.tris += f.length - 2;
                        if (p.tris > MAX_TRIANGLES) {
                            throw new IOException("El modelo supera " + MAX_TRIANGLES + " triángulos; simplifícalo antes de importarlo.");
                        }
                    }
                    case "usemtl" -> {
                        material = rest;
                        current = null;
                    }
                    case "o" -> {
                        object = rest;
                        named = true;
                        current = null;
                    }
                    case "g" -> {
                        if (!named) {
                            object = rest;
                            current = null;
                        }
                    }
                    case "mtllib" -> p.mtlLib = rest;
                    default -> {
                        // suavizado, lineas, parametros...: no afectan a lo que se dibuja aqui
                    }
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IOException("El archivo no es un OBJ válido.");
        }
        if (p.vs.size() < 3 || p.tris == 0) {
            throw new IOException("El archivo no contiene geometría (vértices y caras) en formato OBJ.");
        }
        return p;
    }

    private static int firstSpace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t') {
                return i;
            }
        }
        return -1;
    }

    /** Indice OBJ (base 1, negativo = relativo al final) a indice base 0. */
    private static int index(String s, int size) {
        int i = Integer.parseInt(s);
        int idx = i < 0 ? size + i : i - 1;
        if (idx < 0 || idx >= size) {
            throw new NumberFormatException();
        }
        return idx;
    }

    /** Material del MTL: color difuso, brillo especular (o null si no lo declara) y nombre de la textura. */
    private record Mtl(Color kd, Color ks, double ns, String map) {
    }

    private static Map<String, Mtl> readMtl(Source src, String name) {
        Map<String, Mtl> out = new HashMap<>();
        try (BufferedReader r = src.sibling(name)) {
            if (r == null) {
                return out;
            }
            String current = null;
            Color kd = Color.gray(0.72), ks = null;
            double ns = 28;
            String map = null;
            String ln;
            while ((ln = r.readLine()) != null) {
                ln = ln.trim();
                if (ln.startsWith("newmtl ")) {
                    if (current != null) {
                        out.put(current, new Mtl(kd, ks, ns, map));
                    }
                    current = ln.substring(7).trim();
                    kd = Color.gray(0.72);
                    ks = null;
                    ns = 28;
                    map = null;
                } else if (ln.startsWith("Kd ")) {
                    kd = color(ln);
                } else if (ln.startsWith("Ks ")) {
                    ks = color(ln);
                } else if (ln.startsWith("Ns ")) {
                    ns = Double.parseDouble(ln.substring(3).trim());
                } else if (ln.startsWith("map_Kd ")) {
                    map = ln.substring(7).trim();
                }
            }
            if (current != null) {
                out.put(current, new Mtl(kd, ks, ns, map));
            }
        } catch (IOException | RuntimeException e) {
            // un MTL ilegible no impide ver la geometria: se usa el material gris por defecto
        }
        return out;
    }

    private static Color color(String line) {
        String[] t = line.split("\\s+");
        return Color.color(clamp(t[1]), clamp(t[2]), clamp(t[3]));
    }

    private static double clamp(String s) {
        return Math.max(0, Math.min(1, Double.parseDouble(s)));
    }

    /* ------------------------------------------------------------------ listas de primitivos */

    /** Lista creciente de float: la malla se rellena de una vez, no elemento a elemento. */
    private static final class Floats {
        private float[] a = new float[1024];
        private int n;

        void add(float... values) {
            if (n + values.length > a.length) {
                a = Arrays.copyOf(a, Math.max(a.length * 2, n + values.length));
            }
            System.arraycopy(values, 0, a, n, values.length);
            n += values.length;
        }

        float[] toArray() {
            return Arrays.copyOf(a, n);
        }
    }

    private static final class Ints {
        private int[] a = new int[1024];
        private int n;

        void add(int value) {
            if (n == a.length) {
                a = Arrays.copyOf(a, a.length * 2);
            }
            a[n++] = value;
        }

        int[] toArray() {
            return Arrays.copyOf(a, n);
        }
    }
}

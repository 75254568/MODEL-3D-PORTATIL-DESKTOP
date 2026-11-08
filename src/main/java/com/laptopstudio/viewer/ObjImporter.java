package com.laptopstudio.viewer;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee modelos Wavefront OBJ (+ MTL con map_Kd) empaquetados en el classpath y los convierte en MeshView.
 * El OBJ usa +Y hacia arriba y +Z hacia atras; JavaFX usa +Y hacia abajo, asi que se refleja Y
 * (y = yBase - y) y se corrige el orden de los vertices para conservar las normales hacia afuera.
 */
final class ObjImporter {

    record Result(MeshView mesh, PhongMaterial material, Image map,
                  double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        double cx() {
            return (minX + maxX) / 2;
        }

        double cz() {
            return (minZ + maxZ) / 2;
        }
    }

    private ObjImporter() {
    }

    /**
     * @param dir   carpeta del recurso (por ejemplo "/models/tuf")
     * @param file  nombre de la pieza sin extension (se busca dir/parts/file.obj)
     * @param yBase valor de Y de JavaFX que corresponde a y = 0 del OBJ (cara inferior del equipo)
     * @param center si es verdadero, la malla se centra en su propio origen (para aspas que giran)
     */
    static Result load(String dir, String file, double yBase, boolean center) {
        List<double[]> vs = new ArrayList<>();
        List<double[]> vts = new ArrayList<>();
        List<double[]> vns = new ArrayList<>();
        List<int[][]> faces = new ArrayList<>();   // cada cara: n vertices x {v, vt, vn}
        String mtlName = null;
        try (BufferedReader r = open(dir + "/parts/" + file + ".obj")) {
            String ln;
            while ((ln = r.readLine()) != null) {
                if (ln.startsWith("v ")) {
                    String[] p = ln.trim().split("\\s+");
                    vs.add(new double[]{Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])});
                } else if (ln.startsWith("vt ")) {
                    String[] p = ln.trim().split("\\s+");
                    vts.add(new double[]{Double.parseDouble(p[1]), Double.parseDouble(p[2])});
                } else if (ln.startsWith("vn ")) {
                    String[] p = ln.trim().split("\\s+");
                    vns.add(new double[]{Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3])});
                } else if (ln.startsWith("f ")) {
                    String[] p = ln.trim().split("\\s+");
                    int[][] f = new int[p.length - 1][3];
                    for (int i = 1; i < p.length; i++) {
                        String[] t = p[i].split("/");
                        f[i - 1][0] = Integer.parseInt(t[0]) - 1;
                        f[i - 1][1] = t.length > 1 && !t[1].isEmpty() ? Integer.parseInt(t[1]) - 1 : -1;
                        f[i - 1][2] = t.length > 2 && !t[2].isEmpty() ? Integer.parseInt(t[2]) - 1 : -1;
                    }
                    faces.add(f);
                } else if (ln.startsWith("mtllib")) {
                    mtlName = ln.substring(6).trim();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] v : vs) {
            minX = Math.min(minX, v[0]);
            maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
            minZ = Math.min(minZ, v[2]);
            maxZ = Math.max(maxZ, v[2]);
        }
        double ox = center ? (minX + maxX) / 2 : 0, oy = center ? (minY + maxY) / 2 : 0, oz = center ? (minZ + maxZ) / 2 : 0;
        double base = center ? 0 : yBase;

        TriangleMesh mesh = new TriangleMesh();
        Map<Long, Integer> unique = new HashMap<>();
        int[] count = {0};
        boolean flip = !Boolean.getBoolean("obj.noflip");
        for (int[][] f : faces) {
            for (int k = 1; k + 1 < f.length; k++) {
                int[][] tri = {f[0], f[k], f[k + 1]};
                if (needsFlip(tri, vs, vns)) {
                    int[] t = tri[1];
                    tri[1] = tri[2];
                    tri[2] = t;
                }
                // la reflexion de Y invierte el sentido de giro de las caras
                if (flip) {
                    int[] t = tri[1];
                    tri[1] = tri[2];
                    tri[2] = t;
                }
                for (int[] c : tri) {
                    long key = ((long) c[0] << 32) | (c[1] & 0xffffffffL);
                    Integer idx = unique.get(key);
                    if (idx == null) {
                        double[] v = vs.get(c[0]);
                        mesh.getPoints().addAll((float) (v[0] - ox), (float) (base - (v[1] - oy)), (float) (v[2] - oz));
                        double[] t = c[1] >= 0 ? vts.get(c[1]) : new double[]{0, 0};
                        mesh.getTexCoords().addAll((float) t[0], (float) (1 - t[1]));
                        idx = count[0]++;
                        unique.put(key, idx);
                    }
                    mesh.getFaces().addAll(idx, idx);
                }
            }
        }

        // material
        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        Image map = null;
        String mapName = readMapKd(dir + "/parts/" + mtlName);
        if (mapName != null) {
            String path = normalize(dir + "/parts/" + mapName);
            if (ObjImporter.class.getResource(path) != null) {
                map = new Image(ObjImporter.class.getResource(path).toExternalForm(), 0, 0, true, true, false);
                mat.setDiffuseMap(map);
            }
        }
        mat.setSpecularColor(Color.gray(0.18));
        mat.setSpecularPower(24);
        MeshView mv = new MeshView(mesh);
        mv.setMaterial(mat);
        mv.setCullFace(CullFace.NONE);
        return new Result(mv, mat, map, minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static boolean needsFlip(int[][] tri, List<double[]> vs, List<double[]> vns) {
        if (tri[0][2] < 0) {
            return false;
        }
        double[] a = vs.get(tri[0][0]), b = vs.get(tri[1][0]), c = vs.get(tri[2][0]);
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double[] n0 = vns.get(tri[0][2]);
        return nx * n0[0] + ny * n0[1] + nz * n0[2] < 0;
    }

    private static String readMapKd(String mtlPath) {
        if (mtlPath == null) {
            return null;
        }
        try (BufferedReader r = open(mtlPath)) {
            String ln;
            while ((ln = r.readLine()) != null) {
                ln = ln.trim();
                if (ln.startsWith("map_Kd")) {
                    return ln.substring(6).trim();
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return null;
    }

    private static String normalize(String p) {
        String[] parts = p.split("/");
        java.util.ArrayDeque<String> st = new java.util.ArrayDeque<>();
        for (String s : parts) {
            if (s.isEmpty() || s.equals(".")) {
                continue;
            }
            if (s.equals("..")) {
                st.pollLast();
            } else {
                st.addLast(s);
            }
        }
        return "/" + String.join("/", st);
    }

    private static BufferedReader open(String path) {
        InputStream in = ObjImporter.class.getResourceAsStream(path);
        if (in == null) {
            throw new UncheckedIOException(new IOException("No se encontro el recurso " + path));
        }
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}

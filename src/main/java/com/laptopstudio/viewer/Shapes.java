package com.laptopstudio.viewer;

import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria con forma real para las cubiertas: placas con esquinas redondeadas y bordes biselados,
 * y anillos para las paredes. Todo en milimetros, centrado en el origen, con la cara superior en
 * Y negativo (convencion de JavaFX).
 */
final class Shapes {

    /** Segmentos por esquina redondeada. */
    private static final int N = 14;

    /** Modos de coordenadas de textura sobre una cara plana. */
    static final int UV_DECK = 0;     // u = x, v invertida en z (cubierta C vista desde arriba)
    static final int UV_LID = 1;      // u invertida en x, v = z (tapa exterior)
    static final int UV_SCREEN = 2;   // u = x, v = z (pantalla)
    static final int UV_BOTTOM = 3;   // u y v invertidas (cubierta inferior)

    private Shapes() {
    }

    /** Contorno de un rectangulo con radios distintos delante (z negativo) y detras (z positivo). */
    static double[][] outline(double w, double d, double rF, double rB) {
        rF = Math.max(0.2, Math.min(rF, Math.min(w, d) / 2 - 0.1));
        rB = Math.max(0.2, Math.min(rB, Math.min(w, d) / 2 - 0.1));
        double[][] c = {
                {w / 2 - rF, -d / 2 + rF, rF, -90},
                {w / 2 - rB, d / 2 - rB, rB, 0},
                {-w / 2 + rB, d / 2 - rB, rB, 90},
                {-w / 2 + rF, -d / 2 + rF, rF, 180}};
        double[][] pts = new double[4 * (N + 1)][2];
        int k = 0;
        for (double[] corner : c) {
            for (int i = 0; i <= N; i++) {
                double a = Math.toRadians(corner[3] + 90.0 * i / N);
                pts[k][0] = corner[0] + corner[2] * Math.cos(a);
                pts[k][1] = corner[1] + corner[2] * Math.sin(a);
                k++;
            }
        }
        return pts;
    }

    private static float[] uv(int mode, double x, double z, double w, double d) {
        double nx = x / w + 0.5, nz = z / d + 0.5;
        return switch (mode) {
            case UV_DECK -> new float[]{(float) nx, (float) (1 - nz)};
            case UV_LID -> new float[]{(float) (1 - nx), (float) nz};
            case UV_SCREEN -> new float[]{(float) nx, (float) nz};
            default -> new float[]{(float) (1 - nx), (float) (1 - nz)};
        };
    }

    /** Orienta un triangulo para que su normal apunte hacia el lado indicado (convencion de JavaFX). */
    private static boolean wrong(float[] p, int a, int b, int c, double dx, double dy, double dz) {
        double ux = p[3 * b] - p[3 * a], uy = p[3 * b + 1] - p[3 * a + 1], uz = p[3 * b + 2] - p[3 * a + 2];
        double vx = p[3 * c] - p[3 * a], vy = p[3 * c + 1] - p[3 * a + 1], vz = p[3 * c + 2] - p[3 * a + 2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        return nx * dx + ny * dy + nz * dz < 0;
    }

    private static MeshView view(TriangleMesh m) {
        MeshView mv = new MeshView(m);
        mv.setCullFace(CullFace.NONE);
        return mv;
    }

    /** Cuerpo de la placa: laterales y biseles (sin las tapas planas). */
    static MeshView slabBody(double w, double d, double h, double rF, double rB, double bevel) {
        double[][] full = outline(w, d, rF, rB);
        double[][] inset = outline(w - 2 * bevel, d - 2 * bevel, rF - bevel, rB - bevel);
        int m = full.length;
        double[] ys = {h / 2, h / 2 - bevel, -h / 2 + bevel, -h / 2};
        double[][][] layers = {inset, full, full, inset};
        float[] p = new float[4 * m * 3];
        for (int l = 0; l < 4; l++) {
            for (int i = 0; i < m; i++) {
                int o = (l * m + i) * 3;
                p[o] = (float) layers[l][i][0];
                p[o + 1] = (float) ys[l];
                p[o + 2] = (float) layers[l][i][1];
            }
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0);
        List<Integer> faces = new ArrayList<>();
        List<Integer> smooth = new ArrayList<>();
        int[] group = {3, 1, 2};
        for (int l = 0; l < 3; l++) {
            for (int i = 0; i < m; i++) {
                int j = (i + 1) % m;
                int a = l * m + i, b = l * m + j, c = (l + 1) * m + j, e = (l + 1) * m + i;
                int[][] tris = {{a, b, c}, {a, c, e}};
                for (int[] t : tris) {
                    double cx = (p[3 * t[0]] + p[3 * t[1]] + p[3 * t[2]]) / 3.0;
                    double cy = (p[3 * t[0] + 1] + p[3 * t[1] + 1] + p[3 * t[2] + 1]) / 3.0;
                    double cz = (p[3 * t[0] + 2] + p[3 * t[1] + 2] + p[3 * t[2] + 2]) / 3.0;
                    int[] u = t;
                    if (wrong(p, t[0], t[1], t[2], cx, cy, cz)) {
                        u = new int[]{t[0], t[2], t[1]};
                    }
                    faces.add(u[0]);
                    faces.add(0);
                    faces.add(u[1]);
                    faces.add(0);
                    faces.add(u[2]);
                    faces.add(0);
                    smooth.add(group[l]);
                }
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(smooth.stream().mapToInt(Integer::intValue).toArray());
        return view(mesh);
    }

    /** Tapa plana (arriba o abajo) con las coordenadas de textura indicadas. */
    static MeshView slabCap(double w, double d, double h, double rF, double rB, double bevel, boolean top, int uvMode) {
        double[][] o = outline(w - 2 * bevel, d - 2 * bevel, rF - bevel, rB - bevel);
        int m = o.length;
        float y = (float) (top ? -h / 2 : h / 2);
        float[] p = new float[(m + 1) * 3];
        TriangleMesh mesh = new TriangleMesh();
        for (int i = 0; i < m; i++) {
            p[3 * i] = (float) o[i][0];
            p[3 * i + 1] = y;
            p[3 * i + 2] = (float) o[i][1];
            float[] t = uv(uvMode, o[i][0], o[i][1], w, d);
            mesh.getTexCoords().addAll(t[0], t[1]);
        }
        p[3 * m] = 0;
        p[3 * m + 1] = y;
        p[3 * m + 2] = 0;
        float[] tc = uv(uvMode, 0, 0, w, d);
        mesh.getTexCoords().addAll(tc[0], tc[1]);
        mesh.getPoints().addAll(p);
        List<Integer> faces = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            int[] t = {m, i, j};
            if (wrong(p, t[0], t[1], t[2], 0, top ? -1 : 1, 0)) {
                t = new int[]{m, j, i};
            }
            for (int v : t) {
                faces.add(v);
                faces.add(v);
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(new int[faces.size() / 6]);
        return view(mesh);
    }

    /** Pared hueca con esquinas redondeadas (caras exterior, interior y borde superior). */
    static MeshView ring(double w, double d, double h, double rF, double rB, double wall) {
        double[][] outer = outline(w, d, rF, rB);
        double[][] inner = outline(w - 2 * wall, d - 2 * wall, rF - wall, rB - wall);
        int m = outer.length;
        float[] p = new float[4 * m * 3];
        double yt = -h / 2, yb = h / 2;
        for (int i = 0; i < m; i++) {
            put(p, 0 * m + i, outer[i][0], yt, outer[i][1]);
            put(p, 1 * m + i, outer[i][0], yb, outer[i][1]);
            put(p, 2 * m + i, inner[i][0], yt, inner[i][1]);
            put(p, 3 * m + i, inner[i][0], yb, inner[i][1]);
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0);
        List<Integer> faces = new ArrayList<>();
        List<Integer> smooth = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            // {a,b,c,e, direccion deseada: 1 = hacia fuera, -1 = hacia dentro, 0 = arriba}
            int[][] quads = {
                    {i, j, m + j, m + i, 1, 1},
                    {2 * m + i, 2 * m + j, 3 * m + j, 3 * m + i, -1, 2},
                    {i, j, 2 * m + j, 2 * m + i, 0, 3}};
            for (int[] q : quads) {
                int[][] tris = {{q[0], q[1], q[2]}, {q[0], q[2], q[3]}};
                for (int[] t : tris) {
                    double cx = (p[3 * t[0]] + p[3 * t[1]] + p[3 * t[2]]) / 3.0;
                    double cz = (p[3 * t[0] + 2] + p[3 * t[1] + 2] + p[3 * t[2] + 2]) / 3.0;
                    double dx = q[4] == 0 ? 0 : q[4] * cx, dy = q[4] == 0 ? -1 : 0, dz = q[4] == 0 ? 0 : q[4] * cz;
                    int[] u = t;
                    if (wrong(p, t[0], t[1], t[2], dx, dy, dz)) {
                        u = new int[]{t[0], t[2], t[1]};
                    }
                    for (int v : u) {
                        faces.add(v);
                        faces.add(0);
                    }
                    smooth.add(q[5]);
                }
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(smooth.stream().mapToInt(Integer::intValue).toArray());
        return view(mesh);
    }


    /** Tecla con paredes inclinadas: base (w x d) y cara superior reducida por taper en cada lado. Solo las paredes. */
    static MeshView keycapBody(double w, double d, double h, double taper, double r) {
        double[][] base = outline(w, d, r, r);
        double[][] top = outline(w - 2 * taper, d - 2 * taper, Math.max(0.3, r - taper), Math.max(0.3, r - taper));
        int m = base.length;
        float[] p = new float[2 * m * 3];
        for (int i = 0; i < m; i++) {
            put(p, i, base[i][0], h / 2, base[i][1]);
            put(p, m + i, top[i][0], -h / 2, top[i][1]);
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0);
        List<Integer> faces = new ArrayList<>();
        List<Integer> smooth = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            int[][] tris = {{i, j, m + j}, {i, m + j, m + i}};
            for (int[] t : tris) {
                double cx = (p[3 * t[0]] + p[3 * t[1]] + p[3 * t[2]]) / 3.0;
                double cz = (p[3 * t[0] + 2] + p[3 * t[1] + 2] + p[3 * t[2] + 2]) / 3.0;
                int[] u = t;
                if (wrong(p, t[0], t[1], t[2], cx, 0, cz)) {
                    u = new int[]{t[0], t[2], t[1]};
                }
                for (int v : u) {
                    faces.add(v);
                    faces.add(0);
                }
                smooth.add(1);
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(smooth.stream().mapToInt(Integer::intValue).toArray());
        return view(mesh);
    }

    /** Cara superior de la tecla: la imagen completa se ajusta al contorno reducido (v de arriba = parte trasera). */
    static MeshView keycapTop(double w, double d, double h, double taper, double r) {
        double tw = w - 2 * taper, td = d - 2 * taper;
        double[][] o = outline(tw, td, Math.max(0.3, r - taper), Math.max(0.3, r - taper));
        int m = o.length;
        float y = (float) (-h / 2);
        float[] p = new float[(m + 1) * 3];
        TriangleMesh mesh = new TriangleMesh();
        for (int i = 0; i < m; i++) {
            p[3 * i] = (float) o[i][0];
            p[3 * i + 1] = y;
            p[3 * i + 2] = (float) o[i][1];
            mesh.getTexCoords().addAll((float) (o[i][0] / tw + 0.5), (float) (1 - (o[i][1] / td + 0.5)));
        }
        p[3 * m] = 0;
        p[3 * m + 1] = y;
        p[3 * m + 2] = 0;
        mesh.getTexCoords().addAll(0.5f, 0.5f);
        mesh.getPoints().addAll(p);
        List<Integer> faces = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            int[] t = {m, i, j};
            if (wrong(p, t[0], t[1], t[2], 0, -1, 0)) {
                t = new int[]{m, j, i};
            }
            for (int v : t) {
                faces.add(v);
                faces.add(v);
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(new int[faces.size() / 6]);
        return view(mesh);
    }

    /** Prisma de un poligono convexo (x,z) con altura h, centrado en Y. */
    static MeshView prism(double[][] poly, double h) {
        int m = poly.length;
        float[] p = new float[(2 * m + 2) * 3];
        double cx = 0, cz = 0;
        for (double[] q : poly) {
            cx += q[0] / m;
            cz += q[1] / m;
        }
        for (int i = 0; i < m; i++) {
            put(p, i, poly[i][0], -h / 2, poly[i][1]);
            put(p, m + i, poly[i][0], h / 2, poly[i][1]);
        }
        put(p, 2 * m, cx, -h / 2, cz);
        put(p, 2 * m + 1, cx, h / 2, cz);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0);
        List<Integer> faces = new ArrayList<>();
        List<Integer> smooth = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int j = (i + 1) % m;
            int[][] tris = {{i, j, m + j}, {i, m + j, m + i}, {2 * m, i, j}, {2 * m + 1, m + i, m + j}};
            double[][] dir = {{p[3 * i] - cx, 0, p[3 * i + 2] - cz}, {p[3 * i] - cx, 0, p[3 * i + 2] - cz}, {0, -1, 0}, {0, 1, 0}};
            for (int k = 0; k < 4; k++) {
                int[] t = tris[k];
                int[] u = wrong(p, t[0], t[1], t[2], dir[k][0], dir[k][1], dir[k][2]) ? new int[]{t[0], t[2], t[1]} : t;
                for (int v : u) {
                    faces.add(v);
                    faces.add(0);
                }
                smooth.add(k < 2 ? 1 : 2 + k);
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(smooth.stream().mapToInt(Integer::intValue).toArray());
        return view(mesh);
    }

    /** Toro (anillo) en el plano XZ centrado en el origen: radio mayor R y radio del tubo r. */
    static MeshView torus(double R, double r, int seg, int ring) {
        float[] p = new float[seg * ring * 3];
        for (int i = 0; i < seg; i++) {
            double a = 2 * Math.PI * i / seg;
            for (int j = 0; j < ring; j++) {
                double b = 2 * Math.PI * j / ring;
                double rr = R + r * Math.cos(b);
                put(p, i * ring + j, rr * Math.cos(a), -r * Math.sin(b), rr * Math.sin(a));
            }
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(p);
        mesh.getTexCoords().addAll(0, 0);
        List<Integer> faces = new ArrayList<>();
        List<Integer> smooth = new ArrayList<>();
        for (int i = 0; i < seg; i++) {
            int i2 = (i + 1) % seg;
            double a = 2 * Math.PI * (i + 0.5) / seg;
            for (int j = 0; j < ring; j++) {
                int j2 = (j + 1) % ring;
                int[][] tris = {{i * ring + j, i2 * ring + j, i2 * ring + j2}, {i * ring + j, i2 * ring + j2, i * ring + j2}};
                for (int[] t : tris) {
                    double cx = (p[3 * t[0]] + p[3 * t[1]] + p[3 * t[2]]) / 3.0 - R * Math.cos(a);
                    double cy = (p[3 * t[0] + 1] + p[3 * t[1] + 1] + p[3 * t[2] + 1]) / 3.0;
                    double cz = (p[3 * t[0] + 2] + p[3 * t[1] + 2] + p[3 * t[2] + 2]) / 3.0 - R * Math.sin(a);
                    int[] u = wrong(p, t[0], t[1], t[2], cx, cy, cz) ? new int[]{t[0], t[2], t[1]} : t;
                    for (int v : u) {
                        faces.add(v);
                        faces.add(0);
                    }
                    smooth.add(1);
                }
            }
        }
        mesh.getFaces().addAll(faces.stream().mapToInt(Integer::intValue).toArray());
        mesh.getFaceSmoothingGroups().addAll(smooth.stream().mapToInt(Integer::intValue).toArray());
        return view(mesh);
    }

    private static void put(float[] p, int idx, double x, double y, double z) {
        p[3 * idx] = (float) x;
        p[3 * idx + 1] = (float) y;
        p[3 * idx + 2] = (float) z;
    }
}

package com.laptopexplorer.viewer;

import com.laptopexplorer.model.AMJLaptop;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

/**
 * "Foto de producto" de un laptop a partir de su propio modelo 3D: abierto, visto de frente y algo desde arriba,
 * sobre fondo transparente. Es la imagen de las tarjetas del catálogo, así que cada tarjeta enseña el mismo
 * diseño que se ve después en el visor. Construye el modelo, de modo que debe llamarse en el hilo de JavaFX.
 */
public final class AMJLaptopShot {

    private static final double FIELD_OF_VIEW = 30;
    private static final double LID_OPEN = 108;

    private AMJLaptopShot() {
    }

    /** La imagen se dibuja al doble del tamaño pedido para que, al reducirla, los bordes queden suaves. */
    public static Image amjRender(AMJLaptop laptop, int width, int height) {
        // El relieve que el visor saca de las fotos no se aprecia a este tamaño y es lo que más tarda en calcularse:
        // se desactiva mientras se construye este modelo (todo ocurre en el hilo de JavaFX, sin otro modelo a medias).
        double relief = AMJLaptopModel.reliefStrength;
        AMJLaptopModel model;
        try {
            AMJLaptopModel.reliefStrength = 0;
            model = new AMJLaptopModel(laptop, AMJLaptopViewer.ACCENT_A, null, true);
        } finally {
            AMJLaptopModel.reliefStrength = relief;
        }
        model.amjSetLid(LID_OPEN);
        double w = model.amjWidth(), d = model.amjDepth();
        // abierta, la tapa sube casi tanto como mide el fondo y se echa algo hacia atrás de la bisagra
        double back = d * Math.max(0, -Math.cos(Math.toRadians(LID_OPEN)));
        double tall = d * Math.sin(Math.toRadians(LID_OPEN));
        Group hold = new Group(model.amjGetRoot());
        // algo más arriba del centro: visto desde arriba, el canto delantero de la base es lo que más baja en la imagen
        hold.setTranslateY(model.amjCenterShift() - tall * 0.05);
        hold.setTranslateZ(-back / 2);
        Group turn = new Group(hold);
        turn.getTransforms().addAll(new Rotate(13, Rotate.X_AXIS), new Rotate(-24, Rotate.Y_AXIS));

        // luz suave de estudio: las texturas ya traen sus sombras y solo hay que iluminar la cubierta, vista casi de canto
        AmbientLight ambient = new AmbientLight(Color.gray(0.56));
        PointLight key = new PointLight(Color.gray(0.6));
        key.setTranslateX(-700);
        key.setTranslateY(-1100);
        key.setTranslateZ(-1100);
        PointLight fill = new PointLight(Color.gray(0.2));
        fill.setTranslateX(900);
        fill.setTranslateY(-300);
        fill.setTranslateZ(-500);
        Group root = new Group(turn, ambient, key, fill);

        // Primero una toma holgada, en la que el equipo cabe seguro sea cual sea su forma (la esfera que lo envuelve
        // entra entera en un lienzo cuadrado); después se recorta a su contorno real y se escala para llenar la
        // imagen. Así todos los laptops se ven igual de grandes, sean de 13 o de 18 pulgadas.
        int side = (int) Math.round(Math.max(width, height) * 2.0 * 1.15);
        double radius = Math.sqrt(w * w + (d + back) * (d + back) + tall * tall) / 2;
        double distance = 1.05 * radius / Math.sin(Math.toRadians(FIELD_OF_VIEW / 2));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FIELD_OF_VIEW);
        camera.setTranslateZ(-distance);
        camera.setNearClip(Math.max(2, distance / 300));
        camera.setFarClip(distance * 8);

        // una captura de un nodo suelto ignora sus luces; dentro de una escena se aplican
        Scene scene = new Scene(root, side, side, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        scene.setCamera(camera);
        WritableImage raw = scene.snapshot(null);
        return amjFit(raw, width * 2, height * 2);
    }

    /**
     * Recorta la toma a lo que no es transparente y la escala, sin deformarla, para que ocupe todo el ancho o
     * todo el alto de la imagen final (lo que se alcance antes), centrada y apoyada abajo como en una foto de tienda.
     */
    private static Image amjFit(WritableImage raw, int outW, int outH) {
        int w = (int) raw.getWidth(), h = (int) raw.getHeight();
        int[] px = new int[w * h];
        raw.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) > 10) {
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    y0 = Math.min(y0, y);
                    y1 = Math.max(y1, y);
                }
            }
        }
        if (x1 < x0 || y1 < y0) {
            return raw;                              // toma vacía: se devuelve tal cual
        }
        double cw = x1 - x0 + 1, ch = y1 - y0 + 1;
        double pad = outW * 0.03;
        double k = Math.min((outW - 2 * pad) / cw, (outH - pad) / ch);
        double dw = cw * k, dh = ch * k;
        Canvas canvas = new Canvas(outW, outH);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(true);
        g.drawImage(raw, x0, y0, cw, ch, (outW - dw) / 2, outH - dh - pad * 0.4, dw, dh);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return canvas.snapshot(sp, null);
    }
}

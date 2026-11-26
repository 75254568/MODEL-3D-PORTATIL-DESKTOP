package com.laptopexplorer.ui;

import com.laptopexplorer.viewer.AMJLaptopViewer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Prueba de interacción real con el ratón y el teclado (con -Dexplorer.interact=true).
 * Mueve el cursor del sistema, así que solo se usa para verificar el visor 3D.
 */
public final class AMJInteractionCheck {

    private AMJInteractionCheck() {
    }

    public static boolean amjEnabled() {
        return Boolean.getBoolean("explorer.interact");
    }

    private static final class AMJSeq {
        final List<Supplier<Integer>> steps = new ArrayList<>();
        int i = 0;

        void amjAdd(Runnable r, int waitMs) {
            steps.add(() -> {
                r.run();
                return waitMs;
            });
        }

        void amjRun() {
            if (i >= steps.size()) {
                System.out.println("INTERACT terminado");
                Platform.exit();
                return;
            }
            int wait;
            try {
                wait = steps.get(i++).get();
            } catch (Throwable t) {
                System.out.println("INTERACT excepción: " + t);
                wait = 100;
            }
            PauseTransition p = new PauseTransition(Duration.millis(wait));
            p.setOnFinished(e -> amjRun());
            p.play();
        }
    }

    private static int[] amjPixels(Scene scene, Rectangle2D r) {
        WritableImage img = scene.getRoot().snapshot(null, null);
        PixelReader pr = img.getPixelReader();
        int w = (int) r.getWidth(), h = (int) r.getHeight();
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out[y * w + x] = pr.getArgb((int) r.getMinX() + x, (int) r.getMinY() + y);
            }
        }
        return out;
    }

    private static int amjDiff(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                n++;
            }
        }
        return n;
    }

    private static List<AMJLaptopViewer> amjViewers(Scene scene) {
        List<AMJLaptopViewer> out = new ArrayList<>();
        for (Node n : scene.getRoot().lookupAll("*")) {
            if (n instanceof AMJLaptopViewer v) {
                out.add(v);
            }
        }
        return out;
    }

    private static javafx.scene.input.MouseEvent amjMouse(javafx.event.EventType<javafx.scene.input.MouseEvent> type,
                                                       double x, double y, boolean down, Node pickNode) {
        javafx.scene.input.PickResult pr = pickNode == null ? null
                : new javafx.scene.input.PickResult(pickNode, new javafx.geometry.Point3D(x, y, 0), 1.0);
        return new javafx.scene.input.MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false,
                down, false, false, true, false, true, pr);
    }

    private static Node amjFindPart(AMJLaptopViewer v, String part) {
        for (Node n : v.amjGetSubScene().getRoot().lookupAll("*")) {
            if (part.equals(n.getUserData())) {
                return n;
            }
        }
        return null;
    }

    private static void amjSend(AMJLaptopViewer v, javafx.event.Event e) {
        javafx.event.Event.fireEvent(v.amjGetSubScene(), e);
    }

    public static void amjRun(Scene scene, AMJNavigationService nav) {
        AMJSeq seq = new AMJSeq();
        Rectangle2D regionA = new Rectangle2D(310, 175, 400, 640);
        Rectangle2D regionB = new Rectangle2D(735, 175, 400, 640);
        int[][] snap = new int[6][];
        var MOVED = javafx.scene.input.MouseEvent.MOUSE_MOVED;
        var DRAGGED = javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
        var PRESSED = javafx.scene.input.MouseEvent.MOUSE_PRESSED;
        var RELEASED = javafx.scene.input.MouseEvent.MOUSE_RELEASED;
        var EXITED = javafx.scene.input.MouseEvent.MOUSE_EXITED;

        seq.amjAdd(() -> nav.amjGo(AMJScreen.VISOR), 6000);
        seq.amjAdd(() -> {
            System.out.println("INTERACT visores encontrados: " + amjViewers(scene).size());
            snap[0] = amjPixels(scene, regionA);
            snap[1] = amjPixels(scene, regionB);
        }, 100);

        // 1. hover sobre A: solo A debe moverse
        seq.amjAdd(() -> {
            AMJLaptopViewer a = amjViewers(scene).get(0);
            amjSend(a, amjMouse(MOVED, a.getWidth() * 0.92, a.getHeight() * 0.20, false, null));
        }, 1500);
        seq.amjAdd(() -> {
            int dA = amjDiff(snap[0], amjPixels(scene, regionA));
            int dB = amjDiff(snap[1], amjPixels(scene, regionB));
            System.out.println("INTERACT hover en A -> píxeles cambiados A=" + dA + " B=" + dB
                    + (dA > 2000 && dB < 300 ? "  OK (solo A se mueve)" : "  REVISAR"));
        }, 100);

        // 2. salir del visor: A vuelve al reposo
        seq.amjAdd(() -> amjSend(amjViewers(scene).get(0), amjMouse(EXITED, 5, 5, false, null)), 1800);
        seq.amjAdd(() -> {
            int dA = amjDiff(snap[0], amjPixels(scene, regionA));
            System.out.println("INTERACT al salir -> A vuelve al reposo, píxeles distintos=" + dA
                    + (dA < 300 ? "  OK" : "  REVISAR"));
            snap[2] = amjPixels(scene, regionA);
            snap[3] = amjPixels(scene, regionB);
        }, 100);

        // 3. arrastrar en B: solo B rota
        seq.amjAdd(() -> {
            AMJLaptopViewer b = amjViewers(scene).get(1);
            amjSend(b, amjMouse(PRESSED, b.getWidth() * 0.5, b.getHeight() * 0.5, true, null));
        }, 100);
        for (int k = 1; k <= 8; k++) {
            final int kk = k;
            seq.amjAdd(() -> {
                AMJLaptopViewer b = amjViewers(scene).get(1);
                amjSend(b, amjMouse(DRAGGED, b.getWidth() * 0.5 + kk * 25, b.getHeight() * 0.5, true, null));
            }, 60);
        }
        seq.amjAdd(() -> {
            AMJLaptopViewer b = amjViewers(scene).get(1);
            amjSend(b, amjMouse(RELEASED, b.getWidth() * 0.5 + 200, b.getHeight() * 0.5, false, null));
            amjSend(b, amjMouse(EXITED, 5, 5, false, null));
        }, 1800);
        seq.amjAdd(() -> {
            int dA = amjDiff(snap[2], amjPixels(scene, regionA));
            int dB = amjDiff(snap[3], amjPixels(scene, regionB));
            System.out.println("INTERACT arrastre en B -> píxeles cambiados A=" + dA + " B=" + dB
                    + (dB > 5000 && dA < 300 ? "  OK (solo B rota)" : "  REVISAR"));
            snap[4] = amjPixels(scene, regionB);
        }, 100);

        // 4. rueda: zoom solo en B
        seq.amjAdd(() -> {
            AMJLaptopViewer b = amjViewers(scene).get(1);
            amjSend(b, new javafx.scene.input.ScrollEvent(javafx.scene.input.ScrollEvent.SCROLL, 300, 300, 300, 300,
                    false, false, false, false, false, false, 0, 120, 0, 120,
                    javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
        }, 1200);
        seq.amjAdd(() -> {
            int dB = amjDiff(snap[4], amjPixels(scene, regionB));
            int dA = amjDiff(snap[2], amjPixels(scene, regionA));
            System.out.println("INTERACT rueda en B -> píxeles cambiados B=" + dB + " A=" + dA
                    + (dB > 5000 && dA < 300 ? "  OK (zoom independiente)" : "  REVISAR"));
        }, 100);

        // 5. teclado: la tecla se ilumina en el teclado del modelo
        seq.amjAdd(() -> {
            snap[5] = amjPixels(scene, regionA);
            javafx.event.Event.fireEvent(scene.getRoot(), new javafx.scene.input.KeyEvent(
                    javafx.scene.input.KeyEvent.KEY_PRESSED, "g", "g", KeyCode.G, false, false, false, false));
        }, 400);
        seq.amjAdd(() -> {
            int dA = amjDiff(snap[5], amjPixels(scene, regionA));
            javafx.event.Event.fireEvent(scene.getRoot(), new javafx.scene.input.KeyEvent(
                    javafx.scene.input.KeyEvent.KEY_RELEASED, "g", "g", KeyCode.G, false, false, false, false));
            System.out.println("INTERACT tecla G -> píxeles cambiados en A=" + dA + (dA > 30 ? "  OK (tecla iluminada)" : "  REVISAR"));
        }, 300);

        // 6. clic en una pieza (cubierta C) y en la tapa (pantalla)
        for (String part : new String[]{"deck", "display"}) {
            seq.amjAdd(() -> {
                AMJLaptopViewer a = amjViewers(scene).get(0);
                Node n = amjFindPart(a, part);
                System.out.println("INTERACT nodo '" + part + "' encontrado: " + (n != null));
                amjSend(a, amjMouse(PRESSED, 300, 300, true, n));
                amjSend(a, amjMouse(RELEASED, 300, 300, false, n));
            }, 700);
            seq.amjAdd(() -> {
                Node tp = scene.getRoot().lookup(".part-list .list-cell:selected");
                boolean open = tp != null;
                System.out.println("INTERACT clic en '" + part + "' -> pieza elegida en la lista=" + open + (open ? "  OK" : "  REVISAR"));
            }, 100);
        }
        seq.amjRun();
    }
}

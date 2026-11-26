package com.laptopexplorer.component;

import com.laptopexplorer.component.AMJSpecDoc.AMJBlock;
import com.laptopexplorer.component.AMJSpecDoc.AMJBullets;
import com.laptopexplorer.component.AMJSpecDoc.AMJItem;
import com.laptopexplorer.component.AMJSpecDoc.AMJNote;
import com.laptopexplorer.component.AMJSpecDoc.AMJParagraph;
import com.laptopexplorer.component.AMJSpecDoc.AMJRich;
import com.laptopexplorer.component.AMJSpecDoc.AMJRun;
import com.laptopexplorer.component.AMJSpecDoc.AMJSection;
import com.laptopexplorer.component.AMJSpecDoc.AMJSubheading;
import com.laptopexplorer.component.AMJSpecDoc.AMJTable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte la ficha en Markdown que acompaña a cada modelo en una {@link AMJSpecDoc}.
 * Las fichas vienen escritas para un visor de documentos: llevan fórmulas entre signos de dólar, entidades HTML,
 * diagramas de texto y una lista de archivos con rutas del equipo donde se generaron. Aquí se deja solo el
 * contenido: unidades y símbolos escritos como texto, sin las secciones de archivos y sin los diagramas de caracteres.
 */
public final class AMJSpecMarkdown {

    private static final Pattern BULLET = Pattern.compile("^(\\s*)([*+-]|\\d+[.)])\\s+(.*)$");
    private static final Pattern NUMBERED_TITLE = Pattern.compile("^\\d+[.)]\\s+.*");
    private static final Pattern TABLE_RULE = Pattern.compile("^:?-{2,}:?$");
    private static final Pattern MATH = Pattern.compile("\\$([^$]*)\\$");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]*\\)");
    private static final Pattern MERMAID_LABEL = Pattern.compile("\\[\"([^\"]+)\"]");
    private static final Pattern BROKEN_NUMBER = Pattern.compile("(^|[\\s(])[.,]\\d");

    private AMJSpecMarkdown() {
    }

    public static AMJSpecDoc amjParse(String markdown) {
        String[] lines = markdown.replace("\uFEFF", "").split("\\r?\\n");
        String heading = null;
        boolean subtitleTaken = false;      // la línea bajo el título ya se uso (o el título salio de un "##")
        boolean inSections = false;
        List<String> highlights = new ArrayList<>();
        List<AMJSection> sections = new ArrayList<>();
        String title = null;
        List<String> body = new ArrayList<>();

        for (String raw : lines) {
            String t = raw.trim();
            int level = amjHeadingLevel(t);
            if (level == 1 && !inSections) {
                // "FICHA TÉCNICA DE INGENIERIA: <nombre>": el nombre va tras los dos puntos; un título genérico se ignora
                String text = amjClean(t.substring(1));
                int colon = text.indexOf(':');
                if (colon > 0 && colon < text.length() - 1) {
                    heading = text.substring(colon + 1).trim();
                }
                continue;
            }
            if (level == 2) {
                String text = t.substring(2).trim();
                boolean numbered = NUMBERED_TITLE.matcher(text).matches();
                if (!numbered && !inSections && heading == null) {
                    heading = amjClean(text);
                    subtitleTaken = true;
                    continue;
                }
                if (!numbered && !inSections && !subtitleTaken) {
                    highlights.add(amjClean(text));
                    subtitleTaken = true;
                    continue;
                }
                if (title != null) {
                    amjAddSection(sections, title, body);
                }
                inSections = true;
                title = amjClean(text.replaceFirst("^\\d+[.)]\\s+", ""));
                body = new ArrayList<>();
                continue;
            }
            if (level >= 3 && !inSections) {
                for (String part : amjClean(t.substring(level)).split("\\s*•\\s*")) {
                    if (!part.isBlank()) {
                        highlights.add(part.trim());
                    }
                }
                continue;
            }
            if (inSections) {
                body.add(raw);
            }
        }
        if (title != null) {
            amjAddSection(sections, title, body);
        }
        return new AMJSpecDoc(heading, List.copyOf(highlights), List.copyOf(sections));
    }

    private static int amjHeadingLevel(String t) {
        int n = 0;
        while (n < t.length() && t.charAt(n) == '#') {
            n++;
        }
        return n > 0 && n < t.length() && t.charAt(n) == ' ' ? n : 0;
    }

    private static void amjAddSection(List<AMJSection> into, String title, List<String> body) {
        // inventario de archivos de la carpeta original: son rutas de otro equipo, no datos del componente
        if (title.toUpperCase(Locale.ROOT).contains("ARCHIVOS")) {
            return;
        }
        List<AMJBlock> blocks = amjBlocks(body);
        if (!blocks.isEmpty()) {
            into.add(new AMJSection(title, blocks));
        }
    }

    /* ------------------------------------------------------------------ bloques */

    private static List<AMJBlock> amjBlocks(List<String> lines) {
        List<AMJBlock> out = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String raw = lines.get(i);
            String t = raw.trim();
            if (t.isEmpty() || t.matches("^-{3,}$") || t.contains("file:///")) {
                i++;
            } else if (t.startsWith("```")) {
                boolean mermaid = t.toLowerCase(Locale.ROOT).startsWith("```mermaid");
                List<String> code = new ArrayList<>();
                i++;
                while (i < lines.size() && !lines.get(i).trim().startsWith("```")) {
                    code.add(lines.get(i++));
                }
                i++;
                if (mermaid) {
                    // del diagrama de flujo se conservan los rótulos de sus cajas, en orden, como lista
                    Set<String> labels = new LinkedHashSet<>();
                    for (String c : code) {
                        Matcher m = MERMAID_LABEL.matcher(c);
                        while (m.find()) {
                            labels.add(m.group(1));
                        }
                    }
                    List<AMJItem> items = new ArrayList<>();
                    labels.forEach(l -> items.add(new AMJItem(0, "•", amjRich(l))));
                    if (!items.isEmpty()) {
                        out.add(new AMJBullets(items));
                    }
                }
                // un dibujo hecho con caracteres no se puede leer en un panel estrecho: el modelo 3D lo sustituye
            } else if (amjHeadingLevel(t) >= 3) {
                out.add(new AMJSubheading(amjClean(t.substring(amjHeadingLevel(t)))));
                i++;
            } else if (t.startsWith("|")) {
                List<String> rows = new ArrayList<>();
                while (i < lines.size() && lines.get(i).trim().startsWith("|")) {
                    rows.add(lines.get(i++).trim());
                }
                AMJTable table = amjTable(rows);
                if (table != null) {
                    out.add(table);
                }
            } else if (t.startsWith(">")) {
                List<String> quoted = new ArrayList<>();
                while (i < lines.size() && lines.get(i).trim().startsWith(">")) {
                    String q = lines.get(i++).trim().substring(1);
                    q = q.startsWith(" ") ? q.substring(1) : q;
                    if (!q.trim().matches("^\\[![A-Za-z]+]$")) {
                        quoted.add(q);
                    }
                }
                List<AMJBlock> inner = amjBlocks(quoted);
                if (!inner.isEmpty()) {
                    out.add(new AMJNote(inner));
                }
            } else if (BULLET.matcher(raw).matches()) {
                List<AMJItem> items = new ArrayList<>();
                while (i < lines.size()) {
                    String line = lines.get(i);
                    Matcher m = BULLET.matcher(line);
                    if (m.matches()) {
                        if (!line.contains("file:///")) {
                            String marker = Character.isDigit(m.group(2).charAt(0)) ? m.group(2).replace(')', '.') : "•";
                            items.add(new AMJItem(amjIndent(m.group(1)) / 2, marker, amjRich(m.group(3))));
                        }
                        i++;
                    } else if (!line.isBlank() && Character.isWhitespace(line.charAt(0)) && !items.isEmpty()
                            && !line.trim().startsWith("|") && !line.trim().startsWith("```")) {
                        // línea de continuación del elemento anterior
                        AMJItem last = items.remove(items.size() - 1);
                        items.add(new AMJItem(last.level(), last.marker(), amjJoin(last.text(), amjRich(line.trim()))));
                        i++;
                    } else {
                        break;
                    }
                }
                if (!items.isEmpty()) {
                    out.add(new AMJBullets(items));
                }
            } else {
                StringBuilder sb = new StringBuilder(t);
                i++;
                while (i < lines.size() && amjIsPlain(lines.get(i))) {
                    sb.append(' ').append(lines.get(i++).trim());
                }
                out.add(new AMJParagraph(amjRich(sb.toString())));
            }
        }
        return out;
    }

    private static boolean amjIsPlain(String line) {
        String t = line.trim();
        return !t.isEmpty() && amjHeadingLevel(t) == 0 && !t.startsWith("|") && !t.startsWith(">") && !t.startsWith("```")
                && !t.matches("^-{3,}$") && !BULLET.matcher(line).matches() && !t.contains("file:///");
    }

    private static int amjIndent(String spaces) {
        int n = 0;
        for (char c : spaces.toCharArray()) {
            n += c == '\t' ? 2 : 1;
        }
        return n;
    }

    private static AMJRich amjJoin(AMJRich a, AMJRich b) {
        List<AMJRun> runs = new ArrayList<>(a.runs());
        runs.add(new AMJRun(" ", false));
        runs.addAll(b.runs());
        return new AMJRich(runs, a.incomplete() || b.incomplete());
    }

    private static AMJTable amjTable(List<String> rows) {
        List<String> headers = null;
        List<List<AMJRich>> data = new ArrayList<>();
        for (String row : rows) {
            String inner = row.substring(1, row.endsWith("|") && row.length() > 1 ? row.length() - 1 : row.length());
            String[] cells = inner.split("\\|", -1);
            boolean rule = true;
            for (String c : cells) {
                rule &= TABLE_RULE.matcher(c.trim()).matches();
            }
            if (rule) {
                continue;
            }
            if (headers == null) {
                headers = new ArrayList<>();
                for (String c : cells) {
                    headers.add(amjRich(c).amjPlain());
                }
                continue;
            }
            List<AMJRich> line = new ArrayList<>();
            for (String c : cells) {
                line.add(amjRich(c));
            }
            data.add(line);
        }
        return headers == null || data.isEmpty() ? null : new AMJTable(headers, data);
    }

    /* ------------------------------------------------------------------ texto */

    /** Texto limpio y partido en tramos normales y resaltados. */
    static AMJRich amjRich(String raw) {
        String s = amjClean(raw, true);
        // Algunas fichas perdieron cifras al generarse ("$357.6" quedo en ".6"). Se nota en que sobra un signo de
        // dólar (las fórmulas van entre dos) o en que un número empieza por el separador decimal.
        boolean incomplete = raw.chars().filter(c -> c == '$').count() % 2 == 1 || BROKEN_NUMBER.matcher(s).find();
        List<AMJRun> runs = new ArrayList<>();
        Matcher m = BOLD.matcher(s);
        int from = 0;
        while (m.find()) {
            if (m.start() > from) {
                runs.add(new AMJRun(s.substring(from, m.start()), false));
            }
            runs.add(new AMJRun(m.group(1), true));
            from = m.end();
        }
        if (from < s.length()) {
            runs.add(new AMJRun(s.substring(from).replace("**", ""), false));
        }
        return new AMJRich(List.copyOf(runs), incomplete);
    }

    /** Texto limpio sin marcas de resaltado. */
    static String amjClean(String raw) {
        return amjClean(raw, false).replace("**", "");
    }

    private static String amjClean(String raw, boolean keepBold) {
        String s = raw.replace("&bull;", "•").replace("&nbsp;", " ").replace("&amp;", "&").replace("&deg;", "°");
        // fórmulas completas: se traducen por dentro, donde "_" y "^" son subindice y superindice
        Matcher m = MATH.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(amjMath(m.group(1), true)));
        }
        m.appendTail(sb);
        // lo que quede fuera de una fórmula completa (una a medias) se traduce sin tocar guiones bajos del texto
        s = amjMath(sb.toString(), false).replace("$", "");
        s = LINK.matcher(s).replaceAll("$1").replace("`", "");
        if (!keepBold) {
            s = s.replace("**", "");
        }
        return s.replaceAll("[ \\t]+", " ").replaceAll(" ([,;.])(\\s|$)", "$1$2").trim();
    }

    /** Notación de fórmulas a texto: {@code 4.30\text{ mm}} pasa a "4.30 mm", {@code \pm} a "±", {@code ^2} a "²". */
    private static String amjMath(String s, boolean insideFormula) {
        String r = s.replaceAll("\\\\(?:text|mathrm|textbf|mathbf)\\{([^}]*)}", "$1")
                .replaceAll("\\^\\{?\\\\circ}?", "°")
                .replace("\\times", "×").replace("\\pm", "±").replace("\\mu", "µ")
                .replace("\\leq", "≤").replace("\\geq", "≥").replace("\\le", "≤").replace("\\ge", "≥")
                .replace("\\approx", "≈").replace("\\sim", "~").replace("\\cdot", "·")
                .replace("\\rightarrow", "→").replace("\\to", "→").replace("\\Omega", "Ω").replace("\\Delta", "Δ")
                .replace("\\degree", "°").replace("\\%", "%")
                .replaceAll("\\\\[,;! ]", " ");
        r = r.replaceAll("\\^\\{?2}?", "²").replaceAll("\\^\\{?3}?", "³");
        if (insideFormula) {
            r = r.replaceAll("_\\{([^}]*)}", "$1").replaceAll("_(\\w)", "$1").replace("{", "").replace("}", "");
        }
        // cualquier otra orden se queda en su nombre: mejor "alpha" que una barra invertida suelta
        return r.replaceAll("\\\\([A-Za-z]+)", "$1");
    }
}

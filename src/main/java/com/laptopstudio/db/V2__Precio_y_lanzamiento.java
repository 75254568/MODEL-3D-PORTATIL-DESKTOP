package com.laptopstudio.db;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Migracion 2: columnas de precio, ano de lanzamiento y disponibilidad.
 * Se escribe en Java y no en SQL porque es condicional: antes de usar migraciones, Hibernate anadia las columnas
 * nuevas por su cuenta, asi que una base puede tenerlas ya, y SQLite no admite "ADD COLUMN IF NOT EXISTS".
 * Flyway la encuentra por ser un componente de Spring; el nombre de la clase le da la version y la descripcion.
 */
@Component
public class V2__Precio_y_lanzamiento extends BaseJavaMigration {

    private static final Map<String, String> COLUMNAS = new LinkedHashMap<>();

    static {
        COLUMNAS.put("release_year", "integer check (release_year>=1990)");
        COLUMNAS.put("availability", "varchar(20)");
        COLUMNAS.put("price_usd", "integer check (price_usd>=0)");
        COLUMNAS.put("price_estimated", "boolean");
    }

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            Set<String> existentes = new HashSet<>();
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(laptop)")) {
                while (rs.next()) {
                    existentes.add(rs.getString("name").toLowerCase(Locale.ROOT));
                }
            }
            for (Map.Entry<String, String> c : COLUMNAS.entrySet()) {
                if (!existentes.contains(c.getKey())) {
                    st.execute("ALTER TABLE laptop ADD COLUMN " + c.getKey() + " " + c.getValue());
                }
            }
        }
    }
}

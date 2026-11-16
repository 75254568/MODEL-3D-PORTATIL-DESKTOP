-- Marca y modelo identifican a un laptop sin distinguir mayusculas. Hasta ahora solo lo comprobaba la aplicacion.
-- Si una base antigua tuviera repetidos, se conserva el primero y los demas se distinguen por su id en vez de
-- borrarse: el indice no se podria crear con filas repetidas.
UPDATE laptop
SET model = model || ' (duplicado ' || id || ')'
WHERE id NOT IN (SELECT MIN(id) FROM laptop GROUP BY brand COLLATE NOCASE, model COLLATE NOCASE);

CREATE UNIQUE INDEX uk_laptop_brand_model ON laptop (brand COLLATE NOCASE, model COLLATE NOCASE);

-- Pasos de carga inicial ya aplicados (modelos de la semilla y tareas de una sola vez). Vive en la misma base
-- que el catalogo: si la base se pierde, se pierde tambien esta lista y el catalogo se vuelve a cargar entero.
CREATE TABLE seed_aplicado (
    clave varchar(160) not null,
    primary key (clave)
);

-- Insertar la categoría por defecto "Inbox"
-- Solo inserta si no existe para evitar errores si el script se ejecuta múltiples veces
-- o si el @PostConstruct en PomodoroService ya lo creó (aunque con ddl-auto=none y schema.sql,
-- el @PostConstruct no debería ser necesario para esto, pero es bueno tenerlo idempotente).

-- Para H2, no hay un INSERT IGNORE o ON CONFLICT DO NOTHING directo y simple sin subconsultas complejas
-- o MERGE que puede ser verboso para un simple insert.
-- Dado que schema.sql borra y recrea, un simple INSERT es suficiente aquí.
-- Si el @PostConstruct aún existe, podría causar un error de violación de unicidad si este script
-- se ejecuta DESPUÉS del PostConstruct.
-- Lo ideal es que SOLO UNA fuente cree los datos iniciales.
-- Si usas data.sql, considera quitar la creación de "Inbox" del @PostConstruct.

-- Asumiendo que schema.sql ya limpió la tabla:
INSERT INTO category (name) VALUES ('Inbox');

-- Opcional: Insertar algunas tareas de ejemplo
-- Primero, obtenemos el ID de 'Inbox' ya que no lo conocemos de antemano
-- Esto es más complejo en data.sql sin scripting.
-- Una forma más simple es asignar un ID fijo si sabes que será el primero.
-- Por ejemplo, si 'Inbox' es el primer registro, su ID será 1.

-- INSERT INTO task (name, description, creation_date, category_id, completed, pomodoros_spent)
-- VALUES ('Revisar correos', 'Limpiar la bandeja de entrada principal', CURRENT_TIMESTAMP, 1, FALSE, 0);

-- INSERT INTO task (name, description, creation_date, category_id, completed, pomodoros_spent)
-- VALUES ('Planificar el día', 'Definir prioridades para hoy', CURRENT_TIMESTAMP, 1, FALSE, 0);
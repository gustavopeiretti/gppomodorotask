-- Eliminar tablas si existen para una inicialización limpia (opcional, útil para desarrollo)
DROP TABLE IF EXISTS task CASCADE;
DROP TABLE IF EXISTS category CASCADE;

-- Crear tabla Category
CREATE TABLE category (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(255) NOT NULL UNIQUE
);

-- Crear tabla Task
CREATE TABLE task (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      description VARCHAR(1000),
                      creation_date TIMESTAMP NOT NULL,
                      completion_date TIMESTAMP,
                      category_id BIGINT NOT NULL,
                      completed BOOLEAN DEFAULT FALSE NOT NULL,
                      pomodoros_spent INT DEFAULT 0 NOT NULL,
                      CONSTRAINT fk_task_category FOREIGN KEY (category_id) REFERENCES category(id)
);

-- Opcional: Crear índices para mejorar el rendimiento de las búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_task_completed ON task(completed);
CREATE INDEX IF NOT EXISTS idx_task_category_id ON task(category_id);
CREATE INDEX IF NOT EXISTS idx_category_name ON category(name);
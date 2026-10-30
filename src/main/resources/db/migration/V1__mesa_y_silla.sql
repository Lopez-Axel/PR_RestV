CREATE TABLE mesa (
    mesa_id   BIGSERIAL PRIMARY KEY,
    numero    INTEGER     NOT NULL UNIQUE,
    capacidad INTEGER     NOT NULL CHECK (capacidad > 0),
    estado    VARCHAR(20) NOT NULL DEFAULT 'libre',
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE silla (
    silla_id BIGSERIAL PRIMARY KEY,
    mesa_id  BIGINT  NOT NULL REFERENCES mesa (mesa_id) ON DELETE CASCADE,
    numero   INTEGER NOT NULL,
    ocupada  BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uk_silla_mesa_numero UNIQUE (mesa_id, numero)
);

CREATE OR REPLACE FUNCTION fn_sincronizar_sillas()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        FOR i IN 1..NEW.capacidad LOOP
            INSERT INTO silla (mesa_id, numero, ocupada)
            VALUES (NEW.mesa_id, i, false);
        END LOOP;
        RETURN NEW;

    ELSIF TG_OP = 'UPDATE' THEN
        IF NEW.capacidad > OLD.capacidad THEN
            FOR i IN OLD.capacidad + 1..NEW.capacidad LOOP
                INSERT INTO silla (mesa_id, numero, ocupada)
                VALUES (NEW.mesa_id, i, false);
            END LOOP;

        ELSIF NEW.capacidad < OLD.capacidad THEN
            IF EXISTS (
                SELECT 1 FROM silla
                WHERE mesa_id = NEW.mesa_id
                  AND numero > NEW.capacidad
                  AND ocupada = true
            ) THEN
                RAISE EXCEPTION 'No se puede reducir la capacidad: hay sillas ocupadas con numero mayor a %',
                                NEW.capacidad USING ERRCODE = 'P0001';
            END IF;
            DELETE FROM silla
            WHERE mesa_id = NEW.mesa_id
              AND numero > NEW.capacidad;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_mesa_sillas_insert ON mesa;
CREATE TRIGGER trg_mesa_sillas_insert
AFTER INSERT ON mesa
FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_sillas();

DROP TRIGGER IF EXISTS trg_mesa_sillas_update ON mesa;
CREATE TRIGGER trg_mesa_sillas_update
AFTER UPDATE OF capacidad ON mesa
FOR EACH ROW EXECUTE FUNCTION fn_sincronizar_sillas();
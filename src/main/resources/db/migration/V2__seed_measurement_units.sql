CREATE TABLE measurement_unit (
    id                          uuid PRIMARY KEY,
    name                        varchar(255) NOT NULL,
    abbreviation                varchar(255) NOT NULL,
    unit_type                   varchar(255) NOT NULL,
    conversion_factor_to_base   numeric(19,10) NOT NULL,
    created_at                  timestamptz,
    updated_at                  timestamptz,
    created_by                  uuid,
    updated_by                  uuid
);

INSERT INTO measurement_unit (id, name, abbreviation, unit_type, conversion_factor_to_base)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Kilogram', 'kg', 'WEIGHT', 1),
    ('00000000-0000-0000-0000-000000000002', 'Gram', 'g', 'WEIGHT', 0.001),
    ('00000000-0000-0000-0000-000000000003', 'Ounce', 'oz', 'WEIGHT', 0.028349523125),
    ('00000000-0000-0000-0000-000000000004', 'Quintal', 'qq', 'WEIGHT', 45.359237),
    ('00000000-0000-0000-0000-000000000005', 'Liter', 'L', 'VOLUME', 1),
    ('00000000-0000-0000-0000-000000000006', 'Unit', 'unit', 'COUNT', 1);
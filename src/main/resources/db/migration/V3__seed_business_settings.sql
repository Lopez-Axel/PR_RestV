CREATE TABLE business_settings (
    id                         bigint PRIMARY KEY,
    default_margin_percentage  numeric(10,2) NOT NULL,
    min_margin_percentage      numeric(10,2) NOT NULL,
    max_margin_percentage      numeric(10,2) NOT NULL,
    created_at                 timestamptz,
    updated_at                 timestamptz,
    created_by                 uuid,
    updated_by                 uuid
);

INSERT INTO business_settings (id, default_margin_percentage, min_margin_percentage, max_margin_percentage)
VALUES (1, 30, 10, 60);
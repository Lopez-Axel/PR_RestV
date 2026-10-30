CREATE TABLE order_status (
    id          uuid PRIMARY KEY,
    name        varchar(255) NOT NULL UNIQUE,
    color_hex   varchar(7)   NOT NULL,
    created_at  timestamptz,
    updated_at  timestamptz,
    created_by  uuid,
    updated_by  uuid,
    deleted     boolean NOT NULL DEFAULT false
);

INSERT INTO order_status (id, name, color_hex)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'PENDING', '#F59E0B'),
    ('00000000-0000-0000-0000-000000000002', 'IN_PROGRESS', '#3B82F6'),
    ('00000000-0000-0000-0000-000000000003', 'SERVED', '#10B981'),
    ('00000000-0000-0000-0000-000000000004', 'CLOSED', '#6B7280'),
    ('00000000-0000-0000-0000-000000000005', 'CANCELLED', '#EF4444');

CREATE TABLE payment_status (
    id          uuid PRIMARY KEY,
    name        varchar(255) NOT NULL UNIQUE,
    created_at  timestamptz,
    updated_at  timestamptz,
    created_by  uuid,
    updated_by  uuid,
    deleted     boolean NOT NULL DEFAULT false
);

INSERT INTO payment_status (id, name)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'PENDING'),
    ('00000000-0000-0000-0000-000000000002', 'CONFIRMED'),
    ('00000000-0000-0000-0000-000000000003', 'FAILED'),
    ('00000000-0000-0000-0000-000000000004', 'EXPIRED');
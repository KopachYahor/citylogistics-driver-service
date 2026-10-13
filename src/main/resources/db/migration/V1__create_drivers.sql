CREATE TABLE drivers (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         phone VARCHAR(50) NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                         rating DECIMAL(3, 2) DEFAULT 5.00,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_assignments (
                                   id BIGSERIAL PRIMARY KEY,
                                   order_id BIGINT NOT NULL,
                                   driver_id BIGINT NOT NULL REFERENCES drivers(id),
                                   assigned_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_drivers_status ON drivers(status);
CREATE INDEX idx_assignments_order_id ON order_assignments(order_id);
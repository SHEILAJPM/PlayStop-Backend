-- Sucursales + Empleados (Plan Enterprise)
--
-- Este proyecto no usa Flyway/Liquibase (spring.jpa.hibernate.ddl-auto=none,
-- ver application.properties), asi que el esquema se gestiona con SQL manual
-- contra Neon. Ejecutar este script una sola vez antes de desplegar el
-- backend con estos cambios.
--
-- Verificar antes de ejecutar: confirmar que la columna users.role no tenga
-- un CHECK constraint limitado a ('ADMIN','OWNER','USER'). Si lo tiene, hay
-- que alterarlo para permitir 'EMPLOYEE' antes de que un empleado pueda
-- registrarse.

CREATE TABLE branches (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(255),
    district VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_branches_owner ON branches(owner_id);

-- Nullable a proposito: canchas existentes quedan sin sucursal, sin romper nada.
ALTER TABLE courts ADD COLUMN branch_id UUID REFERENCES branches(id);
CREATE INDEX idx_courts_branch ON courts(branch_id);

CREATE TABLE branch_employees (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL REFERENCES branches(id),
    employee_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_branch_employee UNIQUE (branch_id, employee_id)
);
CREATE INDEX idx_branch_employees_employee ON branch_employees(employee_id);

CREATE TABLE branch_invitations (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL REFERENCES branches(id),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_branch_invitations_email ON branch_invitations(email);

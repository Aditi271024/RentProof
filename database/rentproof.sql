-- ==============================================================================
-- RENTPROOF DATABASE SCHEMA
-- Project: RentProof - Landlord Rental & Property Management System
-- Database: rentproof
-- Module Lead: Aditi Sharma (Database, Authentication & Property Management)
-- Team Coordination:
--   - Aditi Sharma  : Landlords, Properties, Rooms, DB Connectivity
--   - Shweta        : Tenants Module (References property_id, room_id)
--   - Aanvi         : Payments Module (References tenant_id, property_id, room_id)
--   - Gungun        : Reminders Module (References landlord_id, tenant_id)
-- Engine: InnoDB | Charset: utf8mb4 | Collation: utf8mb4_unicode_ci
-- ==============================================================================

-- 1. DATABASE CREATION & SETUP
CREATE DATABASE IF NOT EXISTS rentproof
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE rentproof;

-- Disable foreign key checks during schema creation/resets
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables if already existing (in reverse dependency order)
DROP TABLE IF EXISTS reminders;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS tenants;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS landlords;

SET FOREIGN_KEY_CHECKS = 1;


-- ==============================================================================
-- TABLE 1: landlords
-- Managed by: Aditi Sharma (Authentication Module)
-- Purpose: Stores registered landlord user credentials, contact details, and account metadata.
-- ==============================================================================
CREATE TABLE landlords (
    landlord_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NULL,
    password_iterations INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_landlord_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index on email for fast authentication lookup
CREATE INDEX idx_landlords_email ON landlords(email);


-- ==============================================================================
-- TABLE 2: properties
-- Managed by: Aditi Sharma (Property Management Module)
-- Purpose: Stores real-estate properties owned by landlords (PGs, Flats, Apartments, Houses).
-- ==============================================================================
CREATE TABLE properties (
    property_id INT AUTO_INCREMENT PRIMARY KEY,
    landlord_id INT NOT NULL,
    property_name VARCHAR(100) NOT NULL,
    property_type VARCHAR(50) NOT NULL, -- 'PG', 'Flat', 'Apartment', 'House'
    address VARCHAR(255) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    total_rooms INT NOT NULL DEFAULT 1,
    monthly_rent DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_properties_landlord 
        FOREIGN KEY (landlord_id) REFERENCES landlords(landlord_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for property searches and landlord foreign key
CREATE INDEX idx_properties_landlord ON properties(landlord_id);
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_properties_type ON properties(property_type);


-- ==============================================================================
-- TABLE 3: rooms
-- Managed by: Aditi Sharma (Property Management Module)
-- Purpose: Stores individual rooms within properties (essential for PGs, hostels, and shared flats).
-- Constraint: Duplicate room numbers inside the same property are strictly forbidden.
-- ==============================================================================
CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    monthly_rent DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    room_status VARCHAR(20) NOT NULL DEFAULT 'Vacant', -- 'Vacant', 'Occupied', 'Maintenance'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rooms_property 
        FOREIGN KEY (property_id) REFERENCES properties(property_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    CONSTRAINT uq_property_room UNIQUE (property_id, room_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for room lookups by property and occupancy status
CREATE INDEX idx_rooms_property ON rooms(property_id);
CREATE INDEX idx_rooms_status ON rooms(room_status);


-- ==============================================================================
-- TEAM INTEGRATION CONTRACT: SHWETA (TENANTS MODULE)
-- Table: tenants
-- Required Foreign Keys:
--   - property_id -> properties(property_id)
--   - room_id     -> rooms(room_id)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS tenants (
    tenant_id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    room_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    emergency_contact VARCHAR(20),
    id_proof_number VARCHAR(50),
    move_in_date DATE NOT NULL,
    move_out_date DATE NULL,
    security_deposit DECIMAL(10, 2) DEFAULT 0.00,
    tenant_status VARCHAR(20) NOT NULL DEFAULT 'Active', -- 'Active', 'Moved Out', 'Pending'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenants_property 
        FOREIGN KEY (property_id) REFERENCES properties(property_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    CONSTRAINT fk_tenants_room 
        FOREIGN KEY (room_id) REFERENCES rooms(room_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tenants_property ON tenants(property_id);
CREATE INDEX idx_tenants_room ON tenants(room_id);
CREATE INDEX idx_tenants_status ON tenants(tenant_status);


-- ==============================================================================
-- TEAM INTEGRATION CONTRACT: AANVI (PAYMENTS MODULE)
-- Table: payments
-- Required Foreign Keys:
--   - tenant_id   -> tenants(tenant_id)
--   - property_id -> properties(property_id)
--   - room_id     -> rooms(room_id)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    property_id INT NOT NULL,
    room_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_month VARCHAR(20) NOT NULL, -- e.g. 'November 2026'
    payment_mode VARCHAR(30) NOT NULL,  -- 'UPI', 'Bank Transfer', 'Cash', 'Cheque'
    transaction_ref VARCHAR(100) NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'Paid', -- 'Paid', 'Pending', 'Overdue', 'Failed'
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_tenant 
        FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    CONSTRAINT fk_payments_property 
        FOREIGN KEY (property_id) REFERENCES properties(property_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    CONSTRAINT fk_payments_room 
        FOREIGN KEY (room_id) REFERENCES rooms(room_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_property ON payments(property_id);
CREATE INDEX idx_payments_status ON payments(payment_status);


-- ==============================================================================
-- TEAM INTEGRATION CONTRACT: GUNGUN (REMINDERS MODULE)
-- Table: reminders
-- Required Foreign Keys:
--   - landlord_id -> landlords(landlord_id)
--   - tenant_id   -> tenants(tenant_id) (OPTIONAL / NULLABLE if general reminder)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS reminders (
    reminder_id INT AUTO_INCREMENT PRIMARY KEY,
    landlord_id INT NOT NULL,
    tenant_id INT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    due_date DATE NOT NULL,
    reminder_type VARCHAR(50) NOT NULL, -- 'Rent Due', 'Agreement Expiry', 'Maintenance', 'General'
    reminder_status VARCHAR(20) NOT NULL DEFAULT 'Pending', -- 'Pending', 'Sent', 'Resolved'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_landlord 
        FOREIGN KEY (landlord_id) REFERENCES landlords(landlord_id)
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    CONSTRAINT fk_reminders_tenant 
        FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
        ON DELETE SET NULL 
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reminders_landlord ON reminders(landlord_id);
CREATE INDEX idx_reminders_tenant ON reminders(tenant_id);
CREATE INDEX idx_reminders_status ON reminders(reminder_status);

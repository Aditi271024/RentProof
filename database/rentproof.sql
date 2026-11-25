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
    monthly_rent DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
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


-- ==============================================================================
-- INITIAL SEED DATA FOR TESTING & VIVA DEMONSTRATION
-- Passwords are stored as SHA-256 hashes generated by our Java AuthService
-- Seed password for all demo accounts: 'RentProof@123'
-- SHA-256 hash: 2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab
-- ===============================================================================

-- 1. Insert Sample Landlords
INSERT INTO landlords (landlord_id, full_name, email, phone, password_hash) VALUES
(1, 'Aditi Sharma', 'aditi.landlord@rentproof.com', '9876543210', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab'),
(2, 'Rajesh Kumar Verma', 'rajesh.verma@gmail.com', '9811223344', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab'),
(3, 'Pooja Hegde', 'pooja.properties@yahoo.com', '9822334455', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab');

-- 2. Insert Sample Properties
INSERT INTO properties (property_id, landlord_id, property_name, property_type, address, city, state, pincode, total_rooms, monthly_rent, description) VALUES
(1, 1, 'Sunrise Executive PG', 'PG', 'Plot 42, Knowledge Park III', 'Greater Noida', 'Uttar Pradesh', '201306', 6, 8500.00, 'Premium fully furnished student PG with Wi-Fi, 3-time meals, power backup and 24x7 security.'),
(2, 1, 'Ivory Heights 3BHK Flat', 'Flat', 'Tower B, Flat 702, Golf City', 'Noida', 'Uttar Pradesh', '201301', 3, 28000.00, 'Spacious semi-furnished apartment for working professionals, modular kitchen and reserved car parking.'),
(3, 1, 'Blue Ridge Residency', 'Apartment', 'Sector 62, Near Metro Station', 'Noida', 'Uttar Pradesh', '201309', 4, 34000.00, 'Modern 4-room independent apartment with club house access, swimming pool and round-the-clock maintenance.'),
(4, 2, 'Greenfield Luxury Villa', 'House', '15A Civil Lines', 'Jaipur', 'Rajasthan', '302006', 5, 45000.00, 'Independent 2-storey bungalow with private lawn, solar heating and servant quarters.'),
(5, 3, 'Silver Oak Hostel & PG', 'PG', 'Near North Campus, Mall Road', 'Delhi', 'Delhi', '110007', 8, 9500.00, 'Girls PG with biometric security, air conditioning, study tables and attached washrooms.');

-- 3. Insert Sample Rooms for Properties
INSERT INTO rooms (room_id, property_id, room_number, monthly_rent, room_status) VALUES
-- Sunrise Executive PG (Property 1)
(1, 1, '101', 8500.00, 'Occupied'),
(2, 1, '102', 8500.00, 'Occupied'),
(3, 1, '103', 8500.00, 'Vacant'),
(4, 1, '201', 9000.00, 'Vacant'),
(5, 1, '202', 9000.00, 'Occupied'),
(6, 1, '203', 9500.00, 'Maintenance'),

-- Ivory Heights Flat (Property 2)
(7, 2, 'Room-A (Master)', 11000.00, 'Occupied'),
(8, 2, 'Room-B (Guest)', 9000.00, 'Vacant'),
(9, 2, 'Room-C (Standard)', 8000.00, 'Occupied'),

-- Blue Ridge Residency (Property 3)
(10, 3, 'B-101', 8500.00, 'Vacant'),
(11, 3, 'B-102', 8500.00, 'Occupied'),
(12, 3, 'B-103', 8500.00, 'Vacant'),
(13, 3, 'B-104', 8500.00, 'Vacant');

-- 4. Sample Integration Seed for Tenants (For Shweta's Module Reference)
INSERT INTO tenants (tenant_id, property_id, room_id, full_name, email, phone, id_proof_number, move_in_date, monthly_rent, security_deposit, tenant_status) VALUES
(1, 1, 1, 'Rohan Verma', 'rohan.v@gmail.com', '9988776655', 'AADHAAR-8921-3412-9012', '2026-08-01', 8500.00, 17000.00, 'Active'),
(2, 1, 2, 'Ankit Mishra', 'ankit.m@yahoo.com', '9871122334', 'AADHAAR-6712-4590-1123', '2026-09-15', 8500.00, 17000.00, 'Active'),
(3, 2, 7, 'Sneha Kapoor', 'sneha.k@outlook.com', '9711334455', 'PAN-ABCDE1234F', '2026-07-01', 11000.00, 22000.00, 'Active');

-- 5. Sample Integration Seed for Payments (For Aanvi's Module Reference)
INSERT INTO payments (payment_id, tenant_id, property_id, room_id, amount, payment_date, payment_month, payment_mode, transaction_ref, payment_status, remarks) VALUES
(1, 1, 1, 1, 8500.00, '2026-11-05', 'November 2026', 'UPI', 'UPI/20261105/98218731', 'Paid', 'Rent received via Google Pay'),
(2, 2, 1, 2, 8500.00, '2026-11-04', 'November 2026', 'Bank Transfer', 'NEFT/HDFC/001928371', 'Paid', 'On-time monthly rental'),
(3, 3, 2, 7, 11000.00, '2026-11-02', 'November 2026', 'UPI', 'UPI/20261102/44129031', 'Paid', 'Direct payment received');

-- 6. Sample Integration Seed for Reminders (For Gungun's Module Reference)
INSERT INTO reminders (reminder_id, landlord_id, tenant_id, title, message, due_date, reminder_type, reminder_status) VALUES
(1, 1, 1, 'Upcoming Rental Due', 'Friendly reminder: December rent is due on 5th Dec.', '2026-12-05', 'Rent Due', 'Pending'),
(2, 1, NULL, 'Bi-annual Fire Extinguisher Inspection', 'Inspect all fire extinguishers and smoke alarms in Sunrise PG.', '2026-11-30', 'Maintenance', 'Pending');


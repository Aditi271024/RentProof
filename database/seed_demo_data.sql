-- ==============================================================================
-- RENTPROOF: IDEMPOTENT DEMO SEED DATA
-- Purpose: Safely seeds demo accounts, properties, rooms, tenants, payments, and reminders.
-- Safety: Uses INSERT IGNORE / conditional insertion so it can be executed multiple times
-- without causing duplicate key errors.
-- Database: rentproof
-- Demo credentials: aditi.landlord@rentproof.com / RentProof@123
-- ==============================================================================

USE rentproof;

-- 1. Insert Demo Landlords (if not already existing)
INSERT IGNORE INTO landlords (landlord_id, full_name, email, phone, password_hash, password_salt, password_iterations) VALUES
(1, 'Aditi Sharma', 'aditi.landlord@rentproof.com', '9876543210', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab', NULL, NULL),
(2, 'Rajesh Kumar Verma', 'rajesh.verma@gmail.com', '9811223344', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab', NULL, NULL),
(3, 'Pooja Hegde', 'pooja.properties@yahoo.com', '9822334455', '2a3209ec847e336f2bc941f435ac17b58058db791786f7d021a1f827b1cd27ab', NULL, NULL);

-- 2. Insert Demo Properties
INSERT IGNORE INTO properties (property_id, landlord_id, property_name, property_type, address, city, state, pincode, total_rooms, monthly_rent, description) VALUES
(1, 1, 'Sunrise Executive PG', 'PG', 'Plot 42, Knowledge Park III', 'Greater Noida', 'Uttar Pradesh', '201306', 6, 8500.00, 'Premium fully furnished student PG with Wi-Fi, 3-time meals, power backup and 24x7 security.'),
(2, 1, 'Ivory Heights 3BHK Flat', 'Flat', 'Tower B, Flat 702, Golf City', 'Noida', 'Uttar Pradesh', '201301', 3, 28000.00, 'Spacious semi-furnished apartment for working professionals, modular kitchen and reserved car parking.'),
(3, 1, 'Blue Ridge Residency', 'Apartment', 'Sector 62, Near Metro Station', 'Noida', 'Uttar Pradesh', '201309', 4, 34000.00, 'Modern 4-room independent apartment with club house access, swimming pool and round-the-clock maintenance.');

-- 3. Insert Demo Rooms
INSERT IGNORE INTO rooms (room_id, property_id, room_number, monthly_rent, room_status) VALUES
(1, 1, '101', 8500.00, 'Occupied'),
(2, 1, '102', 8500.00, 'Occupied'),
(3, 1, '103', 8500.00, 'Vacant'),
(4, 1, '201', 9000.00, 'Vacant'),
(5, 1, '202', 9000.00, 'Occupied'),
(6, 1, '203', 9500.00, 'Maintenance'),
(7, 2, 'Room-A (Master)', 11000.00, 'Occupied'),
(8, 2, 'Room-B (Guest)', 9000.00, 'Vacant'),
(9, 2, 'Room-C (Standard)', 8000.00, 'Occupied'),
(10, 3, 'B-101', 8500.00, 'Vacant'),
(11, 3, 'B-102', 8500.00, 'Occupied'),
(12, 3, 'B-103', 8500.00, 'Vacant'),
(13, 3, 'B-104', 8500.00, 'Vacant');

-- 4. Insert Demo Tenants
INSERT IGNORE INTO tenants (tenant_id, property_id, room_id, full_name, email, phone, id_proof_number, move_in_date, monthly_rent, security_deposit, tenant_status) VALUES
(1, 1, 1, 'Rohan Verma', 'rohan.v@gmail.com', '9988776655', 'AADHAAR-8921-3412-9012', '2026-08-01', 8500.00, 17000.00, 'Active'),
(2, 1, 2, 'Ankit Mishra', 'ankit.m@yahoo.com', '9871122334', 'AADHAAR-6712-4590-1123', '2026-09-15', 8500.00, 17000.00, 'Active'),
(3, 2, 7, 'Sneha Kapoor', 'sneha.k@outlook.com', '9711334455', 'PAN-ABCDE1234F', '2026-07-01', 11000.00, 22000.00, 'Active');

-- 5. Insert Demo Payments
INSERT IGNORE INTO payments (payment_id, tenant_id, property_id, room_id, amount, payment_date, payment_month, payment_mode, transaction_ref, payment_status, remarks) VALUES
(1, 1, 1, 1, 8500.00, '2026-11-05', 'November 2026', 'UPI', 'UPI/20261105/98218731', 'Paid', 'Rent received via Google Pay'),
(2, 2, 1, 2, 8500.00, '2026-11-04', 'November 2026', 'Bank Transfer', 'NEFT/HDFC/001928371', 'Paid', 'On-time monthly rental'),
(3, 3, 2, 7, 11000.00, '2026-11-02', 'November 2026', 'UPI', 'UPI/20261102/44129031', 'Paid', 'Direct payment received');

-- 6. Insert Demo Reminders
INSERT IGNORE INTO reminders (reminder_id, landlord_id, tenant_id, title, message, due_date, reminder_type, reminder_status) VALUES
(1, 1, 1, 'Upcoming Rental Due', 'Friendly reminder: December rent is due on 5th Dec.', '2026-12-05', 'Rent Due', 'Pending'),
(2, 1, NULL, 'Bi-annual Fire Extinguisher Inspection', 'Inspect all fire extinguishers and smoke alarms in Sunrise PG.', '2026-11-30', 'Maintenance', 'Pending');

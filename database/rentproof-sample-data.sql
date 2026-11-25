-- Optional sample fixtures for a fresh local demo database.
-- Register three landlords through the application first, in order, so IDs 1-3 exist.
-- This file contains no landlord password material or real identity numbers.

INSERT INTO properties (property_id, landlord_id, property_name, property_type, address, city, state, pincode, total_rooms, monthly_rent, description) VALUES
(1, 1, 'Sunrise Executive PG', 'PG', 'Plot 42, Knowledge Park III', 'Greater Noida', 'Uttar Pradesh', '201306', 6, 8500.00, 'Premium fully furnished student PG with Wi-Fi, 3-time meals, power backup and 24x7 security.'),
(2, 1, 'Ivory Heights 3BHK Flat', 'Flat', 'Tower B, Flat 702, Golf City', 'Noida', 'Uttar Pradesh', '201301', 3, 28000.00, 'Spacious semi-furnished apartment for working professionals, modular kitchen and reserved car parking.'),
(3, 1, 'Blue Ridge Residency', 'Apartment', 'Sector 62, Near Metro Station', 'Noida', 'Uttar Pradesh', '201309', 4, 34000.00, 'Modern 4-room independent apartment with club house access, swimming pool and round-the-clock maintenance.'),
(4, 2, 'Greenfield Luxury Villa', 'House', '15A Civil Lines', 'Jaipur', 'Rajasthan', '302006', 5, 45000.00, 'Independent 2-storey bungalow with private lawn, solar heating and servant quarters.'),
(5, 3, 'Silver Oak Hostel & PG', 'PG', 'Near North Campus, Mall Road', 'Delhi', 'Delhi', '110007', 8, 9500.00, 'Girls PG with biometric security, air conditioning, study tables and attached washrooms.');

INSERT INTO rooms (room_id, property_id, room_number, monthly_rent, room_status) VALUES
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

INSERT INTO tenants (tenant_id, property_id, room_id, full_name, email, phone, id_proof_number, move_in_date, security_deposit, tenant_status) VALUES
(1, 1, 1, 'Rohan Verma', 'rohan.v@gmail.com', '9988776655', '', '2026-08-01', 17000.00, 'Active'),
(2, 1, 2, 'Ankit Mishra', 'ankit.m@yahoo.com', '9871122334', '', '2026-09-15', 17000.00, 'Active'),
(3, 2, 7, 'Sneha Kapoor', 'sneha.k@outlook.com', '9711334455', '', '2026-07-01', 22000.00, 'Active');

INSERT INTO payments (payment_id, tenant_id, property_id, room_id, amount, payment_date, payment_month, payment_mode, transaction_ref, payment_status, remarks) VALUES
(1, 1, 1, 1, 8500.00, '2026-11-05', 'November 2026', 'UPI', 'UPI/20261105/98218731', 'Paid', 'Rent received via Google Pay'),
(2, 2, 1, 2, 8500.00, '2026-11-04', 'November 2026', 'Bank Transfer', 'NEFT/HDFC/001928371', 'Paid', 'On-time monthly rental'),
(3, 3, 2, 7, 11000.00, '2026-11-02', 'November 2026', 'UPI', 'UPI/20261102/44129031', 'Paid', 'Direct payment received');

INSERT INTO reminders (reminder_id, landlord_id, tenant_id, title, message, due_date, reminder_type, reminder_status) VALUES
(1, 1, 1, 'Upcoming Rental Due', 'Friendly reminder: December rent is due on 5th Dec.', '2026-12-05', 'Rent Due', 'Pending'),
(2, 1, NULL, 'Bi-annual Fire Extinguisher Inspection', 'Inspect all fire extinguishers and smoke alarms in Sunrise PG.', '2026-11-30', 'Maintenance', 'Pending');

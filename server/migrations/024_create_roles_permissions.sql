CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(50) REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(50) REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Seed Roles
INSERT INTO roles (id, name, description) VALUES
('PATIENT', 'Patient', 'General patient access'),
('DOCTOR', 'Doctor', 'Doctor portal access'),
('RECEPTIONIST', 'Receptionist', 'Reception desk access'),
('ADMIN', 'Admin', 'Administrator portal access')
ON CONFLICT (id) DO NOTHING;

-- Seed Permissions
INSERT INTO permissions (id, name, description) VALUES
('VIEW_OWN_PROFILE', 'View Own Profile', 'View own user profile details'),
('BOOK_APPOINTMENT', 'Book Appointment', 'Request an appointment with a doctor'),
('REQUEST_TOKEN', 'Request Token', 'Request a digital queue token'),
('VIEW_OWN_QUEUE', 'View Own Queue', 'Track live position of own active token'),
('VIEW_OWN_MEDICAL_RECORDS', 'View Own Medical Records', 'View own consultation history and EMR'),
('VIEW_OWN_BILLS', 'View Own Bills', 'View own billing details'),
('VIEW_OWN_PRESCRIPTIONS', 'View Own Prescriptions', 'View own prescription list'),
('SAVE_FAVOURITE_HOSPITAL', 'Save Favourite Hospital', 'Save hospital in favorites'),
('VIEW_PATIENTS', 'View Patients', 'Search and view patient directory'),
('MANAGE_REQUESTS', 'Manage Requests', 'Approve/reject appointment requests'),
('CREATE_TOKEN', 'Create Token', 'Create new walk-in / online tokens'),
('MANAGE_QUEUE', 'Manage Queue', 'Reorder, skip, recall, pause, call queue tokens'),
('CREATE_BILL', 'Create Bill', 'Generate new invoice bills'),
('RECORD_PAYMENT', 'Record Payment', 'Receive and record payments for bills'),
('VIEW_ASSIGNED_PATIENTS', 'View Assigned Patients', 'View list of patients booked/queued for doctor'),
('VIEW_PATIENT_HISTORY', 'View Patient History', 'Check history of patient EMR records'),
('CREATE_CONSULTATION', 'Create Consultation', 'Start a clinical consultation'),
('CREATE_PRESCRIPTION', 'Create Prescription', 'Write prescription for consultation'),
('ORDER_LAB', 'Order Lab', 'Submit new lab test order'),
('COMPLETE_CONSULTATION', 'Complete Consultation', 'Finalize consultation and create bill/records'),
('TOGGLE_AVAILABILITY', 'Toggle Availability', 'Toggle doctor availability status'),
('MANAGE_USERS', 'Manage Users', 'Create, update, deactivate user profiles'),
('MANAGE_HOSPITALS', 'Manage Hospitals', 'Setup and manage hospital details'),
('MANAGE_DOCTORS', 'Manage Doctors', 'Recruit and configure doctor profiles'),
('MANAGE_DEPARTMENTS', 'Manage Departments', 'Setup hospital departments'),
('VIEW_ANALYTICS', 'View Analytics', 'View revenue and workload statistics'),
('MANAGE_PERMISSIONS', 'Manage Permissions', 'Edit role permission privileges')
ON CONFLICT (id) DO NOTHING;

-- Associate Permissions to Roles
-- Patient Permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('PATIENT', 'VIEW_OWN_PROFILE'),
('PATIENT', 'BOOK_APPOINTMENT'),
('PATIENT', 'REQUEST_TOKEN'),
('PATIENT', 'VIEW_OWN_QUEUE'),
('PATIENT', 'VIEW_OWN_MEDICAL_RECORDS'),
('PATIENT', 'VIEW_OWN_BILLS'),
('PATIENT', 'VIEW_OWN_PRESCRIPTIONS'),
('PATIENT', 'SAVE_FAVOURITE_HOSPITAL')
ON CONFLICT DO NOTHING;

-- Receptionist Permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('RECEPTIONIST', 'VIEW_OWN_PROFILE'),
('RECEPTIONIST', 'VIEW_PATIENTS'),
('RECEPTIONIST', 'MANAGE_REQUESTS'),
('RECEPTIONIST', 'CREATE_TOKEN'),
('RECEPTIONIST', 'MANAGE_QUEUE'),
('RECEPTIONIST', 'CREATE_BILL'),
('RECEPTIONIST', 'RECORD_PAYMENT')
ON CONFLICT DO NOTHING;

-- Doctor Permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('DOCTOR', 'VIEW_OWN_PROFILE'),
('DOCTOR', 'VIEW_ASSIGNED_PATIENTS'),
('DOCTOR', 'VIEW_PATIENT_HISTORY'),
('DOCTOR', 'CREATE_CONSULTATION'),
('DOCTOR', 'CREATE_PRESCRIPTION'),
('DOCTOR', 'ORDER_LAB'),
('DOCTOR', 'COMPLETE_CONSULTATION'),
('DOCTOR', 'TOGGLE_AVAILABILITY')
ON CONFLICT DO NOTHING;

-- Admin Permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('ADMIN', 'VIEW_OWN_PROFILE'),
('ADMIN', 'MANAGE_USERS'),
('ADMIN', 'MANAGE_HOSPITALS'),
('ADMIN', 'MANAGE_DOCTORS'),
('ADMIN', 'MANAGE_DEPARTMENTS'),
('ADMIN', 'VIEW_ANALYTICS'),
('ADMIN', 'MANAGE_PERMISSIONS')
ON CONFLICT DO NOTHING;

-- Now add foreign key constraint on users table to enforce role mapping
ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles(id);

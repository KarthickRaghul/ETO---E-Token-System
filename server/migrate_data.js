const db = require('./db');
const crypto = require('crypto');

// Fixed UUIDs for default hospital, departments, and doctors
const HOSPITAL_ID = 'e4b77f98-5c1a-4fdf-9737-1234567890ab';
const HOSPITAL_2_ID = 'e4b77f98-5c1a-4fdf-9737-1234567890cd';
const HOSPITAL_3_ID = 'e4b77f98-5c1a-4fdf-9737-1234567890ef';

// Staff UUIDs
const RECP_USER_ID = 'fa55a55a-5555-5555-5555-555555555555';
const RECP_ID = 'fb66b66b-6666-6666-6666-666666666666';

const ADMIN_USER_ID = 'ac11ac11-1111-1111-1111-111111111111';
const ADMIN_ID = 'ad22ad22-2222-2222-2222-222222222222';

const PATIENT_USER_ID = 'fa99a99a-9999-9999-9999-999999999999';
const PATIENT_ID = 'fb88b88b-8888-8888-8888-888888888888';

// Doctor UUIDs
const DOCTORS = [
  {
    id: 'd1111111-1111-1111-1111-111111111111',
    userId: 'd1000000-0000-0000-0000-000000000001',
    email: 'sarah.jenkins@eto.com',
    phone: '9876540001',
    first: 'Sarah',
    last: 'Jenkins',
    docNum: 'DOC-SARAH-001',
    specialty: 'Cardiology',
    qual: 'MBBS, MD (Cardiology)',
    exp: 10,
    fee: 500.00,
    bio: 'Specialist in cardiovascular diagnostics and patient care.',
    room: 'OPD-101',
    hospitalId: HOSPITAL_ID,
    deptId: '11111111-1111-1111-1111-111111111111'
  },
  {
    id: 'd2222222-2222-2222-2222-222222222222',
    userId: 'd2000000-0000-0000-0000-000000000002',
    email: 'robert.chen@eto.com',
    phone: '9876540002',
    first: 'Robert',
    last: 'Chen',
    docNum: 'DOC-ROBERT-002',
    specialty: 'Pediatrics',
    qual: 'MBBS, MD (Pediatrics)',
    exp: 8,
    fee: 350.00,
    bio: 'Dedicated child health specialist with experience in newborn and adolescent care.',
    room: 'OPD-102',
    hospitalId: HOSPITAL_ID,
    deptId: '22222222-2222-2222-2222-222222222222'
  },
  {
    id: 'd3333333-3333-3333-3333-333333333333',
    userId: 'd3000000-0000-0000-0000-000000000003',
    email: 'amanda.ross@eto.com',
    phone: '9876540003',
    first: 'Amanda',
    last: 'Ross',
    docNum: 'DOC-AMANDA-003',
    specialty: 'Dermatology',
    qual: 'MBBS, MD (Dermatology)',
    exp: 12,
    fee: 400.00,
    bio: 'Expert dermatologist specializing in clinical and aesthetic skin treatments.',
    room: 'OPD-103',
    hospitalId: HOSPITAL_ID,
    deptId: '33333333-3333-3333-3333-333333333333'
  },
  {
    id: 'd4444444-4444-4444-4444-444444444444',
    userId: 'd4000000-0000-0000-0000-000000000004',
    email: 'james.carter@eto.com',
    phone: '9876540004',
    first: 'James',
    last: 'Carter',
    docNum: 'DOC-JAMES-004',
    specialty: 'General Medicine',
    qual: 'MBBS, MD (Internal Medicine)',
    exp: 15,
    fee: 300.00,
    bio: 'Consultant physician with extensive expertise in primary healthcare and preventive medicine.',
    room: 'OPD-104',
    hospitalId: HOSPITAL_ID,
    deptId: '44444444-4444-4444-4444-444444444444'
  }
];

function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

const ADMIN_PASSWORD_HASH = hashPassword('admin123');
const PATIENT_PASSWORD_HASH = hashPassword('patient123');
const RECEPTIONIST_PASSWORD_HASH = hashPassword('receptionist123');
const DOCTOR_PASSWORD_HASH = hashPassword('doctor123');

async function migrateData() {
  const client = await db.pool.connect();
  try {
    console.log('Starting data migration mapping...');
    await client.query('BEGIN');
    
    console.log('Clearing clean database tables...');
    await client.query(`
      TRUNCATE TABLE
        queue_events,
        queue_entries,
        prescription_items,
        prescriptions,
        payments,
        bill_items,
        bills,
        consultations,
        medical_records,
        tokens,
        patient_medical_profiles,
        patients,
        doctor_schedules,
        doctors,
        administrators,
        receptionists,
        users,
        departments,
        hospitals
        CASCADE;
    `);

    // 1. Create Hospitals
    console.log('Inserting hospitals...');
    const hospitalsList = [
      {
        id: HOSPITAL_ID,
        name: 'City General Hospital & ETO Clinic',
        reg: 'HOSP-ETO-999',
        desc: 'Premium ETO queue management and clinical care facility',
        phone: '044-2456789',
        email: 'info@citygeneral.com',
        addr: '100 Medical Plaza, Adyar',
        city: 'Chennai',
        state: 'Tamil Nadu',
        zip: '600020',
        lat: 13.0063,
        lon: 80.2574
      },
      {
        id: HOSPITAL_2_ID,
        name: "St. Mary's Pediatric & Family Health",
        reg: 'HOSP-ETO-888',
        desc: 'Dedicated pediatric and comprehensive family health center',
        phone: '044-2888888',
        email: 'contact@stmarys.com',
        addr: '50 Park Avenue, Mylapore',
        city: 'Chennai',
        state: 'Tamil Nadu',
        zip: '600004',
        lat: 13.0329,
        lon: 80.2644
      },
      {
        id: HOSPITAL_3_ID,
        name: 'Metro Heart & Neurological Institute',
        reg: 'HOSP-ETO-777',
        desc: 'Advanced cardiology, neurology, and specialty surgery hospital',
        phone: '044-2777777',
        email: 'info@metroheart.com',
        addr: '12 G.N. Chetty Road, T. Nagar',
        city: 'Chennai',
        state: 'Tamil Nadu',
        zip: '600017',
        lat: 13.0418,
        lon: 80.2337
      }
    ];

    for (const h of hospitalsList) {
      await client.query(`
        INSERT INTO hospitals (id, name, registration_number, description, phone, email, address_line, city, state, postal_code, timezone, latitude, longitude)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'Asia/Kolkata', $11, $12);
      `, [h.id, h.name, h.reg, h.desc, h.phone, h.email, h.addr, h.city, h.state, h.zip, h.lat, h.lon]);
    }

    // 2. Create Departments
    console.log('Creating departments...');
    const departmentsList = [
      { id: '11111111-1111-1111-1111-111111111111', hospId: HOSPITAL_ID, name: 'Cardiology', desc: 'Cardiovascular diagnostics and therapy' },
      { id: '22222222-2222-2222-2222-222222222222', hospId: HOSPITAL_ID, name: 'Pediatrics', desc: 'Child health and infant care' },
      { id: '33333333-3333-3333-3333-333333333333', hospId: HOSPITAL_ID, name: 'Dermatology', desc: 'Skin care and dermatology treatments' },
      { id: '44444444-4444-4444-4444-444444444444', hospId: HOSPITAL_ID, name: 'General Medicine', desc: 'Primary care and internal medicine' },
      { id: '55555555-5555-5555-5555-555555555555', hospId: HOSPITAL_ID, name: 'Orthopedics', desc: 'Bone and joint care specialist center' }
    ];

    for (const d of departmentsList) {
      await client.query(`
        INSERT INTO departments (id, hospital_id, name, description, specialization, is_active)
        VALUES ($1, $2, $3, $4, $5, TRUE);
      `, [d.id, d.hospId, d.name, d.desc, d.name]);
    }

    // 3. Create Doctors
    console.log('Inserting doctors...');
    for (const doc of DOCTORS) {
      // Create User
      await client.query(`
        INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
        VALUES ($1, $2, $3, $4, 'DOCTOR', $5, $6, TRUE, TRUE);
      `, [doc.userId, doc.email, doc.phone, DOCTOR_PASSWORD_HASH, doc.first, doc.last]);

      // Create Doctor Profile
      await client.query(`
        INSERT INTO doctors (id, user_id, doctor_number, specialization, qualification, experience_years, consultation_fee, bio, hospital_id, department_id, consultation_room, is_available)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, TRUE);
      `, [doc.id, doc.userId, doc.docNum, doc.specialty, doc.qual, doc.exp, doc.fee, doc.bio, doc.hospitalId, doc.deptId, doc.room]);

      // Create Schedule for Doctor (Mon-Fri 09:00 to 17:00)
      for (let day = 1; day <= 5; day++) {
        await client.query(`
          INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, appointment_duration_minutes, max_patients, is_active)
          VALUES ($1, $2, '09:00:00', '17:00:00', 30, 30, TRUE);
        `, [doc.id, day]);
      }
    }

    // 4. Create Receptionist (ReceptionistKarthi)
    console.log('Seeding receptionist user (ReceptionistKarthi)...');
    await client.query(`
      INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
      VALUES ($1, 'receptionist.karthi@eto.com', '9876541111', $2, 'RECEPTIONIST', 'Receptionist', 'Karthi', TRUE, TRUE);
    `, [RECP_USER_ID, RECEPTIONIST_PASSWORD_HASH]);

    await client.query(`
      INSERT INTO receptionists (id, user_id, employee_number, hospital_id, designation, shift, joining_date, is_active)
      VALUES ($1, $2, 'EMP-RECP-001', $3, 'Head Desk Receptionist', 'MORNING', CURRENT_DATE, TRUE);
    `, [RECP_ID, RECP_USER_ID, HOSPITAL_ID]);

    // 5. Create Admin
    console.log('Seeding admin user...');
    await client.query(`
      INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
      VALUES ($1, 'admin@eto.com', '9876542222', $2, 'ADMIN', 'Super', 'Admin', TRUE, TRUE);
    `, [ADMIN_USER_ID, ADMIN_PASSWORD_HASH]);

    await client.query(`
      INSERT INTO administrators (id, user_id, hospital_id, admin_level, is_active)
      VALUES ($1, $2, $3, 'HOSPITAL', TRUE);
    `, [ADMIN_ID, ADMIN_USER_ID, HOSPITAL_ID]);

    // 6. Create Patient (PatientRaghul)
    console.log('Seeding patient user (PatientRaghul) with empty medical profile...');
    await client.query(`
      INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
      VALUES ($1, 'patient.raghul@eto.com', '9876543333', $2, 'PATIENT', 'Patient', 'Raghul', TRUE, TRUE);
    `, [PATIENT_USER_ID, PATIENT_PASSWORD_HASH]);

    await client.query(`
      INSERT INTO patients (id, user_id, patient_number, date_of_birth, gender, blood_group, address, emergency_contact_name, emergency_contact_phone)
      VALUES ($1, $2, 'PAT-RAGHUL-001', NULL, NULL, NULL, NULL, NULL, NULL);
    `, [PATIENT_ID, PATIENT_USER_ID]);

    await client.query(`
      INSERT INTO patient_medical_profiles (patient_id, blood_group, allergies, chronic_conditions, current_medications)
      VALUES ($1, NULL, NULL, NULL, NULL);
    `, [PATIENT_ID]);

    await client.query('COMMIT');
    console.log('Data migration complete. Professional data successfully ingested and queues cleared.');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Data migration failed:', err);
    process.exit(1);
  } finally {
    client.release();
  }
}

migrateData().then(() => process.exit(0));

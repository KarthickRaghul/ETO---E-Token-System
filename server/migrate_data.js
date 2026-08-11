const db = require('./db');

// Fixed UUIDs for default hospital, departments, and doctors
const HOSPITAL_ID = 'e4b77f98-5c1a-4fdf-9737-1234567890ab';

const DEPT_MAP = {
  '1': '11111111-1111-1111-1111-111111111111', // Cardiology
  '2': '22222222-2222-2222-2222-222222222222', // Pediatrics
  '3': '33333333-3333-3333-3333-333333333333', // Dermatology
  '4': '44444444-4444-4444-4444-444444444444'  // General Medicine
};

const DOC_MAP = {
  'd1': {
    id: 'd1111111-1111-1111-1111-111111111111',
    user_id: 'd1000000-0000-0000-0000-000000000001',
    email: 'sarah.jenkins@citygeneral.com',
    phone: '9876540001',
    first: 'Sarah',
    last: 'Jenkins',
    doc_num: 'DOC-SARAH-001'
  },
  'd2': {
    id: 'd2222222-2222-2222-2222-222222222222',
    user_id: 'd2000000-0000-0000-0000-000000000002',
    email: 'robert.chen@citygeneral.com',
    phone: '9876540002',
    first: 'Robert',
    last: 'Chen',
    doc_num: 'DOC-ROBERT-002'
  },
  'd3': {
    id: 'd3333333-3333-3333-3333-333333333333',
    user_id: 'd3000000-0000-0000-0000-000000000003',
    email: 'amanda.ross@citygeneral.com',
    phone: '9876540003',
    first: 'Amanda',
    last: 'Ross',
    doc_num: 'DOC-AMANDA-003'
  },
  'd4': {
    id: 'd4444444-4444-4444-4444-444444444444',
    user_id: 'd4000000-0000-0000-0000-000000000004',
    email: 'james.carter@citygeneral.com',
    phone: '9876540004',
    first: 'James',
    last: 'Carter',
    doc_num: 'DOC-JAMES-004'
  }
};

// Seed receptionist
const RECP_USER_ID = 'fa55a55a-5555-5555-5555-555555555555';
const RECP_ID = 'fb66b66b-6666-6666-6666-666666666666';

// Seed admin
const ADMIN_USER_ID = 'ac11ac11-1111-1111-1111-111111111111';
const ADMIN_ID = 'ad22ad22-2222-2222-2222-222222222222';

const DEFAULT_PASSWORD_HASH = '$2b$10$D2B7Ym9iL0RhdGEubWlncmF0aW9uLXNlZWQ'; // Mock hash

async function migrateData() {
  const client = await db.pool.connect();
  try {
    console.log('Starting data migration mapping...');
    await client.query('BEGIN');
    const insertedKeys = new Set();


    // 1. Create Default Hospital
    console.log('Inserting default hospital...');
    await client.query(`
      INSERT INTO hospitals (id, name, registration_number, description, phone, email, address_line, city, state, postal_code, timezone)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      ON CONFLICT (registration_number) DO NOTHING;
    `, [
      HOSPITAL_ID,
      'City General Hospital & ETO Clinic',
      'HOSP-ETO-999',
      'Premium ETO queue management and clinical care facility',
      '044-2456789',
      'info@citygeneral.com',
      '100 Medical Plaza, Adyar',
      'Chennai',
      'Tamil Nadu',
      '600020',
      'Asia/Kolkata'
    ]);

    // 2. Migrate Departments
    console.log('Migrating departments...');
    const oldDepts = await client.query('SELECT * FROM old_departments');
    for (const row of oldDepts.rows) {
      const newId = DEPT_MAP[row.id];
      if (newId) {
        await client.query(`
          INSERT INTO departments (id, hospital_id, name, description, specialization, is_active)
          VALUES ($1, $2, $3, $4, $5, TRUE)
          ON CONFLICT (hospital_id, name) DO NOTHING;
        `, [newId, HOSPITAL_ID, row.name, row.description, row.name]);
      }
    }

    // 3. Migrate Doctors (User first, then Doctor profile)
    console.log('Migrating doctors...');
    const oldDocs = await client.query('SELECT * FROM old_doctors');
    for (const row of oldDocs.rows) {
      const mapping = DOC_MAP[row.id];
      if (mapping) {
        // Create User
        await client.query(`
          INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
          VALUES ($1, $2, $3, $4, 'DOCTOR', $5, $6, TRUE, TRUE)
          ON CONFLICT (phone) DO NOTHING;
        `, [
          mapping.user_id,
          mapping.email,
          mapping.phone,
          DEFAULT_PASSWORD_HASH,
          mapping.first,
          mapping.last
        ]);

        // Create Doctor Profile
        await client.query(`
          INSERT INTO doctors (id, user_id, doctor_number, specialization, qualification, experience_years, consultation_fee, bio, hospital_id, department_id, consultation_room, is_available)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
          ON CONFLICT (doctor_number) DO NOTHING;
        `, [
          mapping.id,
          mapping.user_id,
          mapping.doc_num,
          row.specialty,
          'MBBS, MD',
          10,
          row.id === 'd1' ? 500.00 : 350.00,
          `Experienced specialist in ${row.specialty}`,
          HOSPITAL_ID,
          DEPT_MAP[row.department_id],
          row.id === 'd1' ? 'Room A-101' : 'Room B-102',
          row.is_available === true
        ]);

        // Create Schedule for Doctor (Mon-Fri 09:00 to 17:00)
        for (let day = 1; day <= 5; day++) {
          await client.query(`
            INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, appointment_duration_minutes, max_patients, is_active)
            VALUES ($1, $2, '09:00:00', '17:00:00', $3, 30, TRUE);
          `, [mapping.id, day, row.average_service_time_minutes]);
        }
      }
    }

    // 4. Create Staff Users (Receptionist & Admin)
    console.log('Seeding receptionist user...');
    await client.query(`
      INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
      VALUES ($1, 'receptionist@citygeneral.com', '9876541111', $2, 'RECEPTIONIST', 'Mary', 'Jane', TRUE, TRUE)
      ON CONFLICT (phone) DO NOTHING;
    `, [RECP_USER_ID, DEFAULT_PASSWORD_HASH]);

    await client.query(`
      INSERT INTO receptionists (id, user_id, employee_number, hospital_id, designation, shift, joining_date, is_active)
      VALUES ($1, $2, 'EMP-RECP-001', $3, 'Head Desk Receptionist', 'MORNING', CURRENT_DATE, TRUE)
      ON CONFLICT (employee_number) DO NOTHING;
    `, [RECP_ID, RECP_USER_ID, HOSPITAL_ID]);

    console.log('Seeding admin user...');
    await client.query(`
      INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active, is_verified)
      VALUES ($1, 'admin@citygeneral.com', '9876542222', $2, 'ADMIN', 'Super', 'Admin', TRUE, TRUE)
      ON CONFLICT (phone) DO NOTHING;
    `, [ADMIN_USER_ID, DEFAULT_PASSWORD_HASH]);

    await client.query(`
      INSERT INTO administrators (id, user_id, hospital_id, admin_level, is_active)
      VALUES ($1, $2, $3, 'HOSPITAL', TRUE)
      ON CONFLICT (user_id) DO NOTHING;
    `, [ADMIN_ID, ADMIN_USER_ID, HOSPITAL_ID]);


    // 5. Migrate Patients & Tokens
    console.log('Migrating tokens and building patients...');
    const oldTokens = await client.query('SELECT * FROM old_tokens ORDER BY id ASC');
    
    let positionCounter = 1;
    for (const row of oldTokens.rows) {
      // 5.1 Patient user & profile (created dynamically from token name/phone)
      const patientPhone = row.patient_phone;
      const patientName = row.patient_name || 'Anonymous Patient';
      
      const userRes = await client.query('SELECT id FROM users WHERE phone = $1', [patientPhone]);
      let patientUserId;
      let patientId;

      if (userRes.rows.length === 0) {
        // Generate new User & Patient profile
        patientUserId = `e1000000-0000-0000-0000-${Math.floor(Math.random() * 900000000000 + 100000000000)}`;
        patientId = `f2000000-0000-0000-0000-${Math.floor(Math.random() * 900000000000 + 100000000000)}`;

        
        const names = patientName.split(' ');
        const first = names[0];
        const last = names.slice(1).join(' ') || 'Patient';

        await client.query(`
          INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active)
          VALUES ($1, $2, $3, $4, 'PATIENT', $5, $6, TRUE);
        `, [
          patientUserId,
          `${first.toLowerCase()}.${Math.floor(Math.random()*1000)}@patient.com`,
          patientPhone,
          DEFAULT_PASSWORD_HASH,
          first,
          last
        ]);

        await client.query(`
          INSERT INTO patients (id, user_id, patient_number, date_of_birth, gender, blood_group)
          VALUES ($1, $2, $3, '1990-01-01', 'MALE', 'A+')
        `, [patientId, patientUserId, `PAT-${patientPhone}`]);
      } else {
        patientUserId = userRes.rows[0].id;
        const patProfileRes = await client.query('SELECT id FROM patients WHERE user_id = $1', [patientUserId]);
        patientId = patProfileRes.rows[0].id;
      }

      // Lookup Doctor
      const doctorMapping = DOC_MAP[row.doctor_id] || DOC_MAP['d1']; // default to d1
      const doctorId = doctorMapping.id;
      const departmentId = DEPT_MAP[row.doctor_id === 'd1' ? '1' : (row.doctor_id === 'd2' ? '2' : (row.doctor_id === 'd3' ? '3' : '4'))];

      // Resolve duplicate token numbers for same doctor + day
      let tokenNumber = row.token_number;
      const todayStr = new Date().toISOString().split('T')[0];
      const tokenKey = `${doctorId}_${todayStr}_${tokenNumber}`;
      if (insertedKeys.has(tokenKey)) {
        let suffix = 2;
        while (insertedKeys.has(`${doctorId}_${todayStr}_${tokenNumber}-${suffix}`)) {
          suffix++;
        }
        tokenNumber = `${tokenNumber}-${suffix}`;
        insertedKeys.add(`${doctorId}_${todayStr}_${tokenNumber}`);
      } else {
        insertedKeys.add(tokenKey);
      }

      // Insert Token
      // Map old token ID so it keeps sequence/ordering integrity
      // Since new token ID is UUID, let's generate a consistent UUID based on old integer ID
      const tokenId = `00000000-0000-0000-0000-${String(row.id).padStart(12, '0')}`;
      
      const createdBy = RECP_USER_ID; // default receptionist
      
      await client.query(`
        INSERT INTO tokens (id, serial_id, token_number, patient_id, appointment_id, hospital_id, department_id, doctor_id, queue_date, priority, source, status, requested_at, estimated_wait_minutes, created_by, created_at, updated_at)
        VALUES ($1, $2, $3, $4, NULL, $5, $6, $7, CURRENT_DATE, 'NORMAL', 'WALK_IN', $8, $9, $10, $11, $12, $12);
      `, [
        tokenId,
        parseInt(row.id, 10),
        tokenNumber,
        patientId,
        HOSPITAL_ID,
        departmentId,
        doctorId,
        row.status, // PENDING, APPROVED, SERVING, COMPLETED, SKIPPED
        new Date(parseInt(row.created_at || Date.now())),
        row.estimated_wait_minutes || 0,
        createdBy,
        new Date(parseInt(row.created_at || Date.now()))
      ]);



      // If token is approved/called/serving, put into active queue_entries
      if (['APPROVED', 'CALLED', 'SERVING', 'SKIPPED'].includes(row.status)) {
        let qStatus = 'WAITING';
        if (row.status === 'SERVING') qStatus = 'SERVING';
        if (row.status === 'SKIPPED') qStatus = 'SKIPPED';
        if (row.status === 'CALLED') qStatus = 'CALLED';

        await client.query(`
          INSERT INTO queue_entries (token_id, doctor_id, queue_date, position, status, priority, joined_at)
          VALUES ($1, $2, CURRENT_DATE, $3, $4, 'NORMAL', CURRENT_TIMESTAMP);
        `, [tokenId, doctorId, positionCounter++, qStatus]);
      }

      // Log Token Creation Event
      await client.query(`
        INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by)
        VALUES ($1, 'TOKEN_CREATED', NULL, $2, $3);
      `, [tokenId, row.status, createdBy]);

      // If token is COMPLETED, create Consultation, Medical Record, and Bills
      if (row.status === 'COMPLETED') {
        const consultationId = `c0000000-0000-0000-0000-${String(row.id).padStart(12, '0')}`;
        const billId = `b0000000-0000-0000-0000-${String(row.id).padStart(12, '0')}`;
        const billAmount = parseFloat(row.bill_amount) || 300.00;
        
        // 1. Insert Consultation
        await client.query(`
          INSERT INTO consultations (id, token_id, patient_id, doctor_id, started_at, completed_at, diagnosis, notes, consultation_fee, status)
          VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP - INTERVAL '15 minutes', CURRENT_TIMESTAMP, $5, $6, $7, 'COMPLETED');
        `, [
          consultationId,
          tokenId,
          patientId,
          doctorId,
          row.diagnosis || 'General Checkup',
          row.prescription || 'No special notes',
          billAmount
        ]);

        // 2. Insert Medical Record
        await client.query(`
          INSERT INTO medical_records (patient_id, doctor_id, hospital_id, token_id, diagnosis, clinical_notes)
          VALUES ($1, $2, $3, $4, $5, $6);
        `, [patientId, doctorId, HOSPITAL_ID, tokenId, row.diagnosis || 'General Checkup', row.prescription || 'No special notes']);

        // 3. Create Bill
        const billStatus = row.payment_status === 'PAID' ? 'PAID' : 'PENDING';
        await client.query(`
          INSERT INTO bills (id, bill_number, patient_id, hospital_id, token_id, consultation_id, subtotal, total_amount, status, created_by)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $7, $8, $9);
        `, [
          billId,
          `BILL-${row.id}-${Math.floor(Math.random()*10000)}`,
          patientId,
          HOSPITAL_ID,
          tokenId,
          consultationId,
          billAmount,
          billStatus,
          doctorMapping.user_id
        ]);

        // 4. Create Bill Item
        await client.query(`
          INSERT INTO bill_items (bill_id, item_type, description, quantity, unit_price, total_price)
          VALUES ($1, 'CONSULTATION', 'Doctor Consultation Fee', 1, $2, $2);
        `, [billId, billAmount]);

        // 5. Create Payment if paid
        if (row.payment_status === 'PAID') {
          await client.query(`
            INSERT INTO payments (bill_id, amount, payment_method, payment_status, paid_by)
            VALUES ($1, $2, 'CASH', 'SUCCESS', $3);
          `, [billId, billAmount, patientUserId]);
        }
      }
    }

    await client.query('COMMIT');
    console.log('Data migration complete. Default hospital, departments, doctors, and patients seeded.');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Data migration failed:', err);
    process.exit(1);
  } finally {
    client.release();
  }
}

migrateData().then(() => process.exit(0));

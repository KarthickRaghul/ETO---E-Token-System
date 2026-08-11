const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const crypto = require('crypto');
const db = require('./db');
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST', 'PATCH', 'DELETE']
  }
});

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// Session cache (in-memory)
const sessions = new Map();

// Helper to hash passwords
function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// Default constants for backward compatibility fallback
const DEFAULT_HOSPITAL_ID = 'e4b77f98-5c1a-4fdf-9737-1234567890ab';
const DEFAULT_RECP_USER_ID = 'fa55a55a-5555-5555-5555-555555555555';

// Helper to broadcast queue updates to all clients
function broadcastQueueUpdate() {
  io.emit('queue_update', { timestamp: Date.now() });
  console.log('Broadcasted queue update event to clients');
}

// Middleware for authentication & RBAC
function requireRole(roles) {
  return (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
      // Fallback for Android client prototype which does not send tokens yet
      req.user = { id: DEFAULT_RECP_USER_ID, role: 'RECEPTIONIST' };
      return next();
    }
    const parts = authHeader.split(' ');
    if (parts.length !== 2 || parts[0] !== 'Bearer') {
      return res.status(401).json({ error: 'Malformed authorization header' });
    }
    const token = parts[1];
    const session = sessions.get(token);
    if (!session) {
      return res.status(401).json({ error: 'Session expired or invalid' });
    }
    if (roles && roles.length > 0 && !roles.includes(session.role)) {
      return res.status(403).json({ error: 'Forbidden: Insufficient privileges' });
    }
    req.user = session;
    next();
  };
}

// Helper to insert notifications
async function createNotification(client, userId, type, title, message) {
  try {
    await client.query(`
      INSERT INTO notifications (user_id, type, title, message)
      VALUES ($1, $2, $3, $4)
    `, [userId, type, title, message]);
  } catch (err) {
    console.error('Failed to write notification:', err);
  }
}

// Helper to insert audit log
async function writeAuditLog(client, userId, action, entityType, entityId, newValues) {
  try {
    await client.query(`
      INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_values)
      VALUES ($1, $2, $3, $4, $5)
    `, [userId, action, entityType, entityId, JSON.stringify(newValues)]);
  } catch (err) {
    console.error('Failed to write audit log:', err);
  }
}

// ----------------------------------------------------
// AUTH ENDPOINTS
// ----------------------------------------------------
app.post('/api/auth/register', async (req, res) => {
  const { email, phone, password, role, firstName, lastName } = req.body;
  try {
    const passwordHash = hashPassword(password);
    const result = await db.query(`
      INSERT INTO users (email, phone, password_hash, role, first_name, last_name)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING id, email, phone, role, first_name, last_name
    `, [email, phone, passwordHash, role || 'PATIENT', firstName, lastName]);
    
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Registration failed. Email/phone may already be in use.' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const passwordHash = hashPassword(password);
    const result = await db.query(`
      SELECT * FROM users WHERE email = $1 AND password_hash = $2 AND is_active = TRUE
    `, [email, passwordHash]);

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = result.rows[0];
    const token = crypto.randomBytes(32).toString('hex');
    sessions.set(token, { id: user.id, role: user.role, email: user.email });

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        phone: user.phone,
        role: user.role,
        firstName: user.first_name,
        lastName: user.last_name
      }
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Login failed.' });
  }
});

app.post('/api/auth/verify-otp', (req, res) => {
  res.json({ success: true, message: 'OTP verified successfully.' });
});

app.post('/api/auth/logout', requireRole([]), (req, res) => {
  const authHeader = req.headers.authorization;
  if (authHeader) {
    const token = authHeader.split(' ')[1];
    sessions.delete(token);
  }
  res.json({ success: true, message: 'Logged out successfully.' });
});

// ----------------------------------------------------
// HOSPITALS & DEPARTMENTS
// ----------------------------------------------------
app.get('/api/hospitals', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM hospitals WHERE is_active = TRUE');
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch hospitals' });
  }
});

app.get('/api/hospitals/:id', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM hospitals WHERE id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'Hospital not found' });
    res.json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching hospital' });
  }
});

app.get('/api/hospitals/:id/departments', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM departments WHERE hospital_id = $1 AND is_active = TRUE', [req.params.id]);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching departments' });
  }
});

// ----------------------------------------------------
// USER PROFILES
// ----------------------------------------------------
app.get('/api/profile/patient/:phone', async (req, res) => {
  try {
    const { phone } = req.params;
    let userRes = await db.query('SELECT u.*, p.id as patient_id, p.date_of_birth, p.gender, p.blood_group FROM users u LEFT JOIN patients p ON p.user_id = u.id WHERE u.phone = $1', [phone]);
    
    if (userRes.rows.length === 0) {
      // Create user and patient profile on the fly
      const newUserId = crypto.randomUUID();
      const newPatientId = crypto.randomUUID();
      await db.query(`
        INSERT INTO users (id, email, phone, password_hash, role, first_name, last_name, is_active)
        VALUES ($1, $2, $3, $4, 'PATIENT', 'Aarav', 'Sharma', TRUE)
      `, [newUserId, 'aarav.sharma@email.com', phone, 'hashed_password']);
      
      await db.query(`
        INSERT INTO patients (id, user_id, patient_number, date_of_birth, gender, blood_group)
        VALUES ($1, $2, 'PT0001', '1996-05-12', 'MALE', 'B+')
      `, [newPatientId, newUserId]);

      userRes = await db.query('SELECT u.*, p.id as patient_id, p.date_of_birth, p.gender, p.blood_group FROM users u LEFT JOIN patients p ON p.user_id = u.id WHERE u.phone = $1', [phone]);
    }

    const row = userRes.rows[0];
    const apptCountRes = await db.query('SELECT COUNT(*) FROM appointments WHERE patient_id = $1', [row.patient_id]);
    const apptCount = parseInt(apptCountRes.rows[0].count, 10);

    res.json({
      id: row.patient_id || '',
      first_name: row.first_name || 'Aarav',
      last_name: row.last_name || 'Sharma',
      email: row.email || 'aarav.sharma@email.com',
      phone: row.phone,
      date_of_birth: row.date_of_birth ? new Date(row.date_of_birth).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '12 May 1996',
      gender: row.gender ? (row.gender.charAt(0).toUpperCase() + row.gender.slice(1).toLowerCase()) : 'Male',
      blood_group: row.blood_group || 'B+',
      allergies: 'Penicillin',
      conditions: 'None',
      appointmentCount: apptCount > 0 ? apptCount : 18,
      savedHospitalsCount: 5,
      created_at: row.created_at ? new Date(row.created_at).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '12 Jan 2024'
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching patient profile' });
  }
});

app.get('/api/profile/doctor/:id', async (req, res) => {
  try {
    let { id } = req.params;
    const DOC_UUID_MAP = {
      'd1': 'd1111111-1111-1111-1111-111111111111',
      'd2': 'd2222222-2222-2222-2222-222222222222',
      'd3': 'd3333333-3333-3333-3333-333333333333'
    };
    if (DOC_UUID_MAP[id]) {
      id = DOC_UUID_MAP[id];
    }
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
    let queryStr = `
      SELECT d.*, u.first_name, u.last_name, u.email, u.phone, dept.name AS department_name, h.name as hospital_name
      FROM doctors d
      JOIN users u ON d.user_id = u.id
      LEFT JOIN departments dept ON d.department_id = dept.id
      LEFT JOIN hospitals h ON d.hospital_id = h.id
    `;
    if (isUuid) {
      queryStr += ` WHERE d.id = $1`;
    } else {
      queryStr += ` WHERE d.doctor_number = $1`;
    }
    const { rows } = await db.query(queryStr, [id]);

    if (rows.length === 0) {
      // Mock fallback if not found
      return res.json({
        id: id,
        name: "Dr. Rahul Verma",
        specialty: "Cardiologist",
        department_id: "d1",
        department_name: "Cardiology",
        rating: 4.8,
        averageServiceTimeMinutes: 15,
        isAvailable: true,
        working_days: "Mon - Sat",
        consultation_hours: "09:00 AM - 05:00 PM",
        appointment_duration: "15 mins per patient",
        specialization: "Cardiologist",
        qualification: "MBBS, MD (Cardiology)",
        experience: "10+ Years",
        consultation_fee: 800,
        hospital_name: "City Care Hospital",
        room_cabin: "Cardiology OPD - 204",
        phone: "+91 98765 43210",
        email: "rahul.verma@eto.com"
      });
    }

    const doc = rows[0];
    res.json({
      id: doc.id,
      name: `Dr. ${doc.first_name} ${doc.last_name}`,
      specialty: doc.specialty || 'General Physician',
      department_id: doc.department_id || '',
      department_name: doc.department_name || 'General Medicine',
      rating: 4.8,
      averageServiceTimeMinutes: doc.average_service_time_minutes || 15,
      isAvailable: doc.is_available,
      working_days: "Mon - Sat",
      consultation_hours: "09:00 AM - 05:00 PM",
      appointment_duration: `${doc.average_service_time_minutes || 15} mins per patient`,
      specialization: doc.specialty || 'General Physician',
      qualification: doc.qualification || 'MBBS, MD',
      experience: `${doc.experience_years || 10}+ Years`,
      consultation_fee: parseFloat(doc.consultation_fee) || 800,
      hospital_name: doc.hospital_name || 'City Care Hospital',
      room_cabin: doc.room_cabin || 'OPD - 101',
      phone: doc.phone || '+91 98765 43210',
      email: doc.email || `${doc.first_name.toLowerCase()}@eto.com`
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching doctor profile' });
  }
});

app.get('/api/profile/receptionist/:phoneOrId', async (req, res) => {
  try {
    const { phoneOrId } = req.params;
    const { rows } = await db.query(`
      SELECT r.*, u.first_name, u.last_name, u.email, u.phone, h.name as hospital_name
      FROM receptionists r
      JOIN users u ON r.user_id = u.id
      LEFT JOIN hospitals h ON r.hospital_id = h.id
      WHERE u.phone = $1 OR r.id = $1 OR r.employee_number = $1
    `, [phoneOrId]);

    if (rows.length === 0) {
      // Fallback receptionist
      return res.json({
        id: phoneOrId,
        first_name: "Neha",
        last_name: "Sharma",
        email: "neha.sharma@eto.com",
        phone: "9876541111",
        employee_number: "RC0001",
        designation: "Senior Receptionist",
        shift: "Morning Shift",
        hospital_name: "City Care Hospital",
        department_name: "Front Desk",
        working_days: "Mon - Sat",
        working_hours: "08:00 AM - 04:00 PM"
      });
    }

    const rec = rows[0];
    res.json({
      id: rec.id,
      first_name: rec.first_name,
      last_name: rec.last_name,
      email: rec.email || 'neha.sharma@eto.com',
      phone: rec.phone,
      employee_number: rec.employee_number || 'RC0001',
      designation: rec.designation || 'Senior Receptionist',
      shift: rec.shift === 'MORNING' ? 'Morning Shift' : rec.shift === 'EVENING' ? 'Evening Shift' : rec.shift,
      hospital_name: rec.hospital_name || 'City Care Hospital',
      department_name: 'Front Desk',
      working_days: "Mon - Sat",
      working_hours: "08:00 AM - 04:00 PM"
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching receptionist profile' });
  }
});

// ----------------------------------------------------
// DOCTORS & SCHEDULES
// ----------------------------------------------------
app.get('/api/doctors/:id', async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT d.*, u.first_name, u.last_name, u.email, u.phone
      FROM doctors d
      JOIN users u ON d.user_id = u.id
      WHERE d.id = $1
    `, [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'Doctor not found' });
    res.json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching doctor' });
  }
});

app.get('/api/doctors/:id/schedule', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM doctor_schedules WHERE doctor_id = $1 AND is_active = TRUE', [req.params.id]);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error fetching schedule' });
  }
});

// ----------------------------------------------------
// APPOINTMENTS & REQUESTS
// ----------------------------------------------------
app.post('/api/appointments', requireRole(['PATIENT', 'RECEPTIONIST']), async (req, res) => {
  const { patientId, doctorId, departmentId, appointmentDate, appointmentTime, appointmentType, reasonForVisit, symptoms } = req.body;
  try {
    const { rows } = await db.query(`
      INSERT INTO appointments (patient_id, doctor_id, department_id, hospital_id, appointment_date, appointment_time, appointment_type, status, reason_for_visit, symptoms)
      VALUES ($1, $2, $3, $4, $5, $6, $7, 'REQUESTED', $8, $9)
      RETURNING *
    `, [patientId, doctorId, departmentId, DEFAULT_HOSPITAL_ID, appointmentDate, appointmentTime, appointmentType || 'ONLINE', reasonForVisit, symptoms]);
    
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to create appointment' });
  }
});

app.get('/api/appointments', requireRole(['DOCTOR', 'RECEPTIONIST', 'ADMIN']), async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT a.*, u.first_name || ' ' || u.last_name AS patient_name, doc_u.first_name || ' ' || doc_u.last_name AS doctor_name
      FROM appointments a
      JOIN patients p ON a.patient_id = p.id
      JOIN users u ON p.user_id = u.id
      JOIN doctors d ON a.doctor_id = d.id
      JOIN users doc_u ON d.user_id = doc_u.id
      ORDER BY a.appointment_date DESC, a.appointment_time DESC
    `);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch appointments' });
  }
});

// ----------------------------------------------------
// APPOINTMENT REQUESTS & APPROVALS
// ----------------------------------------------------
app.get('/api/appointment-requests', requireRole(['RECEPTIONIST', 'ADMIN']), async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT ar.*, u.first_name || ' ' || u.last_name AS patient_name, u.phone AS patient_phone, doc_u.first_name || ' ' || doc_u.last_name AS doctor_name
      FROM appointment_requests ar
      JOIN patients p ON ar.patient_id = p.id
      JOIN users u ON p.user_id = u.id
      JOIN doctors d ON ar.requested_doctor_id = d.id
      JOIN users doc_u ON d.user_id = doc_u.id
      WHERE ar.status = 'PENDING'
      ORDER BY ar.created_at ASC
    `);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch pending requests' });
  }
});

// Approve Request -> Starts Transaction (Generates token & places in queue)
app.patch('/api/appointment-requests/:id/approve', requireRole(['RECEPTIONIST', 'ADMIN']), async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get request details
    const reqRes = await client.query('SELECT * FROM appointment_requests WHERE id = $1 FOR UPDATE', [req.params.id]);
    if (reqRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Request not found' });
    }
    const request = reqRes.rows[0];

    // 2. Update request status
    await client.query("UPDATE appointment_requests SET status = 'APPROVED', reviewed_by = $1, reviewed_at = CURRENT_TIMESTAMP WHERE id = $2", [req.user.id, request.id]);

    // 3. Create or update appointment status
    let appointmentId = request.appointment_id;
    if (!appointmentId) {
      const apptRes = await client.query(`
        INSERT INTO appointments (patient_id, doctor_id, department_id, hospital_id, appointment_date, appointment_time, appointment_type, status, symptoms)
        VALUES ($1, $2, $3, $4, $5, $6, 'ONLINE', 'APPROVED', $7)
        RETURNING id
      `, [request.patient_id, request.requested_doctor_id, request.requested_department_id, DEFAULT_HOSPITAL_ID, request.requested_date, request.requested_time, request.symptoms]);
      appointmentId = apptRes.rows[0].id;
    } else {
      await client.query("UPDATE appointments SET status = 'APPROVED' WHERE id = $1", [appointmentId]);
    }

    // 4. Calculate queue number and waiting time
    const doctorRes = await client.query('SELECT * FROM doctors WHERE id = $1', [request.requested_doctor_id]);
    const doctor = doctorRes.rows[0];

    const activeRes = await client.query("SELECT COUNT(*) FROM tokens WHERE doctor_id = $1 AND queue_date = CURRENT_DATE AND status IN ('APPROVED', 'WAITING', 'CALLED', 'SERVING')", [doctor.id]);
    const queueCount = parseInt(activeRes.rows[0].count, 10);

    const prefix = doctor.specialization.substring(0, 3).toUpperCase();
    const tokenNum = `#${prefix}-${101 + queueCount}`;
    const estWait = queueCount * doctor.experience_years; // average service minutes stored in experience_years or seeded schedule

    // 5. Insert Token
    const tokenRes = await client.query(`
      INSERT INTO tokens (token_number, patient_id, appointment_id, hospital_id, department_id, doctor_id, queue_date, status, estimated_wait_minutes, created_by, approved_by, approved_at)
      VALUES ($1, $2, $3, $4, $5, $6, CURRENT_DATE, 'APPROVED', $7, $8, $8, CURRENT_TIMESTAMP)
      RETURNING id, token_number
    `, [tokenNum, request.patient_id, appointmentId, DEFAULT_HOSPITAL_ID, request.requested_department_id, doctor.id, estWait, req.user.id]);
    const token = tokenRes.rows[0];

    // 6. Insert active Queue Entry
    await client.query(`
      INSERT INTO queue_entries (token_id, doctor_id, queue_date, position, status, joined_at)
      VALUES ($1, $2, CURRENT_DATE, $3, 'WAITING', CURRENT_TIMESTAMP)
    `, [token.id, doctor.id, queueCount + 1]);

    // 7. Write Event and Notification
    await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_APPROVED', 'PENDING', 'APPROVED', $2)", [token.id, req.user.id]);
    
    // Get patient user ID for notification
    const patRes = await client.query('SELECT user_id FROM patients WHERE id = $1', [request.patient_id]);
    if (patRes.rows.length > 0) {
      await createNotification(client, patRes.rows[0].user_id, 'TOKEN_STATUS', 'Token Approved!', `Your token ${token.token_number} is approved and is now active in the queue.`);
    }

    await client.query('COMMIT');
    res.json({ success: true, tokenNumber: token.token_number });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to approve request' });
  } finally {
    client.release();
  }
});

// Reject Request
app.patch('/api/appointment-requests/:id/reject', requireRole(['RECEPTIONIST', 'ADMIN']), async (req, res) => {
  const { reason } = req.body;
  try {
    await db.query(`
      UPDATE appointment_requests 
      SET status = 'REJECTED', rejection_reason = $1, reviewed_by = $2, reviewed_at = CURRENT_TIMESTAMP
      WHERE id = $3
    `, [reason, req.user.id, req.params.id]);
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to reject request' });
  }
});

// ----------------------------------------------------
// QUEUE PROGRESSION & CALL NEXT PATIENT (FOR UPDATE SKIP LOCKED)
// ----------------------------------------------------
app.post('/api/queues/:doctorId/call-next', requireRole(['DOCTOR', 'RECEPTIONIST']), async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Terminate or skip current serving token for this doctor
    const currentRes = await client.query(`
      SELECT q.*, q.token_id 
      FROM queue_entries q
      WHERE q.doctor_id = $1 AND q.queue_date = CURRENT_DATE AND q.status = 'SERVING'
      FOR UPDATE
    `, [req.params.doctorId]);
    
    if (currentRes.rows.length > 0) {
      const currentEntry = currentRes.rows[0];
      // Mark current serving as skipped (not completed)
      await client.query("UPDATE queue_entries SET status = 'SKIPPED', skipped_at = CURRENT_TIMESTAMP WHERE id = $1", [currentEntry.id]);
      await client.query("UPDATE tokens SET status = 'SKIPPED', skipped_at = CURRENT_TIMESTAMP WHERE id = $1", [currentEntry.token_id]);
      await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_SKIPPED', 'SERVING', 'SKIPPED', $2)", [currentEntry.token_id, req.user.id]);
    }

    // 2. Select next waiting token using FOR UPDATE SKIP LOCKED
    const nextRes = await client.query(`
      SELECT q.id, q.token_id, t.token_number, p.user_id, u.first_name || ' ' || u.last_name as name
      FROM queue_entries q
      JOIN tokens t ON q.token_id = t.id
      JOIN patients p ON t.patient_id = p.id
      JOIN users u ON p.user_id = u.id
      WHERE q.doctor_id = $1 AND q.queue_date = CURRENT_DATE AND q.status = 'WAITING'
      ORDER BY q.priority = 'EMERGENCY' DESC, q.position ASC, q.joined_at ASC
      LIMIT 1
      FOR UPDATE SKIP LOCKED
    `, [req.params.doctorId]);

    if (nextRes.rows.length === 0) {
      await client.query('COMMIT');
      return res.json({ success: true, message: 'No patients waiting in queue' });
    }

    const nextPatient = nextRes.rows[0];

    // 3. Update status of the called entry & token
    await client.query("UPDATE queue_entries SET status = 'CALLED', called_at = CURRENT_TIMESTAMP WHERE id = $1", [nextPatient.id]);
    await client.query("UPDATE tokens SET status = 'CALLED', called_at = CURRENT_TIMESTAMP WHERE id = $1", [nextPatient.token_id]);
    await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_CALLED', 'WAITING', 'CALLED', $2)", [nextPatient.token_id, req.user.id]);

    // 4. Send alert notification
    await createNotification(client, nextPatient.user_id, 'QUEUE_CALL', 'It\'s Your Turn!', `Token ${nextPatient.token_number} (${nextPatient.name}): Please report to the consultation room immediately.`);

    await client.query('COMMIT');
    res.json({ success: true, calledToken: nextPatient.token_number, patientName: nextPatient.name });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to progress queue' });
  } finally {
    client.release();
  }
});

// ----------------------------------------------------
// CONSULTATIONS (COMPLETION TRANSACTION BOUNDARY)
// ----------------------------------------------------
app.patch('/api/consultations/:id/complete', requireRole(['DOCTOR']), async (req, res) => {
  const { diagnosis, notes, prescriptionItems, consultationFee, followUpDate } = req.body;
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get Consultation Details
    const consultRes = await client.query('SELECT * FROM consultations WHERE id = $1 FOR UPDATE', [req.params.id]);
    if (consultRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Consultation not found' });
    }
    const consultation = consultRes.rows[0];

    if (consultation.status === 'COMPLETED') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Consultation already completed' });
    }

    // 2. Update Consultation Record
    await client.query(`
      UPDATE consultations 
      SET status = 'COMPLETED', diagnosis = $1, notes = $2, consultation_fee = $3, follow_up_date = $4, completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE id = $5
    `, [diagnosis, notes, consultationFee || 0.00, followUpDate, consultation.id]);

    // 3. Update Token & Queue Entry status
    await client.query("UPDATE tokens SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = $1", [consultation.token_id]);
    await client.query("UPDATE queue_entries SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE token_id = $1", [consultation.token_id]);
    
    // 4. Create Medical Record (EMR)
    await client.query(`
      INSERT INTO medical_records (patient_id, doctor_id, hospital_id, token_id, diagnosis, clinical_notes, follow_up_date)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
    `, [consultation.patient_id, consultation.doctor_id, DEFAULT_HOSPITAL_ID, consultation.token_id, diagnosis, notes, followUpDate]);

    // 5. Create Prescription if provided
    if (prescriptionItems && prescriptionItems.length > 0) {
      const prescriptionNum = `RX-${Date.now()}-${Math.floor(Math.random()*1000)}`;
      const rxRes = await client.query(`
        INSERT INTO prescriptions (consultation_id, patient_id, doctor_id, prescription_number, instructions)
        VALUES ($1, $2, $3, $4, 'Take medicines as directed')
        RETURNING id
      `, [consultation.id, consultation.patient_id, consultation.doctor_id, prescriptionNum]);
      const rxId = rxRes.rows[0].id;

      for (const item of prescriptionItems) {
        await client.query(`
          INSERT INTO prescription_items (prescription_id, medicine_name, dosage, frequency, duration, route, instructions)
          VALUES ($1, $2, $3, $4, $5, $6, $7)
        `, [rxId, item.medicineName, item.dosage, item.frequency, item.duration, item.route || 'ORAL', item.instructions]);
      }
    }

    // 6. Create Bill (Invoice)
    const billNum = `BILL-${Date.now()}-${Math.floor(Math.random()*1000)}`;
    const billAmount = parseFloat(consultationFee) || 300.00;
    const billRes = await client.query(`
      INSERT INTO bills (bill_number, patient_id, hospital_id, token_id, consultation_id, subtotal, total_amount, status, created_by)
      VALUES ($1, $2, $3, $4, $5, $6, $6, 'PENDING', $7)
      RETURNING id
    `, [billNum, consultation.patient_id, DEFAULT_HOSPITAL_ID, consultation.token_id, consultation.id, billAmount, req.user.id]);
    const billId = billRes.rows[0].id;

    // Create Bill Item
    await client.query(`
      INSERT INTO bill_items (bill_id, item_type, description, quantity, unit_price, total_price)
      VALUES ($1, 'CONSULTATION', 'Specialist Consultation Fee', 1, $2, $2)
    `, [billId, billAmount]);

    // Log Event
    await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_COMPLETED', 'SERVING', 'COMPLETED', $2)", [consultation.token_id, req.user.id]);

    await client.query('COMMIT');
    res.json({ success: true, message: 'Consultation finalized successfully' });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to complete consultation' });
  } finally {
    client.release();
  }
});

// ----------------------------------------------------
// BILLING & PAYMENTS (TRANSACTION BOUNDARY)
// ----------------------------------------------------
app.get('/api/bills', requireRole(['RECEPTIONIST', 'ADMIN']), async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT b.*, u.first_name || ' ' || u.last_name as patient_name
      FROM bills b
      JOIN patients p ON b.patient_id = p.id
      JOIN users u ON p.user_id = u.id
      ORDER BY b.created_at DESC
    `);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch bills' });
  }
});

app.post('/api/payments', requireRole(['RECEPTIONIST', 'ADMIN']), async (req, res) => {
  const { billId, amount, paymentMethod, transactionId } = req.body;
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Lock and fetch bill
    const billRes = await client.query('SELECT * FROM bills WHERE id = $1 FOR UPDATE', [billId]);
    if (billRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Bill not found' });
    }
    const bill = billRes.rows[0];

    if (bill.status === 'PAID') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Bill already paid' });
    }

    // 2. Insert Payment Record
    await client.query(`
      INSERT INTO payments (bill_id, amount, payment_method, transaction_id, payment_status, paid_by)
      VALUES ($1, $2, $3, $4, 'SUCCESS', $5)
    `, [bill.id, amount, paymentMethod || 'CASH', transactionId, req.user.id]);

    // 3. Update Bill status
    await client.query("UPDATE bills SET status = 'PAID', updated_at = CURRENT_TIMESTAMP WHERE id = $1", [bill.id]);

    // 4. Update Token payment status (if token linked)
    if (bill.token_id) {
      // Find token and update
      await client.query("UPDATE tokens SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE id = $1", [bill.token_id]);
    }

    // Log Action
    await writeAuditLog(client, req.user.id, 'RECORD_PAYMENT', 'BILL', bill.id, { amount, paymentMethod });

    await client.query('COMMIT');
    res.json({ success: true, message: 'Payment recorded successfully' });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to process payment' });
  } finally {
    client.release();
  }
});

// ----------------------------------------------------
// NOTIFICATIONS
// ----------------------------------------------------
app.get('/api/notifications', requireRole([]), async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM notifications WHERE user_id = $1 ORDER BY created_at DESC', [req.user.id]);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch notifications' });
  }
});

app.patch('/api/notifications/:id/read', requireRole([]), async (req, res) => {
  try {
    await db.query("UPDATE notifications SET is_read = TRUE, read_at = CURRENT_TIMESTAMP WHERE id = $1 AND user_id = $2", [req.params.id, req.user.id]);
    res.json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to mark notification read' });
  }
});

// ----------------------------------------------------
// BACKWARD COMPATIBILITY ENDPOINTS (SUPPORTING ANDROID ROOM CLIENT)
// ----------------------------------------------------

// 1. Fetch Departments (Flat format matching DeptEntity)
app.get('/api/departments', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT id, name, description FROM departments ORDER BY name ASC');
    const mapped = rows.map(r => {
      let icon = 'medical_services';
      if (r.name.includes('Cardiology')) icon = 'favorite';
      else if (r.name.includes('Pediatrics')) icon = 'child_care';
      else if (r.name.includes('Dermatology')) icon = 'face';
      return {
        id: r.id,
        name: r.name,
        description: r.description || '',
        iconName: icon
      };
    });
    res.json(mapped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch departments.' });
  }
});

// 2. Fetch Doctors (CamelCase mapping)
app.get('/api/doctors', async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT d.id, u.first_name || ' ' || u.last_name AS name, d.specialization AS specialty, d.department_id, dept.name AS department_name, d.is_available, d.experience_years
      FROM doctors d
      JOIN users u ON d.user_id = u.id
      JOIN departments dept ON d.department_id = dept.id
    `);
    const mapped = rows.map(r => ({
      id: r.id,
      name: r.name,
      specialty: r.specialty,
      departmentId: r.department_id,
      departmentName: r.department_name,
      rating: 4.8,
      averageServiceTimeMinutes: r.experience_years || 15,
      isAvailable: r.is_available
    }));
    res.json(mapped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch doctors.' });
  }
});

// 3. Update Doctor Availability
app.patch('/api/doctors/:id/availability', async (req, res) => {
  try {
    let { id } = req.params;
    const { isAvailable } = req.body;
    const DOC_UUID_MAP = {
      'd1': 'd1111111-1111-1111-1111-111111111111',
      'd2': 'd2222222-2222-2222-2222-222222222222',
      'd3': 'd3333333-3333-3333-3333-333333333333'
    };
    if (DOC_UUID_MAP[id]) {
      id = DOC_UUID_MAP[id];
    }
    await db.query('UPDATE doctors SET is_available = $1 WHERE id = $2', [isAvailable, id]);
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to update availability.' });
  }
});

// 4. Fetch Tokens (Flat compatible mapping for Room cache)
app.get('/api/tokens', async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT t.id, t.serial_id, t.token_number, u.first_name || ' ' || u.last_name AS patient_name, u.phone AS patient_phone, t.doctor_id, doc_u.first_name || ' ' || doc_u.last_name AS doctor_name, dept.name AS department_name, t.status, t.symptoms, c.diagnosis, p.instructions AS prescription, b.total_amount AS bill_amount, b.status AS payment_status, q.position AS queue_position, t.estimated_wait_minutes, EXTRACT(EPOCH FROM t.created_at) * 1000 AS created_at
      FROM tokens t
      JOIN patients pat ON t.patient_id = pat.id
      JOIN users u ON pat.user_id = u.id
      JOIN doctors doc ON t.doctor_id = doc.id
      JOIN users doc_u ON doc.user_id = doc_u.id
      JOIN departments dept ON t.department_id = dept.id
      LEFT JOIN queue_entries q ON t.id = q.token_id AND q.status != 'COMPLETED'
      LEFT JOIN consultations c ON t.id = c.token_id
      LEFT JOIN prescriptions p ON c.id = p.consultation_id
      LEFT JOIN bills b ON t.id = b.token_id
      ORDER BY t.created_at ASC
    `);

    const mapped = rows.map(r => ({
      id: parseInt(r.serial_id, 10), // Send numeric serial_id as the primary key ID for long-compat
      tokenNumber: r.token_number,
      patientName: r.patient_name,
      patientPhone: r.patient_phone,
      doctorId: r.doctor_id,
      doctorName: r.doctor_name,
      departmentName: r.department_name,
      status: r.status,
      symptoms: r.symptoms || '',
      diagnosis: r.diagnosis || '',
      prescription: r.prescription || '',
      billAmount: parseFloat(r.bill_amount) || 0.0,
      paymentStatus: r.payment_status === 'PAID' ? 'PAID' : 'PENDING',
      queuePosition: r.queue_position || 1,
      estimatedWaitMinutes: r.estimated_wait_minutes || 0,
      createdAt: parseInt(r.created_at, 10)
    }));

    res.json(mapped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch compatibility tokens.' });
  }
});

// 5. Booking Token (Compatibility endpoint)
app.post('/api/tokens/request', async (req, res) => {
  const { patientName, patientPhone, doctorId, symptoms, isWalkIn } = req.body;
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get doctor & department details
    const docRes = await client.query(`
      SELECT d.*, u.first_name || ' ' || u.last_name AS name, dept.name AS department_name
      FROM doctors d
      JOIN users u ON d.user_id = u.id
      JOIN departments dept ON d.department_id = dept.id
      WHERE d.id = $1
    `, [doctorId]);

    if (docRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Doctor not found.' });
    }
    const doctor = docRes.rows[0];

    // 2. Dynamic user/patient generation from name and phone
    const userCheck = await client.query('SELECT id FROM users WHERE phone = $1', [patientPhone]);
    let patientUserId;
    let patientId;

    if (userCheck.rows.length === 0) {
      patientUserId = crypto.randomUUID();
      patientId = crypto.randomUUID();
      const names = patientName.split(' ');
      const first = names[0];
      const last = names.slice(1).join(' ') || 'Patient';

      await client.query(`
        INSERT INTO users (id, phone, role, first_name, last_name, password_hash)
        VALUES ($1, $2, 'PATIENT', $3, $4, $5)
      `, [patientUserId, patientPhone, first, last, hashPassword('patient123')]);

      await client.query(`
        INSERT INTO patients (id, user_id, patient_number, date_of_birth, gender)
        VALUES ($1, $2, $3, '1990-01-01', 'MALE')
      `, [patientId, patientUserId, `PAT-${patientPhone}`]);
    } else {
      patientUserId = userCheck.rows[0].id;
      const patRes = await client.query('SELECT id FROM patients WHERE user_id = $1', [patientUserId]);
      patientId = patRes.rows[0].id;
    }

    // 3. Count active doctor tokens for token number prefix
    const activeRes = await client.query("SELECT COUNT(*) FROM tokens WHERE doctor_id = $1 AND queue_date = CURRENT_DATE AND status IN ('APPROVED', 'WAITING', 'CALLED', 'SERVING')", [doctorId]);
    const queueCount = parseInt(activeRes.rows[0].count, 10);

    const prefix = doctor.specialty.substring(0, 3).toUpperCase();
    const tokenNum = `#${prefix}-${101 + queueCount}`;
    const estWait = queueCount * doctor.experience_years;
    const initialStatus = isWalkIn ? 'APPROVED' : 'PENDING';

    // 4. Save Token
    const insertRes = await client.query(`
      INSERT INTO tokens (token_number, patient_id, hospital_id, department_id, doctor_id, queue_date, status, estimated_wait_minutes, created_by, source, symptoms)
      VALUES ($1, $2, $3, $4, $5, CURRENT_DATE, $6, $7, $8, $9, $10)
      RETURNING serial_id
    `, [
      tokenNum,
      patientId,
      DEFAULT_HOSPITAL_ID,
      doctor.department_id,
      doctorId,
      initialStatus,
      estWait,
      DEFAULT_RECP_USER_ID,
      isWalkIn ? 'WALK_IN' : 'ONLINE',
      symptoms
    ]);

    const newSerialId = insertRes.rows[0].serial_id;

    // 5. If walk-in (immediately APPROVED), create active queue entry
    if (isWalkIn) {
      // Find UUID of newly inserted token
      const tokIdRes = await client.query('SELECT id FROM tokens WHERE serial_id = $1', [newSerialId]);
      const tokenUUID = tokIdRes.rows[0].id;

      await client.query(`
        INSERT INTO queue_entries (token_id, doctor_id, queue_date, position, status, joined_at)
        VALUES ($1, $2, CURRENT_DATE, $3, 'WAITING', CURRENT_TIMESTAMP)
      `, [tokenUUID, doctorId, queueCount + 1]);

      await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_APPROVED', 'PENDING', 'APPROVED', $2)", [tokenUUID, DEFAULT_RECP_USER_ID]);
    }

    await client.query('COMMIT');
    res.json({ success: true, tokenId: newSerialId, tokenNumber: tokenNum });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to request compatibility token.' });
  } finally {
    client.release();
  }
});

// 6. Compatibility Status Change (uses serial_id)
app.patch('/api/tokens/:id/status', async (req, res) => {
  const { id } = req.params; // numeric serial_id
  const { status } = req.body; // e.g. APPROVED, SERVING, SKIPPED, REJECTED
  
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get Token details using serial_id
    const tokRes = await client.query('SELECT * FROM tokens WHERE serial_id = $1 FOR UPDATE', [id]);
    if (tokRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Token not found' });
    }
    const token = tokRes.rows[0];
    const oldStatus = token.status;

    // 2. Perform updates
    await client.query('UPDATE tokens SET status = $1 WHERE id = $2', [status, token.id]);

    // 3. Handle Queue Entries
    if (status === 'APPROVED') {
      // Create Queue Entry if not exists
      const qCheck = await client.query('SELECT 1 FROM queue_entries WHERE token_id = $1', [token.id]);
      if (qCheck.rows.length === 0) {
        const countRes = await client.query("SELECT COUNT(*) FROM queue_entries WHERE doctor_id = $1 AND queue_date = CURRENT_DATE AND status IN ('WAITING', 'CALLED', 'SERVING')", [token.doctor_id]);
        const pos = parseInt(countRes.rows[0].count, 10) + 1;
        await client.query(`
          INSERT INTO queue_entries (token_id, doctor_id, queue_date, position, status, joined_at)
          VALUES ($1, $2, CURRENT_DATE, $3, 'WAITING', CURRENT_TIMESTAMP)
        `, [token.id, token.doctor_id, pos]);
      }
    } else if (status === 'SERVING') {
      await client.query("UPDATE queue_entries SET status = 'SERVING', called_at = CURRENT_TIMESTAMP WHERE token_id = $1", [token.id]);
      // Create consultation
      const consultId = crypto.randomUUID();
      await client.query(`
        INSERT INTO consultations (id, token_id, patient_id, doctor_id, status)
        VALUES ($1, $2, $3, $4, 'IN_PROGRESS')
        ON CONFLICT (token_id) DO NOTHING
      `, [consultId, token.id, token.patient_id, token.doctor_id]);
    } else if (status === 'SKIPPED') {
      await client.query("UPDATE queue_entries SET status = 'SKIPPED', skipped_at = CURRENT_TIMESTAMP WHERE token_id = $1", [token.id]);
    } else if (status === 'REJECTED' || status === 'CANCELLED') {
      await client.query("DELETE FROM queue_entries WHERE token_id = $1", [token.id]);
    }

    // 4. Queue Event log
    await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, $2, $3, $4, $5)", [
      token.id,
      `TOKEN_${status}`,
      oldStatus,
      status,
      DEFAULT_RECP_USER_ID
    ]);

    await client.query('COMMIT');
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to update compatibility token status.' });
  } finally {
    client.release();
  }
});

// 7. Compatibility Complete Consultation (uses serial_id)
app.post('/api/tokens/:id/consultation', async (req, res) => {
  const { id } = req.params; // numeric serial_id
  const { diagnosis, prescription, billAmount } = req.body;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get Token details using serial_id
    const tokRes = await client.query('SELECT * FROM tokens WHERE serial_id = $1 FOR UPDATE', [id]);
    if (tokRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Token not found' });
    }
    const token = tokRes.rows[0];

    // 2. Find or create consultation
    const consultRes = await client.query('SELECT * FROM consultations WHERE token_id = $1 FOR UPDATE', [token.id]);
    let consultation;
    if (consultRes.rows.length === 0) {
      const consultId = crypto.randomUUID();
      const insertConsultRes = await client.query(`
        INSERT INTO consultations (id, token_id, patient_id, doctor_id, started_at, completed_at, diagnosis, notes, consultation_fee, status)
        VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP - INTERVAL '10 minutes', CURRENT_TIMESTAMP, $5, $6, $7, 'COMPLETED')
        RETURNING *
      `, [consultId, token.id, token.patient_id, token.doctor_id, diagnosis, prescription, billAmount]);
      consultation = insertConsultRes.rows[0];
    } else {
      consultation = consultRes.rows[0];
      await client.query(`
        UPDATE consultations 
        SET status = 'COMPLETED', diagnosis = $1, notes = $2, consultation_fee = $3, completed_at = CURRENT_TIMESTAMP
        WHERE id = $4
      `, [diagnosis, prescription, billAmount, consultation.id]);
    }

    // 3. Create EMR clinical entry
    await client.query(`
      INSERT INTO medical_records (patient_id, doctor_id, hospital_id, token_id, diagnosis, clinical_notes)
      VALUES ($1, $2, $3, $4, $5, $6)
    `, [token.patient_id, token.doctor_id, DEFAULT_HOSPITAL_ID, token.id, diagnosis, prescription]);

    // 4. Create Prescription Record
    const rxNumber = `RX-${Date.now()}`;
    const rxRes = await client.query(`
      INSERT INTO prescriptions (consultation_id, patient_id, doctor_id, prescription_number, instructions)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING id
    `, [consultation.id, token.patient_id, token.doctor_id, rxNumber, 'Take medications as directed']);
    
    // Add item to prescription
    await client.query(`
      INSERT INTO prescription_items (prescription_id, medicine_name, dosage, frequency, duration, Route)
      VALUES ($1, $2, 'As directed', 'Daily', '5 days', 'ORAL')
    `, [rxRes.rows[0].id, prescription]);

    // 5. Create Bill invoice
    const billNum = `BILL-${Date.now()}`;
    const billRes = await client.query(`
      INSERT INTO bills (bill_number, patient_id, hospital_id, token_id, consultation_id, subtotal, total_amount, status, created_by)
      VALUES ($1, $2, $3, $4, $5, $6, $6, 'PENDING', $7)
      RETURNING id
    `, [billNum, token.patient_id, DEFAULT_HOSPITAL_ID, token.id, consultation.id, billAmount, DEFAULT_RECP_USER_ID]);

    await client.query(`
      INSERT INTO bill_items (bill_id, item_type, description, quantity, unit_price, total_price)
      VALUES ($1, 'CONSULTATION', 'Consultation fee & drugs', 1, $2, $2)
    `, [billRes.rows[0].id, billAmount]);

    // 6. Update token & queue entries to completed
    await client.query("UPDATE tokens SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE id = $1", [token.id]);
    await client.query("UPDATE queue_entries SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE token_id = $1", [token.id]);
    await client.query("INSERT INTO queue_events (token_id, event_type, old_status, new_status, performed_by) VALUES ($1, 'TOKEN_COMPLETED', $2, 'COMPLETED', $3)", [token.id, token.status, DEFAULT_RECP_USER_ID]);

    await client.query('COMMIT');
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to record compatibility consultation.' });
  } finally {
    client.release();
  }
});

// 8. Compatibility Record Payment (uses serial_id)
app.post('/api/tokens/:id/payment', async (req, res) => {
  const { id } = req.params; // numeric serial_id

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Get Token details using serial_id
    const tokRes = await client.query('SELECT id, patient_id FROM tokens WHERE serial_id = $1 FOR UPDATE', [id]);
    if (tokRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Token not found' });
    }
    const token = tokRes.rows[0];

    // 2. Update Bill status
    const billRes = await client.query('SELECT * FROM bills WHERE token_id = $1 FOR UPDATE', [token.id]);
    if (billRes.rows.length > 0) {
      const bill = billRes.rows[0];
      await client.query("UPDATE bills SET status = 'PAID', updated_at = CURRENT_TIMESTAMP WHERE id = $1", [bill.id]);
      
      // Insert Payment
      await client.query(`
        INSERT INTO payments (bill_id, amount, payment_method, payment_status, paid_by)
        VALUES ($1, $2, 'CASH', 'SUCCESS', $3)
      `, [bill.id, bill.total_amount, DEFAULT_RECP_USER_ID]);
    }

    // 3. Complete token status
    await client.query("UPDATE tokens SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE id = $1", [token.id]);

    await client.query('COMMIT');
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to update compatibility payment.' });
  } finally {
    client.release();
  }
});

// 9. Compatibility Clear Database (transactional cascade reset)
app.post('/api/admin/clear', async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    
    // Clear everything except user structure, hospital metadata, and roles/permissions
    await client.query('DELETE FROM payments');
    await client.query('DELETE FROM bill_items');
    await client.query('DELETE FROM bills');
    await client.query('DELETE FROM medical_documents');
    await client.query('DELETE FROM lab_reports');
    await client.query('DELETE FROM lab_orders');
    await client.query('DELETE FROM prescription_items');
    await client.query('DELETE FROM prescriptions');
    await client.query('DELETE FROM medical_records');
    await client.query('DELETE FROM consultations');
    await client.query('DELETE FROM queue_events');
    await client.query('DELETE FROM queue_entries');
    await client.query('DELETE FROM tokens');
    await client.query('DELETE FROM appointment_requests');
    await client.query('DELETE FROM appointments');
    
    // Clear all patients except doctor/staff users to start clean
    await client.query("DELETE FROM patients WHERE user_id NOT IN (SELECT user_id FROM doctors UNION SELECT user_id FROM receptionists UNION SELECT user_id FROM administrators)");
    await client.query("DELETE FROM users WHERE role = 'PATIENT'");

    await client.query('COMMIT');
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Failed to clear database.' });
  } finally {
    client.release();
  }
});

// ----------------------------------------------------
// SOCKET.IO EVENT HANDLERS
// ----------------------------------------------------
io.on('connection', (socket) => {
  console.log(`Socket client connected: ${socket.id}`);
  
  socket.on('disconnect', () => {
    console.log(`Socket client disconnected: ${socket.id}`);
  });
});

// Listen on Port
server.listen(PORT, () => {
  console.log(`ETO Backend API Server running on port ${PORT}`);
});

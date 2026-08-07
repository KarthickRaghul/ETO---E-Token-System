const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
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

// Helper to broadcast queue updates to all clients
function broadcastQueueUpdate() {
  io.emit('queue_update', { timestamp: Date.now() });
  console.log('Broadcasted queue update event to clients');
}

// ----------------------------------------------------
// DATABASE INITIALIZATION & SEEDING
// ----------------------------------------------------
async function initDatabase() {
  try {
    // 1. Create Tables
    await db.query(`
      CREATE TABLE IF NOT EXISTS departments (
        id VARCHAR(50) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description TEXT,
        icon_name VARCHAR(50)
      );
    `);

    await db.query(`
      CREATE TABLE IF NOT EXISTS doctors (
        id VARCHAR(50) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        specialty VARCHAR(100) NOT NULL,
        department_id VARCHAR(50) REFERENCES departments(id),
        department_name VARCHAR(100) NOT NULL,
        rating NUMERIC(3, 2) DEFAULT 5.0,
        average_service_time_minutes INT DEFAULT 15,
        is_available BOOLEAN DEFAULT TRUE
      );
    `);

    await db.query(`
      CREATE TABLE IF NOT EXISTS tokens (
        id SERIAL PRIMARY KEY,
        token_number VARCHAR(20) NOT NULL,
        patient_name VARCHAR(100) NOT NULL,
        patient_phone VARCHAR(20) NOT NULL,
        doctor_id VARCHAR(50) REFERENCES doctors(id),
        doctor_name VARCHAR(100) NOT NULL,
        department_name VARCHAR(100) NOT NULL,
        status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        symptoms TEXT,
        diagnosis TEXT,
        prescription TEXT,
        bill_amount NUMERIC(10, 2) DEFAULT 0.0,
        payment_status VARCHAR(50) DEFAULT 'PENDING',
        queue_position INT DEFAULT 1,
        estimated_wait_minutes INT DEFAULT 0,
        created_at BIGINT NOT NULL
      );
    `);

    console.log('Database tables verified/created successfully.');

    // 2. Seed Departments
    const deptCheck = await db.query('SELECT COUNT(*) FROM departments');
    if (parseInt(deptCheck.rows[0].count, 10) === 0) {
      const depts = [
        ['1', 'Cardiology', 'Heart & Cardiovascular Care', 'favorite'],
        ['2', 'Pediatrics', 'Child Specialist & Growth Care', 'child_care'],
        ['3', 'Dermatology', 'Skin, Hair, Nail Treatment', 'face'],
        ['4', 'General Medicine', 'Primary Consult & Family Health', 'medical_services']
      ];
      for (const d of depts) {
        await db.query(
          'INSERT INTO departments (id, name, description, icon_name) VALUES ($1, $2, $3, $4)',
          d
        );
      }
      console.log('Seeded departments successfully.');
    }

    // 3. Seed Doctors
    const docCheck = await db.query('SELECT COUNT(*) FROM doctors');
    if (parseInt(docCheck.rows[0].count, 10) === 0) {
      const docs = [
        ['d1', 'Dr. Sarah Jenkins', 'Cardiologist', '1', 'Cardiology', 4.9, 15, true],
        ['d2', 'Dr. Robert Chen', 'Pediatrician', '2', 'Pediatrics', 4.8, 12, true],
        ['d3', 'Dr. Amanda Ross', 'Dermatologist', '3', 'Dermatology', 4.7, 20, true],
        ['d4', 'Dr. James Carter', 'Physician', '4', 'General Medicine', 4.6, 10, true]
      ];
      for (const d of docs) {
        await db.query(
          'INSERT INTO doctors (id, name, specialty, department_id, department_name, rating, average_service_time_minutes, is_available) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
          d
        );
      }
      console.log('Seeded doctors successfully.');
    }

  } catch (err) {
    console.error('Error initializing database:', err);
  }
}

// ----------------------------------------------------
// REST API ENDPOINTS
// ----------------------------------------------------

// Health Check
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', message: 'ETO Server running smoothly.' });
});

// Fetch Departments
app.get('/api/departments', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM departments');
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error fetching departments.' });
  }
});

// Fetch Doctors
app.get('/api/doctors', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM doctors');
    // Map database snake_case names to Compose model properties camelCase
    const mapped = rows.map(r => ({
      id: r.id,
      name: r.name,
      specialty: r.specialty,
      departmentId: r.department_id,
      departmentName: r.department_name,
      rating: parseFloat(r.rating),
      averageServiceTimeMinutes: r.average_service_time_minutes,
      isAvailable: r.is_available
    }));
    res.json(mapped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error fetching doctors.' });
  }
});

// Toggle Doctor Availability
app.patch('/api/doctors/:id/availability', async (req, res) => {
  try {
    const { id } = req.params;
    const { isAvailable } = req.body;
    await db.query(
      'UPDATE doctors SET is_available = $1 WHERE id = $2',
      [isAvailable, id]
    );
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error updating availability.' });
  }
});

// Fetch Tokens
app.get('/api/tokens', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM tokens ORDER BY id ASC');
    const mapped = rows.map(r => ({
      id: parseInt(r.id, 10),
      tokenNumber: r.token_number,
      patientName: r.patient_name,
      patientPhone: r.patient_phone,
      doctorId: r.doctor_id,
      doctorName: r.doctor_name,
      departmentName: r.department_name,
      status: r.status,
      symptoms: r.symptoms,
      diagnosis: r.diagnosis,
      prescription: r.prescription,
      billAmount: parseFloat(r.bill_amount),
      paymentStatus: r.payment_status,
      queuePosition: parseInt(r.queue_position, 10) || 1,
      estimatedWaitMinutes: r.estimated_wait_minutes,
      createdAt: parseInt(r.created_at, 10)
    }));
    res.json(mapped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error fetching tokens.' });
  }
});

// Request/Book a new token
app.post('/api/tokens/request', async (req, res) => {
  try {
    const { patientName, patientPhone, doctorId, symptoms, isWalkIn } = req.body;

    // Get doctor details
    const docRes = await db.query('SELECT * FROM doctors WHERE id = $1', [doctorId]);
    if (docRes.rows.length === 0) {
      return res.status(404).json({ error: 'Doctor not found.' });
    }
    const doctor = docRes.rows[0];

    // Find active tokens for this doctor (APPROVED or SERVING) to calculate estimated wait
    const activeRes = await db.query(
      "SELECT COUNT(*) FROM tokens WHERE doctor_id = $1 AND status IN ('APPROVED', 'SERVING')",
      [doctorId]
    );
    const queueCount = parseInt(activeRes.rows[0].count, 10);

    // Generate token number format
    const prefix = doctor.specialty.substring(0, 3).toUpperCase();
    const tokenNum = `#${prefix}-${101 + queueCount}`;

    const estWait = queueCount * doctor.average_service_time_minutes;
    const initialStatus = isWalkIn ? 'APPROVED' : 'PENDING';

    const insertRes = await db.query(
      `INSERT INTO tokens (
        token_number, patient_name, patient_phone, doctor_id, doctor_name, department_name, symptoms, status, queue_position, estimated_wait_minutes, created_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING id`,
      [
        tokenNum,
        patientName,
        patientPhone,
        doctorId,
        doctor.name,
        doctor.department_name,
        symptoms,
        initialStatus,
        queueCount + 1,
        estWait,
        Date.now()
      ]
    );

    const newId = insertRes.rows[0].id;
    res.json({ success: true, tokenId: newId, tokenNumber: tokenNum });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error booking token.' });
  }
});

// Update Token Status
app.patch('/api/tokens/:id/status', async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body; // e.g. APPROVED, SERVING, SKIPPED, REJECTED
    await db.query('UPDATE tokens SET status = $1 WHERE id = $2', [status, id]);
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error updating token status.' });
  }
});

// Complete Consultation
app.post('/api/tokens/:id/consultation', async (req, res) => {
  try {
    const { id } = req.params;
    const { diagnosis, prescription, billAmount } = req.body;
    await db.query(
      `UPDATE tokens 
       SET diagnosis = $1, prescription = $2, bill_amount = $3, status = 'COMPLETED', payment_status = 'PENDING'
       WHERE id = $4`,
      [diagnosis, prescription, billAmount, id]
    );
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error saving consultation.' });
  }
});

// Record Payment
app.post('/api/tokens/:id/payment', async (req, res) => {
  try {
    const { id } = req.params;
    await db.query(
      "UPDATE tokens SET payment_status = 'PAID' WHERE id = $1",
      [id]
    );
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error updating payment.' });
  }
});

// Reset Logs / Clear Database Tokens
app.post('/api/admin/clear', async (req, res) => {
  try {
    await db.query('DELETE FROM tokens');
    res.json({ success: true });
    broadcastQueueUpdate();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Server error clearing database.' });
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

// Start Server
initDatabase().then(() => {
  server.listen(PORT, () => {
    console.log(`ETO Backend API Server running on port ${PORT}`);
  });
});

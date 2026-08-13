const nodemailer = require('nodemailer');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// Ensure sent_mails directory exists locally for visual debugging
const SENT_MAILS_DIR = path.join(__dirname, 'sent_mails');
if (!fs.existsSync(SENT_MAILS_DIR)) {
  fs.mkdirSync(SENT_MAILS_DIR, { recursive: true });
}

let transporter;

async function getTransporter() {
  if (transporter) return transporter;

  const host = process.env.SMTP_HOST;
  const port = process.env.SMTP_PORT || 587;
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;

  if (host && user && pass) {
    console.log(`Configuring SMTP transporter for ${host}:${port}`);
    transporter = nodemailer.createTransport({
      host,
      port: parseInt(port),
      secure: parseInt(port) === 465,
      auth: { user, pass }
    });
  } else {
    console.log('No SMTP config found. Falling back to local logging and Ethereal Email.');
    // We create a mock/Ethereal transport
    try {
      const testAccount = await nodemailer.createTestAccount();
      transporter = nodemailer.createTransport({
        host: 'smtp.ethereal.email',
        port: 587,
        secure: false,
        auth: {
          user: testAccount.user,
          pass: testAccount.pass
        }
      });
      transporter._isEthereal = true;
    } catch (err) {
      console.error('Failed to create Ethereal test account, using stub transporter:', err);
      // Stub transporter that doesn't crash if network is offline
      transporter = {
        sendMail: async (mailOptions) => {
          return { messageId: 'stub-id', mock: true };
        }
      };
    }
  }
  return transporter;
}

// Helper to wrap body in standard ETO template
function wrapEmailHtml(title, preheader, contentHtml) {
  return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>${title}</title>
  <style>
    body {
      font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
      background-color: #F3F4F6;
      margin: 0;
      padding: 0;
      -webkit-font-smoothing: antialiased;
    }
    .wrapper {
      width: 100%;
      background-color: #F3F4F6;
      padding: 40px 0;
    }
    .container {
      max-width: 600px;
      margin: 0 auto;
      background-color: #FFFFFF;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }
    .header {
      background: linear-gradient(135deg, #2563EB, #1D4ED8);
      padding: 32px;
      text-align: center;
      color: #FFFFFF;
    }
    .header h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: -0.5px;
    }
    .header p {
      margin: 8px 0 0 0;
      font-size: 14px;
      opacity: 0.9;
    }
    .content {
      padding: 32px;
      color: #1F2937;
    }
    .content h2 {
      margin-top: 0;
      font-size: 20px;
      font-weight: 600;
      color: #111827;
    }
    .detail-card {
      background-color: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 20px;
      margin: 24px 0;
    }
    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #F1F5F9;
    }
    .detail-row:last-child {
      border-bottom: none;
    }
    .detail-label {
      font-weight: 500;
      color: #64748B;
    }
    .detail-value {
      font-weight: 600;
      color: #0F172A;
    }
    .status-badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
    }
    .status-pending { background-color: #FEF3C7; color: #D97706; }
    .status-approved { background-color: #D1FAE5; color: #059669; }
    .status-serving { background-color: #DBEAFE; color: #2563EB; }
    .status-skipped { background-color: #F3F4F6; color: #4B5563; }
    .status-completed { background-color: #D1FAE5; color: #059669; }
    .status-cancelled { background-color: #FEE2E2; color: #DC2626; }
    
    .footer {
      background-color: #F9FAFB;
      padding: 24px;
      text-align: center;
      font-size: 12px;
      color: #9CA3AF;
      border-top: 1px solid #E5E7EB;
    }
    .footer a {
      color: #2563EB;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="container">
      <div class="header">
        <h1>ETO Healthcare</h1>
        <p>${preheader}</p>
      </div>
      <div class="content">
        ${contentHtml}
      </div>
      <div class="footer">
        <p>This is an automated notification from ETO (E-Token System).</p>
        <p>&copy; 2026 ETO System. All rights reserved.</p>
      </div>
    </div>
  </div>
</body>
</html>
  `;
}

// Send standard email
async function sendMail(to, subject, preheader, contentHtml) {
  try {
    const transport = await getTransporter();
    const from = process.env.SMTP_FROM || 'no-reply@eto-healthcare.com';
    const html = wrapEmailHtml(subject, preheader, contentHtml);

    // Save to local file system for verification
    const safeSubject = subject.replace(/[^a-zA-Z0-9]/g, '_');
    const safeRecipient = to.replace(/[^a-zA-Z0-9@.]/g, '_');
    const filename = `${Date.now()}_${safeRecipient}_${safeSubject}.html`;
    const filepath = path.join(SENT_MAILS_DIR, filename);
    fs.writeFileSync(filepath, html, 'utf-8');
    console.log(`Saved email draft locally: [${filepath}]`);

    const info = await transport.sendMail({
      from: `"ETO Healthcare" <${from}>`,
      to,
      subject,
      html
    });

    console.log(`Email Alert Sent to ${to} (Subject: "${subject}")`);
    if (transport._isEthereal) {
      console.log(`Ethereal Email Inbox Link: ${nodemailer.getTestMessageUrl(info)}`);
    }
    return true;
  } catch (err) {
    console.error('Failed to send email alert:', err);
    return false;
  }
}

// Specific Alert templates
async function sendTokenCreatedAlert(email, patientName, token) {
  const isPending = token.status === 'PENDING';
  const statusClass = isPending ? 'status-pending' : 'status-approved';
  
  const content = `
    <h2>Appointment Scheduled</h2>
    <p>Dear ${patientName},</p>
    <p>Your appointment has been successfully scheduled. Here are your token and queue details:</p>
    
    <div class="detail-card">
      <div class="detail-row">
        <span class="detail-label">Token Number</span>
        <span class="detail-value" style="color: #2563EB; font-size: 18px;">${token.token_number}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Hospital</span>
        <span class="detail-value">${token.hospital_name || 'City Care Hospital'}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Doctor</span>
        <span class="detail-value">${token.doctor_name}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Department</span>
        <span class="detail-value">${token.department_name}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Queue Date</span>
        <span class="detail-value">${new Date(token.queue_date).toDateString()}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Status</span>
        <span class="detail-value"><span class="status-badge ${statusClass}">${token.status}</span></span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Est. Wait Time</span>
        <span class="detail-value">${token.estimated_wait_minutes} mins</span>
      </div>
    </div>
    
    ${isPending 
      ? '<p><strong>Note:</strong> Your online token request is currently PENDING receptionist approval. We will notify you once it is approved.</p>'
      : '<p>Please report to the hospital check-in desk before your token number is called.</p>'
    }
  `;
  
  return sendMail(
    email,
    `ETO Healthcare: Appointment Confirmed (${token.token_number})`,
    'Your token details and queue estimation',
    content
  );
}

async function sendTokenStatusAlert(email, patientName, token, oldStatus, newStatus) {
  let statusClass = 'status-pending';
  let instruction = '';
  
  switch(newStatus) {
    case 'APPROVED':
      statusClass = 'status-approved';
      instruction = 'Your appointment has been approved! Please check in at the reception desk upon arrival.';
      break;
    case 'CALLED':
    case 'SERVING':
      statusClass = 'status-serving';
      instruction = '<strong>Your turn has arrived!</strong> Please proceed to the doctor\'s consultation room immediately.';
      break;
    case 'SKIPPED':
      statusClass = 'status-skipped';
      instruction = 'You were not present when called, so your token has been skipped. Please speak with the receptionist to re-enter the queue.';
      break;
    case 'CANCELLED':
      statusClass = 'status-cancelled';
      instruction = 'Your appointment has been cancelled.';
      break;
  }
  
  const content = `
    <h2>Token Status Updated</h2>
    <p>Dear ${patientName},</p>
    <p>The status of your E-Token <strong>${token.token_number}</strong> has changed from <strong>${oldStatus}</strong> to <strong>${newStatus}</strong>.</p>
    
    <div class="detail-card">
      <div class="detail-row">
        <span class="detail-label">Token Number</span>
        <span class="detail-value">${token.token_number}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Doctor</span>
        <span class="detail-value">${token.doctor_name}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">New Status</span>
        <span class="detail-value"><span class="status-badge ${statusClass}">${newStatus}</span></span>
      </div>
    </div>
    
    <p>${instruction}</p>
  `;
  
  return sendMail(
    email,
    `ETO Healthcare: Token Status Updated to ${newStatus} (${token.token_number})`,
    'Updates regarding your live queue status',
    content
  );
}

async function sendConsultationAlert(email, patientName, token, consultation) {
  const content = `
    <h2>Consultation Summary</h2>
    <p>Dear ${patientName},</p>
    <p>Your consultation with <strong>${token.doctor_name}</strong> has been completed. Here is the summary of your visit:</p>
    
    <div class="detail-card">
      <div class="detail-row">
        <span class="detail-label">Diagnosis</span>
        <span class="detail-value">${consultation.diagnosis || 'N/A'}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Prescription</span>
        <span class="detail-value" style="color: #2563EB; font-weight: 600;">${consultation.prescription || 'N/A'}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Consultation Fee</span>
        <span class="detail-value">₹${consultation.billAmount}</span>
      </div>
    </div>
    
    <p>Please proceed to the billing/payment counter or check your patient dashboard to pay online.</p>
  `;
  
  return sendMail(
    email,
    `ETO Healthcare: Consultation Completed (${token.token_number})`,
    'Consultation diagnosis and prescription summary',
    content
  );
}

async function sendPaymentAlert(email, patientName, token, payment) {
  const content = `
    <h2>Payment Receipt</h2>
    <p>Dear ${patientName},</p>
    <p>Thank you for your payment. Your receipt is confirmed below:</p>
    
    <div class="detail-card">
      <div class="detail-row">
        <span class="detail-label">Token Number</span>
        <span class="detail-value">${token.token_number}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Doctor</span>
        <span class="detail-value">${token.doctor_name}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Amount Paid</span>
        <span class="detail-value" style="color: #059669; font-weight: 700;">₹${payment.amount}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Payment Method</span>
        <span class="detail-value">${payment.method || 'CASH'}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">Transaction ID</span>
        <span class="detail-value">${payment.transactionId || 'N/A'}</span>
      </div>
    </div>
  `;
  
  return sendMail(
    email,
    `ETO Healthcare: Payment Confirmed (${token.token_number})`,
    'Receipt of your consultation fee payment',
    content
  );
}

module.exports = {
  sendTokenCreatedAlert,
  sendTokenStatusAlert,
  sendConsultationAlert,
  sendPaymentAlert
};

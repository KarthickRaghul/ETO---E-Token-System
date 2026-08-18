const { Pool } = require('pg');
require('dotenv').config();

const isProduction = process.env.NODE_ENV === 'production';

// If DATABASE_URL is provided, prevent standard pg environment variables from overriding it
if (process.env.DATABASE_URL) {
  delete process.env.PGHOST;
  delete process.env.PGUSER;
  delete process.env.PGPASSWORD;
  delete process.env.PGDATABASE;
  delete process.env.PGPORT;
}

// Use DATABASE_URL if available (typical for Railway/Render), otherwise fallback to individual credentials
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  host: process.env.DATABASE_URL ? undefined : (process.env.PGHOST || 'localhost'),
  user: process.env.DATABASE_URL ? undefined : (process.env.PGUSER || 'postgres'),
  password: process.env.DATABASE_URL ? undefined : (process.env.PGPASSWORD || 'postgres'),
  database: process.env.DATABASE_URL ? undefined : (process.env.PGDATABASE || 'eto_db'),
  port: process.env.DATABASE_URL ? undefined : (process.env.PGPORT || 5432),
  ssl: isProduction ? { rejectUnauthorized: false } : false
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};

const db = require('./db');

async function prepare() {
  const client = await db.pool.connect();
  try {
    console.log('Renaming existing prototype tables to old_* for migration...');
    
    // Check if tables exist before renaming to avoid errors or duplicate renames
    const checkTable = async (tableName) => {
      const res = await client.query(`
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' 
          AND table_name = $1
        );
      `, [tableName]);
      return res.rows[0].exists;
    };

    const hasDeps = await checkTable('departments');
    const hasDocs = await checkTable('doctors');
    const hasTokens = await checkTable('tokens');

    await client.query('BEGIN');

    // First drop any foreign key constraints from the old tables to prevent locking/reference issues
    if (hasTokens) {
      try {
        await client.query('ALTER TABLE tokens DROP CONSTRAINT IF EXISTS tokens_doctor_id_fkey');
      } catch (e) {
        console.log('FKey tokens_doctor_id_fkey drop skipped or not found');
      }
    }
    if (hasDocs) {
      try {
        await client.query('ALTER TABLE doctors DROP CONSTRAINT IF EXISTS doctors_department_id_fkey');
      } catch (e) {
        console.log('FKey doctors_department_id_fkey drop skipped or not found');
      }
    }

    if (hasDeps && !(await checkTable('old_departments'))) {
      await client.query('ALTER TABLE departments RENAME TO old_departments;');
      console.log('Renamed departments to old_departments');
    }
    if (hasDocs && !(await checkTable('old_doctors'))) {
      await client.query('ALTER TABLE doctors RENAME TO old_doctors;');
      console.log('Renamed doctors to old_doctors');
    }
    if (hasTokens && !(await checkTable('old_tokens'))) {
      await client.query('ALTER TABLE tokens RENAME TO old_tokens;');
      console.log('Renamed tokens to old_tokens');
    }

    await client.query('COMMIT');
    console.log('Pre-migration preparation complete.');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Pre-migration preparation failed:', err);
    process.exit(1);
  } finally {
    client.release();
  }
}

prepare().then(() => process.exit(0));

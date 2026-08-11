CREATE TABLE IF NOT EXISTS queue_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id UUID NOT NULL REFERENCES tokens(id) ON DELETE CASCADE UNIQUE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    queue_date DATE NOT NULL,
    position INT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'SKIPPED', 'COMPLETED')),
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'EMERGENCY')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    skipped_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_queue_entries_doctor_date_status ON queue_entries(doctor_id, queue_date, status);
CREATE INDEX IF NOT EXISTS idx_queue_entries_ordering ON queue_entries(doctor_id, queue_date, priority, position, joined_at);

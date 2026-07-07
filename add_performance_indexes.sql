CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_court_id ON reservations(court_id);
CREATE INDEX IF NOT EXISTS idx_courts_active ON courts(active);

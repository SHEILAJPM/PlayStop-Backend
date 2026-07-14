-- Cubre la consulta de ReminderScheduler (corre cada 5 min filtrando por
-- date + slot_hour + status), que hoy hace un escaneo completo de la tabla.
CREATE INDEX IF NOT EXISTS idx_reservations_date_slot_status
    ON reservations (date, slot_hour, status);

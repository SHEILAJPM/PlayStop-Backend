-- Evita que ReminderScheduler reenvie el mismo recordatorio (email/WhatsApp)
-- varias veces dentro de la misma ventana de 1 hora.
ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT false;

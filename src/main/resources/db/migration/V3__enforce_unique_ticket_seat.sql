CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_screening_seat
    ON ticket (screening_id, seat_id);

-- The end of a trip keeps its own UTC offset (it may differ from the start's, e.g. when crossing a time zone border).
-- Existing rows get the start's offset, which is what they were displayed with before.
ALTER TABLE trips ADD COLUMN ended_utc_offset_seconds INT;
UPDATE trips SET ended_utc_offset_seconds = utc_offset_seconds;
ALTER TABLE trips ALTER COLUMN ended_utc_offset_seconds SET NOT NULL;

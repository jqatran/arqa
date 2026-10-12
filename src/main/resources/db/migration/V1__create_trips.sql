CREATE TABLE trips (
    trip_identifier    VARCHAR(100)             NOT NULL,
    started_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    utc_offset_seconds INT                      NOT NULL,
    local_trip_date    DATE                     NOT NULL,
    amount             BIGINT                   NOT NULL,
    payment_method     VARCHAR(10)              NOT NULL,
    commission         BIGINT                   NOT NULL,
    CONSTRAINT pk_trips PRIMARY KEY (trip_identifier),
    CONSTRAINT chk_trips_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_trips_commission_within_amount CHECK (commission >= 0 AND commission <= amount),
    CONSTRAINT chk_trips_end_after_start CHECK (ended_at > started_at),
    CONSTRAINT chk_trips_payment_method CHECK (payment_method IN ('CASH', 'CARD'))
);

CREATE INDEX idx_trips_local_trip_date ON trips (local_trip_date);

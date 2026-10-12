package kz.jqd.arqa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import kz.jqd.arqa.model.PaymentMethod;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * One completed trip. Money is stored as whole currency units (tenge).
 * The driver's local day is fixed at creation time from the start time and its own UTC offset,
 * so a trip belongs to the same day regardless of the server's time zone.
 * Both times are stored as instants together with their own UTC offsets, so they are shown as they were sent.
 */
@Entity
@Table(name = "trips")
public class Trip implements Persistable<String> {

    @Id
    @Column(name = "trip_identifier", nullable = false, updatable = false)
    private String tripIdentifier;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "utc_offset_seconds", nullable = false)
    private int utcOffsetSeconds;

    @Column(name = "ended_utc_offset_seconds", nullable = false)
    private int endedUtcOffsetSeconds;

    @Column(name = "local_trip_date", nullable = false)
    private LocalDate localTripDate;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private long commission;

    /**
     * The identifier is assigned by the client, so Spring Data cannot tell a new trip from a stored one by it
     * and would call merge(), which first SELECTs and silently turns a lost race into an UPDATE.
     * Declaring new trips as new makes every save an INSERT, so the primary key decides who wins.
     */
    @Transient
    private boolean isNewlyConstructed = true;

    protected Trip() {
    }

    public Trip(String tripIdentifier, OffsetDateTime startedAt, OffsetDateTime endedAt,
                long amount, PaymentMethod paymentMethod, long commission) {
        this.tripIdentifier = tripIdentifier;
        this.startedAt = startedAt.toInstant();
        this.endedAt = endedAt.toInstant();
        this.utcOffsetSeconds = startedAt.getOffset().getTotalSeconds();
        this.endedUtcOffsetSeconds = endedAt.getOffset().getTotalSeconds();
        this.localTripDate = startedAt.toLocalDate();
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.commission = commission;
    }

    public String getTripIdentifier() {
        return tripIdentifier;
    }

    @Override
    public String getId() {
        return tripIdentifier;
    }

    @Override
    public boolean isNew() {
        return isNewlyConstructed;
    }

    @PostLoad
    @PostPersist
    void markAsStored() {
        isNewlyConstructed = false;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt.atOffset(ZoneOffset.ofTotalSeconds(utcOffsetSeconds));
    }

    public OffsetDateTime getEndedAt() {
        return endedAt.atOffset(ZoneOffset.ofTotalSeconds(endedUtcOffsetSeconds));
    }

    public LocalDate getLocalTripDate() {
        return localTripDate;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public long getCommission() {
        return commission;
    }

    /**
     * True when every business field matches; the identifier is expected to be equal already.
     * The end is compared as an instant only: writing the same moment with another offset is the same trip.
     */
    public boolean hasSameDataAs(Trip other) {
        return startedAt.equals(other.startedAt)
                && endedAt.equals(other.endedAt)
                && utcOffsetSeconds == other.utcOffsetSeconds
                && amount == other.amount
                && paymentMethod == other.paymentMethod
                && commission == other.commission;
    }
}

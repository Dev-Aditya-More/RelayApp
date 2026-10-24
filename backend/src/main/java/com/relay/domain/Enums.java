package com.relay.domain;

public final class Enums {
    private Enums() {}

    public enum Role { CITIZEN, CONTRACTOR, ADMIN }

    public enum Category { POTHOLE, GARBAGE, WATER_LEAK, STREETLIGHT, DRAINAGE, OTHER }

    public enum Severity { LOW, MEDIUM, HIGH }

    public enum Status { OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, CLOSED }

    public enum PaymentType { FAST_TRACK, FINE, CONTRACTOR_PAYOUT }

    /** PENDING = owed but no gateway order yet, CREATED = gateway order open, PAID / FAILED = terminal. */
    public enum PaymentStatus { PENDING, CREATED, PAID, FAILED }
}

package com.relay.config;

import com.relay.domain.*;
import com.relay.domain.Enums.*;
import com.relay.repo.*;
import com.relay.service.ClassifierService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds SLA policies always, and demo users + work orders on an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final SlaPolicyRepository policies;
    private final WorkOrderRepository orders;
    private final WorkOrderEventRepository events;
    private final PaymentRepository payments;
    private final PasswordEncoder encoder;
    private final ClassifierService classifier;
    private final boolean demo;
    private final String adminEmail;
    private final String adminPassword;
    private final String demoPassword;

    public DataSeeder(UserRepository users, SlaPolicyRepository policies, WorkOrderRepository orders,
                      WorkOrderEventRepository events, PaymentRepository payments, PasswordEncoder encoder,
                      ClassifierService classifier, @Value("${relay.seed-demo-data}") boolean demo,
                      @Value("${relay.admin.email}") String adminEmail,
                      @Value("${relay.admin.password}") String adminPassword,
                      @Value("${relay.demo-password}") String demoPassword) {
        this.users = users;
        this.policies = policies;
        this.orders = orders;
        this.events = events;
        this.payments = payments;
        this.encoder = encoder;
        this.classifier = classifier;
        this.demo = demo;
        this.adminEmail = adminEmail.trim();
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (policies.count() == 0) {
            policies.save(new SlaPolicy(Category.POTHOLE, "Roads & Infrastructure", 48, 2000, 6500, 199));
            policies.save(new SlaPolicy(Category.GARBAGE, "Solid Waste Management", 24, 1000, 2500, 99));
            policies.save(new SlaPolicy(Category.WATER_LEAK, "Water Supply", 12, 3000, 4500, 249));
            policies.save(new SlaPolicy(Category.STREETLIGHT, "Electrical", 72, 800, 1800, 99));
            policies.save(new SlaPolicy(Category.DRAINAGE, "Drainage & Sewerage", 36, 2500, 5500, 199));
            policies.save(new SlaPolicy(Category.OTHER, "General Services", 72, 500, 2000, 99));
        }
        bootstrapAdmin();
        if (!demo) return;
        if (demoPassword.isBlank()) {
            log.warn("SEED_DEMO_DATA is on but DEMO_PASSWORD is not set; skipping demo data");
            return;
        }
        if (users.findByEmailIgnoreCase("ravi@relay.app").isPresent()) return;

        User ravi = user("Ravi Constructions", "ravi@relay.app", demoPassword, Role.CONTRACTOR, true);
        User meera = user("Meera Civil Works", "meera@relay.app", demoPassword, Role.CONTRACTOR, true);
        User asha = user("Asha Kulkarni", "asha@relay.app", demoPassword, Role.CITIZEN, true);
        User dev = user("Dev Patil", "dev@relay.app", demoPassword, Role.CITIZEN, true);

        //   title, description, category, severity, area, lat, lng, status, ageHours, contractor, hoursToFinish, rating, citizen
        seed("Deep pothole near Karve Road signal", "Huge pothole, two-wheelers are skidding here every day.", Category.POTHOLE, Severity.HIGH, "Kothrud", 18.5074, 73.8077, Status.OPEN, 6, null, 0, 0, asha);
        seed("Garbage pile outside school gate", "Trash has not been collected for four days and it stinks.", Category.GARBAGE, Severity.MEDIUM, "Baner", 18.5590, 73.7868, Status.OPEN, 26, null, 0, 0, dev);
        seed("Water pipe burst on main lane", "Burst pipe, water flooding the street since morning.", Category.WATER_LEAK, Severity.HIGH, "Kothrud", 18.5089, 73.8101, Status.ASSIGNED, 5, ravi, 0, 0, asha);
        seed("Streetlight not working", "Three streetlights are dark on the stretch near the park.", Category.STREETLIGHT, Severity.MEDIUM, "Aundh", 18.5580, 73.8075, Status.IN_PROGRESS, 30, meera, 0, 0, dev);
        seed("Clogged drain after rain", "Drain overflow every time it rains, road waterlogging.", Category.DRAINAGE, Severity.MEDIUM, "Hadapsar", 18.4967, 73.9417, Status.IN_PROGRESS, 20, ravi, 0, 0, asha);
        seed("Pothole cluster on service road", "Several small potholes near the bus stop.", Category.POTHOLE, Severity.LOW, "Baner", 18.5642, 73.7769, Status.ASSIGNED, 70, meera, 0, 0, dev);
        seed("Overflowing dustbin", "Public dustbin overflowing onto the footpath.", Category.GARBAGE, Severity.LOW, "Kothrud", 18.5042, 73.8123, Status.COMPLETED, 40, ravi, 10, 0, asha);
        seed("Manhole cover missing", "Dangerous open manhole next to the market.", Category.DRAINAGE, Severity.HIGH, "Hadapsar", 18.5009, 73.9380, Status.COMPLETED, 60, meera, 30, 0, dev);
        seed("Leaking tap supply line", "Water leak from supply pipe near the temple.", Category.WATER_LEAK, Severity.MEDIUM, "Aundh", 18.5611, 73.8120, Status.CLOSED, 90, ravi, 8, 5, asha);
        seed("Pothole on flyover approach", "Crater on the approach, quick fix, thanks.", Category.POTHOLE, Severity.HIGH, "Kothrud", 18.5101, 73.8052, Status.CLOSED, 120, meera, 40, 4, dev);
        seed("Streetlight pole sparking", "Live wire sparking, very dangerous, still not fixed again.", Category.STREETLIGHT, Severity.HIGH, "Baner", 18.5566, 73.7898, Status.CLOSED, 150, ravi, 80, 2, asha);
        seed("Garbage dump behind market", "Waste dump growing behind the vegetable market.", Category.GARBAGE, Severity.MEDIUM, "Hadapsar", 18.4990, 73.9450, Status.CLOSED, 100, meera, 14, 4, dev);
        // Overdue but not yet flagged: the SLA scheduler picks this one up shortly after start.
        seed("Sewage smell near apartments", "Sewer overflow near the society gate.", Category.DRAINAGE, Severity.MEDIUM, "Aundh", 18.5560, 73.8090, Status.OPEN, 40, null, 0, 0, asha);

        log.info("Seeded demo data. Demo logins are listed for admins under People.");
    }

    /** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD. Never promotes an existing account. */
    private void bootstrapAdmin() {
        if (adminEmail.isBlank()) return;
        if (users.findByEmailIgnoreCase(adminEmail).isPresent()) return;
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD must be at least 8 characters; admin {} not created", adminEmail);
            return;
        }
        user("Relay Admin", adminEmail.toLowerCase(), adminPassword, Role.ADMIN, false);
        log.info("Created admin account {}", adminEmail);
    }

    private User user(String name, String email, String password, Role role, boolean isDemo) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setApproved(true);
        u.setEmailVerified(true);
        u.setDemo(isDemo);
        return users.save(u);
    }

    private void seed(String title, String desc, Category cat, Severity sev, String area, double lat, double lng,
                      Status status, long ageHours, User contractor, long hoursToFinish, int rating, User citizen) {
        SlaPolicy policy = policies.findByCategory(cat).orElseThrow();
        Instant now = Instant.now();
        WorkOrder w = new WorkOrder();
        w.setTitle(title);
        w.setDescription(desc);
        w.setCategory(cat);
        w.setSeverity(sev);
        w.setStatus(status);
        w.setArea(area);
        w.setAddress(area + ", Pune");
        w.setLatitude(lat);
        w.setLongitude(lng);
        w.setCitizen(citizen);
        w.setContractor(contractor);
        w.setCreatedAt(now.minus(Duration.ofHours(ageHours)));
        w.setSlaDeadline(w.getCreatedAt().plus(Duration.ofHours(policy.getHours())));
        if (contractor != null) w.setAssignedAt(w.getCreatedAt().plus(Duration.ofHours(1)));
        if (status == Status.IN_PROGRESS) w.setStartedAt(now.minus(Duration.ofHours(2)));
        boolean done = status == Status.COMPLETED || status == Status.CLOSED;
        if (done) {
            w.setCompletedAt(w.getCreatedAt().plus(Duration.ofHours(hoursToFinish)));
            w.setSlaBreached(w.getCompletedAt().isAfter(w.getSlaDeadline()));
        } else if (status != Status.OPEN || w.getSlaDeadline().isBefore(now.minus(Duration.ofHours(1)))) {
            // active and already late by a margin: flag it (the OPEN overdue demo row is left for the scheduler)
            w.setSlaBreached(w.getSlaDeadline().isBefore(now) && status != Status.OPEN);
        }
        if (rating > 0) {
            String fb = desc.contains("thanks") ? "Quick fix, thanks" : desc.contains("again") ? "Still slow and had to follow up again" : "Good work";
            w.setRating(rating);
            w.setFeedback(fb);
            w.setSentiment(classifier.sentiment(fb, rating));
        }
        orders.save(w);
        w.setCode(String.format("RLY-%06d", w.getId()));
        events.save(ev(w, citizen.getName(), "REPORTED", "Issue reported and work order " + w.getCode() + " created", w.getCreatedAt()));
        if (contractor != null) events.save(ev(w, "Relay Admin", "ASSIGNED", "Handed off to " + contractor.getName(), w.getAssignedAt()));
        if (w.getStartedAt() != null || done) events.save(ev(w, contractor.getName(), "IN_PROGRESS", "Crew on site", w.getCreatedAt().plus(Duration.ofHours(2))));
        if (done) events.save(ev(w, contractor.getName(), "COMPLETED", "Work completed", w.getCompletedAt()));
        List<User> admins = users.findByRole(Role.ADMIN);
        if (status == Status.CLOSED && contractor != null && rating >= 4 && !admins.isEmpty()) {
            Payment p = new Payment();
            p.setType(PaymentType.CONTRACTOR_PAYOUT);
            p.setStatus(PaymentStatus.PAID);
            p.setAmount(policy.getContractorRate());
            p.setPayer(admins.get(0));
            p.setPayee(contractor);
            p.setWorkOrder(w);
            p.setPaidAt(w.getCompletedAt().plus(Duration.ofHours(6)));
            p.setRazorpayPaymentId("payout_sim_seed" + w.getId());
            p.setNote("Simulated payout");
            payments.save(p);
            p.setInvoiceNo("INV-" + java.time.Year.now().getValue() + "-" + String.format("%05d", p.getId()));
            w.setContractorPaid(true);
        }
    }

    private WorkOrderEvent ev(WorkOrder w, String actor, String kind, String msg, Instant at) {
        WorkOrderEvent e = new WorkOrderEvent(w, actor, kind, msg, null);
        e.setCreatedAt(at);
        return e;
    }
}

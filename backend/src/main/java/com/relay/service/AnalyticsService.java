package com.relay.service;

import com.relay.domain.*;
import com.relay.domain.Enums.*;
import com.relay.repo.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private final WorkOrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final Mapper mapper;

    public AnalyticsService(WorkOrderRepository orders, PaymentRepository payments, UserRepository users, Mapper mapper) {
        this.orders = orders;
        this.payments = payments;
        this.users = users;
        this.mapper = mapper;
    }

    public Map<String, Object> summary() {
        List<WorkOrder> all = orders.findAll();
        List<WorkOrder> resolved = all.stream().filter(w -> w.getCompletedAt() != null).toList();
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("total", all.size());
        out.put("open", count(all, Status.OPEN));
        out.put("active", count(all, Status.ASSIGNED) + count(all, Status.IN_PROGRESS));
        out.put("resolved", resolved.size());
        out.put("breached", all.stream().filter(WorkOrder::isSlaBreached).count());
        out.put("slaCompliance", resolved.isEmpty() ? 100
                : Math.round(100.0 * resolved.stream().filter(w -> !w.isSlaBreached()).count() / resolved.size()));
        out.put("avgResolutionHours", round1(resolved.stream()
                .mapToDouble(w -> Duration.between(w.getCreatedAt(), w.getCompletedAt()).toMinutes() / 60.0)
                .average().orElse(0)));

        out.put("byStatus", Arrays.stream(Status.values()).collect(Collectors.toMap(
                Enum::name, s -> count(all, s), (a, b) -> a, LinkedHashMap::new)));
        out.put("byCategory", all.stream().collect(Collectors.groupingBy(
                w -> w.getCategory().name(), TreeMap::new, Collectors.counting())));

        Map<String, Long> areas = all.stream().collect(Collectors.groupingBy(WorkOrder::getArea, Collectors.counting()));
        out.put("hotspots", areas.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(6)
                .map(e -> Map.of("area", e.getKey(), "count", e.getValue())).toList());
        out.put("points", all.stream().filter(w -> w.getLatitude() != null && w.getLongitude() != null)
                .map(w -> Map.of("lat", w.getLatitude(), "lng", w.getLongitude(),
                        "severity", w.getSeverity().name(), "status", w.getStatus().name())).toList());

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long n = all.stream().filter(w -> w.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(d)).count();
            trend.add(Map.of("day", d.getDayOfWeek().name().substring(0, 3), "count", n));
        }
        out.put("trend", trend);

        out.put("atRisk", all.stream().map(mapper::view)
                .filter(v -> v.delayRisk() != null && v.delayRisk() >= 60)
                .sorted(Comparator.comparing(v -> -v.delayRisk())).limit(5).toList());

        List<Payment> paid = payments.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID).toList();
        out.put("revenue", Map.of(
                "fastTrack", sum(paid, PaymentType.FAST_TRACK),
                "fines", sum(paid, PaymentType.FINE),
                "payouts", sum(paid, PaymentType.CONTRACTOR_PAYOUT)));

        out.put("avgRating", round1(all.stream().filter(w -> w.getRating() != null)
                .mapToInt(WorkOrder::getRating).average().orElse(0)));
        out.put("sentiment", all.stream().filter(w -> w.getSentiment() != null)
                .collect(Collectors.groupingBy(WorkOrder::getSentiment, TreeMap::new, Collectors.counting())));

        out.put("contractors", users.findByRole(Role.CONTRACTOR).stream().map(c -> {
            List<WorkOrder> mine = all.stream().filter(w -> w.getContractor() != null && w.getContractor().getId().equals(c.getId())).toList();
            List<WorkOrder> done = mine.stream().filter(w -> w.getCompletedAt() != null).toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("assigned", mine.size());
            m.put("active", mine.stream().filter(w -> w.getStatus() == Status.ASSIGNED || w.getStatus() == Status.IN_PROGRESS).count());
            m.put("completed", done.size());
            m.put("onTime", done.isEmpty() ? 100 : Math.round(100.0 * done.stream().filter(w -> !w.isSlaBreached()).count() / done.size()));
            m.put("avgRating", round1(mine.stream().filter(w -> w.getRating() != null).mapToInt(WorkOrder::getRating).average().orElse(0)));
            return m;
        }).toList());
        return out;
    }

    private long count(List<WorkOrder> all, Status s) {
        return all.stream().filter(w -> w.getStatus() == s).count();
    }

    private BigDecimal sum(List<Payment> paid, PaymentType t) {
        return paid.stream().filter(p -> p.getType() == t).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

package com.relay.domain;

import com.relay.domain.Enums.Category;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class SlaPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(unique = true)
    private Category category;

    private String department;
    private int hours;
    private BigDecimal fineAmount;
    private BigDecimal contractorRate;
    private BigDecimal fastTrackFee;

    public SlaPolicy(Category category, String department, int hours, long fine, long rate, long fastTrack) {
        this.category = category;
        this.department = department;
        this.hours = hours;
        this.fineAmount = BigDecimal.valueOf(fine);
        this.contractorRate = BigDecimal.valueOf(rate);
        this.fastTrackFee = BigDecimal.valueOf(fastTrack);
    }
}

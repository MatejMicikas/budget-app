package cz.cvut.fit.budget_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "funding_sources")
@Data
@NoArgsConstructor
public class FundingSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingType type;

    // Volitelný limit — null = bez limitu
    private BigDecimal allocatedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    public enum FundingType {
        PUBLIC_GRANT, SPONSORSHIP, MEMBERSHIP, OWN_ACTIVITY
    }
}

package cz.cvut.fit.budget_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_items")
@Data
@NoArgsConstructor
public class BudgetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false)
    private BigDecimal plannedAmount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    // Volitelné — ne každá položka má zdroj
    @ManyToOne
    @JoinColumn(name = "funding_source_id")
    private FundingSource fundingSource;

    public enum ItemType {
        INCOME, EXPENSE
    }
}

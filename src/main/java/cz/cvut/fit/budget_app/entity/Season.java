package cz.cvut.fit.budget_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "seasons")
@Data
@NoArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeasonStatus status = SeasonStatus.OPEN;

    @Column(nullable = false)
    private boolean memberSummaryVisible = true;

    public enum SeasonStatus {
        OPEN, CLOSED
    }
}

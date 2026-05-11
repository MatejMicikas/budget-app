package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.Season;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SeasonResponse {
    private Long id;
    private String name;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Season.SeasonStatus status;
    private boolean memberSummaryVisible;

    public static SeasonResponse from(Season season) {
        SeasonResponse r = new SeasonResponse();
        r.id = season.getId();
        r.name = season.getName();
        r.dateFrom = season.getDateFrom();
        r.dateTo = season.getDateTo();
        r.status = season.getStatus();
        r.memberSummaryVisible = season.isMemberSummaryVisible();
        return r;
    }
}

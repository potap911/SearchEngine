package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsRs {
    private Boolean result;
    private StatisticsData statistics;
    private String error;
}

package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsRs {
    private Boolean result;
    private StatisticsData statistics;
    private String error;
}

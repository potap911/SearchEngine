package searchengine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BaseRs {
    private boolean result;
    private String error;
}

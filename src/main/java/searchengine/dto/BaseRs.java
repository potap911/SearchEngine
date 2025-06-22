package searchengine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BaseRs {
    private boolean result;
    private String error;

    public static BaseRs getBaseRs(boolean result, String error) {
        return BaseRs.builder()
                .result(result)
                .error(error)
                .build();
    }
}

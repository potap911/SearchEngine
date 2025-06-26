package searchengine.dto;

import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@Builder
public class BaseRs {
    private static final Logger logger = LoggerFactory.getLogger(BaseRs.class);
    private boolean result;
    private String error;

    public static BaseRs getBaseRs(boolean result, String error) {
        BaseRs baseRs = BaseRs.builder()
                .result(result)
                .error(error)
                .build();
        logger.info("[RESPONSE] {}", baseRs);
        return baseRs;
    }
}

package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchRs {
    private boolean result;
    private int count;
    private List<SearchData> data;
    private String error;

    public static SearchRs getBaseRs(boolean result, String error) {
        return SearchRs.builder()
                .result(result)
                .error(error)
                .build();
    }
}

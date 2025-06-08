package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchRs {
    private boolean result;
    private List<SearchData> data;
    private String error;
}

package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchData {
    private String site;
    private String name;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;
}

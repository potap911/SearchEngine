package searchengine.services.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import searchengine.entity.SearchIndex;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SearchIndexMap {
    private String lemma;
    private List<SearchIndex> searchIndexList;
}

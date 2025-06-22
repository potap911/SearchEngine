package searchengine.services.search;

import java.util.Comparator;

public class SearchIndexMapByFrequencyComparator implements Comparator<SearchIndexMap> {
    
    @Override
    public int compare(SearchIndexMap searchIndexMap1, SearchIndexMap searchIndexMap2) {
        return Integer.compare(searchIndexMap1.getSearchIndexList().size(), searchIndexMap2.getSearchIndexList().size());
    }
}

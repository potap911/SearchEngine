package searchengine.dto.search;

import java.util.Comparator;

public class SearchDataByMaxRelevenceComparator implements Comparator<SearchData> {
    
    @Override
    public int compare(SearchData searchData1, SearchData searchData2) {
        return searchData2.getRelevance().compareTo(searchData1.getRelevance());
    }
}

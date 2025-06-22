package searchengine.services.search;

import searchengine.dto.search.SearchRs;

public interface SearchService {
    SearchRs search(String query, String siteUrl, Integer offset, Integer limit);
}

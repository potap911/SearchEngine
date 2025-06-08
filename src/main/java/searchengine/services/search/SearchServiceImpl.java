package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchRs;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @Override
    public SearchRs search() {
        SearchRs response = new SearchRs();
        response.setResult(true);
        response.setError("searchError");
        return response;
    }
}

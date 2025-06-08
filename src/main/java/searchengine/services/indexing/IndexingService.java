package searchengine.services.indexing;

import searchengine.dto.BaseRs;

public interface IndexingService {
    BaseRs indexPage(String url);
    BaseRs startIndexing();
    BaseRs stopIndexing();
}

package searchengine.services.indexing;

import searchengine.dto.BaseRs;
import searchengine.dto.indexing.IndexPageRq;

public interface IndexingService {
    BaseRs indexPage(IndexPageRq rq);
    BaseRs startIndexing();
    BaseRs stopIndexing();
    BaseRs addAlias(String word, String alias);
}

package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchDataByMaxRelevenceComparator;
import searchengine.dto.search.SearchRs;
import searchengine.entity.SearchIndex;
import searchengine.entity.Site;
import searchengine.repositorys.SearchIndexDao;
import searchengine.repositorys.SiteDao;
import searchengine.utils.Lemanizer;

import java.util.*;

import static searchengine.config.SearchConfig.LIMIT;
import static searchengine.config.SearchConfig.OFFSET;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final SiteListConfig siteListConfig;
    private final SearchIndexDao searchIndexDao;
    private final SiteDao siteDao;

    @Autowired
    private final Lemanizer lemanizer;

    @Override
    public SearchRs search(String query, String siteUrl, Integer offset, Integer limit) {
        Long siteId = null;
        if (siteUrl != null) {
            SiteConfig siteConfig = siteListConfig.findSiteNameConfig(siteUrl);
            if (siteConfig == null)
                return SearchRs.getBaseRs(false, "The transferred site is located outside the sites specified in the configuration file");
            Site site = siteDao.findByName(siteConfig.getName());
            if (site == null)
                return SearchRs.getBaseRs(false, "The transferred site has not been indexed yet");
            siteId = site.getId();
        }

        offset = offset == null ? OFFSET : offset;
        limit = limit == null ? LIMIT : limit;
        List<SearchData> searchDataList = getSearchDataList(query, siteId);
        SearchRs rs = SearchRs.builder()
                .result(true)
                .count(searchDataList.size())
                .data(getSliceOfSearchDataList(searchDataList, offset, limit))
                .build();
        logger.info("[RESPONSE] {}", rs);
        return rs;
    }

    private List<SearchData> getSearchDataList(String query, Long siteId) {
        List<SearchIndex> searchIndexList = getRelevanceSearchIndexList(query, siteId);
        TreeSet<SearchData> searchDataList = new TreeSet<>(new SearchDataByMaxRelevenceComparator());
        Float maxTotalRelevance = getMaxTotalRelevance(searchIndexList);
        for (SearchIndex searchIndex : searchIndexList) {
            searchDataList.add(SearchData.builder()
                    .site(searchIndex.getPage().getSite().getUrl())
                    .siteName(searchIndex.getPage().getSite().getName())
                    .uri(searchIndex.getPage().getPath())
                    .title(searchIndex.getPage().getTitle())
                    .snippet(searchIndex.getSnippet() == null ? "" : searchIndex.getSnippet())
                    .relevance(getTotalRelevance(searchIndexList, searchIndex) / maxTotalRelevance)
                    .build());
        }
        return searchDataList.stream().toList();
    }

    private List<SearchIndex> getRelevanceSearchIndexList(String query, Long siteId) {
        Set<String> lemmaSet = lemanizer.getLemmaSet(query);
        if (lemmaSet.isEmpty()) return new ArrayList<>(0);
        TreeSet<SearchIndexMap> searchIndexMaps = new TreeSet<>(new SearchIndexMapByFrequencyComparator());
        for (String keyLemma : lemmaSet) {
                List<SearchIndex> searchIndexList = siteId == null ? searchIndexDao.selectByLemma(keyLemma) : searchIndexDao.selectByLemmaAndSiteId(keyLemma, siteId);
                if (!searchIndexList.isEmpty()) {
                    searchIndexMaps.add(new SearchIndexMap(keyLemma, searchIndexList));
                }
        }
        return getRelevanceSearchIndexList(searchIndexMaps);
    }

    private List<SearchIndex> getRelevanceSearchIndexList(TreeSet<SearchIndexMap> searchIndexMaps) {
        if (searchIndexMaps.isEmpty()) return new ArrayList<>(0);
        List<SearchIndex> relevanceSearchIndexList = new ArrayList<>();
        Set<SearchIndex> firstSearchIndexList = new HashSet<>(searchIndexMaps.first().getSearchIndexList());
        Iterator<SearchIndexMap> iterator = searchIndexMaps.iterator();
        iterator.next();
        while (iterator.hasNext()) {
            List<SearchIndex> currentSearchIndexList = iterator.next().getSearchIndexList();
            currentSearchIndexList
                    .forEach(searchIndex -> {
                        firstSearchIndexList.forEach(firstSearchIndex -> {
                            if (searchIndex.getPage().getPath().equals(firstSearchIndex.getPage().getPath())) {
                                relevanceSearchIndexList.add(searchIndex);
                            }
                        });
                    });
            firstSearchIndexList.addAll(currentSearchIndexList);
        }
        if (relevanceSearchIndexList.isEmpty()
                || relevanceSearchIndexList.size() >= searchIndexMaps.first().getSearchIndexList().size()) return firstSearchIndexList.stream().toList();
        return relevanceSearchIndexList;
    }

    private Float getMaxTotalRelevance(List<SearchIndex> searchIndexList) {
        Optional<Float> maxTotalRelevance = searchIndexList.stream()
                .map(SearchIndex::getLemmaRank)
                .max(Comparator.naturalOrder());
        return maxTotalRelevance.orElse(null);
    }

    private Float getTotalRelevance(List<SearchIndex> searchIndexList, SearchIndex searchIndex) {
        return searchIndexList.stream()
                .filter(i -> i.getPage().equals(searchIndex.getPage()))
                .map(SearchIndex::getLemmaRank)
                .reduce((float) 0, Float::sum);
    }

    private List<SearchData> getSliceOfSearchDataList(List<SearchData> searchDataList, Integer offset, Integer limit) {
        if (searchDataList.size() > offset) {
            if (searchDataList.size() > offset + limit) {
                return searchDataList.subList(offset, offset + limit);
            } else return searchDataList.subList(offset, searchDataList.size());
        }
        if (searchDataList.size() > limit) return searchDataList.subList(0, limit);
        return searchDataList;
    }
}

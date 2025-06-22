package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchDataByMaxRelevenceComparator;
import searchengine.dto.search.SearchRs;
import searchengine.entity.SearchIndex;
import searchengine.repositorys.LemmaDao;
import searchengine.repositorys.SearchIndexDao;
import searchengine.utils.Lemanizer;

import java.util.*;

import static searchengine.config.SearchConfig.LIMIT;
import static searchengine.config.SearchConfig.OFFSET;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteListConfig siteListConfig;

    private final LemmaDao lemmaDao;
    private final SearchIndexDao searchIndexDao;

    @Autowired
    private final Lemanizer lemanizer;

    @Override
    public SearchRs search(String query, String siteUrl, Integer offset, Integer limit) {
        if (siteUrl != null) {
            SiteConfig siteConfig = siteListConfig.findSiteNameConfig(siteUrl);
            if (siteConfig == null)
                return SearchRs.getBaseRs(false, "The transferred site is located outside the sites specified in the configuration file");
        }

        offset = offset == null ? OFFSET : offset;
        limit = limit == null ? LIMIT : limit;

        List<SearchData> searchDataList = getSearchDataList(query, offset, limit);
        return SearchRs.builder()
                .result(true)
                .count(searchDataList.size())
                .data(searchDataList)
                .build();
    }

    private List<SearchData> getSearchDataList(String query, Integer offset, Integer limit) {
        List<SearchIndex> searchIndexList = getRelevanceSearchIndexList(query);
        TreeSet<SearchData> searchDataSet = new TreeSet<>(new SearchDataByMaxRelevenceComparator());
        Float maxTotalRelevance = getMaxTotalRelevance(searchIndexList);
        for (SearchIndex searchIndex : searchIndexList) {
            searchDataSet.add(SearchData.builder()
                    .site(searchIndex.getPage().getSite().getUrl())
                    .siteName(searchIndex.getPage().getSite().getName())
                    .uri(searchIndex.getPage().getPath())
                    .title(searchIndex.getPage().getTitle())
                    .snippet(searchIndex.getSnippet() == null ? "" : searchIndex.getSnippet())
                    .relevance(getTotalRelevance(searchIndexList, searchIndex) / maxTotalRelevance)
                    .build());
        }

        return searchDataSet.size() > offset && searchDataSet.size() > limit ?
                searchDataSet.stream().toList().subList(offset, limit) : searchDataSet.stream().toList();
    }

    private List<SearchIndex> getRelevanceSearchIndexList(String query) {
        Set<String> lemmaSet = lemanizer.getLemmaMap(query).keySet();
        if (lemmaSet.isEmpty()) return new ArrayList<>(0);
        TreeSet<SearchIndexMap> searchIndexMaps = new TreeSet<>(new SearchIndexMapByFrequencyComparator());
        for (String keyLemma : lemmaSet) {
                List<SearchIndex> searchIndexList = searchIndexDao.selectByLemma(keyLemma);
            System.out.println(searchIndexList.size());
                if (searchIndexList.size() > 100) searchIndexList = searchIndexList.subList(0, 50);
                if (!searchIndexList.isEmpty()) {
                    searchIndexMaps.add(new SearchIndexMap(keyLemma, searchIndexList));
                }
        }
        return getRelevanceSearchIndexList(searchIndexMaps);
    }

    private List<SearchIndex> getRelevanceSearchIndexList(TreeSet<SearchIndexMap> searchIndexMaps) {
        if (searchIndexMaps.isEmpty()) return new ArrayList<>(0);
        Set<SearchIndex> relevanceSearchIndexList = new HashSet<>();
        List<SearchIndex> firstSearchIndexList = new ArrayList<>(searchIndexMaps.first().getSearchIndexList());
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
                        firstSearchIndexList.addAll(relevanceSearchIndexList);
                    });
        }
        if (relevanceSearchIndexList.isEmpty()) return firstSearchIndexList;
        return relevanceSearchIndexList.stream().toList();
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
}

package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.dto.enums.Status;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsRs;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.entity.Site;
import searchengine.repositorys.LemmaDao;
import searchengine.repositorys.PageDao;
import searchengine.repositorys.SiteDao;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final LemmaDao lemmaDao;

    @Override
    public StatisticsRs getStatistics() {
        List<Site> siteList = siteDao.findAll();

        if (siteList.isEmpty()) {
            return getErrorStatisticsRs();
        }
        StatisticsRs rs = StatisticsRs.builder()
                .result(true)
                .statistics(StatisticsData.builder()
                        .total(TotalStatistics.builder()
                                .sites(siteList.size())
                                .pages(pageDao.selectCountPages())
                                .lemmas(lemmaDao.selectCountLemmas())
                                .indexing(isIndexing(siteList))
                                .build())
                        .detailed(getDetailedStatisticsItemList(siteList))
                        .build())
                .build();
        logger.info("[RESPONSE] {}", rs.toString());
        return rs;
    }

    private List<DetailedStatisticsItem> getDetailedStatisticsItemList(List<Site> siteList) {
        List<DetailedStatisticsItem> detailedStatisticsItemList = new ArrayList<>(siteList.size());
        siteList.forEach(site -> {
            int lemmas = lemmaDao.selectCountLemmasBySite(site);
            detailedStatisticsItemList.add(DetailedStatisticsItem.builder()
                            .url(site.getUrl())
                            .name(site.getName())
                            .status(site.getStatus().name())
                            .statusTime(site.getStatusTime().getTime())
                            .error(site.getLastError())
                            .pages(site.getPages().size())
                            .lemmas(lemmas)
                    .build());
        });
        return detailedStatisticsItemList;
    }

    private boolean isIndexing(List<Site> siteList) {
        for (Site site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private StatisticsRs getErrorStatisticsRs() {
        StatisticsRs rs = StatisticsRs.builder()
                .result(false)
                .error("Индексированные сайты отсутствуют")
                .build();
        logger.info("[RESPONSE] {}", rs.toString());
        return rs;
    }
}

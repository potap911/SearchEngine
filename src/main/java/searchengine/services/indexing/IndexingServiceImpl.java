package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.BaseRs;
import searchengine.dto.enums.Status;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.repositorys.PageDao;
import searchengine.repositorys.SiteDao;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private static final String INDEXING_IS_NOT_RUNNING_ERROR = "Индексация не запущена";
    private static final String INDEXING_IS_RUNNING_ERROR = "Индексация уже запущена";
    private static final String INDEXING_STOPPED_BY_USER_LAST_ERROR = "[STOPPED_INDEXING] [Индексация остановлена пользователем]";
    private static final String NOT_RECEIPTED_SITE_STRING_FORMAT_LAST_ERROR = "[NOT_RECEIPTED] [Site name: %s] [url: %s] [id: %s]";
    private static final String NOT_RECEIPTED_PAGE_HTTP_EXP_STRING_FORMAT_LAST_ERROR = "[NOT_RECEIPTED] %s [Status code: %s] [Message: %s]";
    private static final String NOT_RECEIPTED_PAGE_IO_EXP_STRING_FORMAT_LAST_ERROR = "[NOT_RECEIPTED] %s [Message: %s]";

    private ForkJoinPool pool;
    private Map<String, Set<String>> sitesMap;
    private final SiteListConfig siteListConfig;

    private final SiteDao siteDao;
    private final PageDao pageDao;

    private static StringBuilder lastErrorBuilder;
    private static boolean isStartIndexing = false;

    @Override
    public BaseRs startIndexing() {
        if (isStartIndexing) {
            return BaseRs.builder()
                    .result(false)
                    .error(INDEXING_IS_RUNNING_ERROR)
                    .build();
        }

        isStartIndexing = true;
        pageDao.deleteAll();
        siteDao.deleteAll();

        pool = new ForkJoinPool(siteListConfig.getSites().size());
        sitesMap = new HashMap<>(siteListConfig.getSites().size());
        lastErrorBuilder = new StringBuilder();

        siteListConfig.getSites()
                .forEach(siteConfig -> {
                    Site site = siteDao.save(Site.builder()
                            .name(siteConfig.getName())
                            .url(siteConfig.getUrl())
                            .status(Status.INDEXING)
                            .statusTime(Timestamp.from(Instant.now()))
                            .build());
                    if (Domain.isAvailable(siteConfig.getUrl())) {
                        sitesMap.put(site.getName(), new HashSet<>());
                        pool.submit(new Thread(() -> startIndexing(site, site.getUrl(), site.getUrl())));
                    } else {
                        String warn = String.format(NOT_RECEIPTED_SITE_STRING_FORMAT_LAST_ERROR, site.getName(), site.getUrl(), site.getId());
                        lastErrorBuilder.append(warn).append("\n");
                        siteDao.updateStatus(site.getId(), Status.FAILED, lastErrorBuilder.toString());
                        logger.warn("[NOT CONNECTION] [name: {}] [url: {}] [id: {}]", site.getName(), site.getUrl(), site.getId());
                    }
                });
        return BaseRs.builder()
                .result(true)
                .build();
    }

    private void startIndexing(Site site, String previousUrl, String currentUrl) {
        if (pool.isShutdown()) return;
        try {
            Domain domain = Domain.getInstance(site.getUrl(), previousUrl, currentUrl);
            domain.getInnerLinkSet()
                    .forEach(link -> {
                        if (sitesMap.get(site.getName()).add(link)) {
                            pageDao.save(Page.builder()
                                    .site(site)
                                    .code(domain.getCode())
                                    .content(domain.getHtml())
                                    .path(link.replaceAll(site.getUrl(), ""))
                                    .build());
                            siteDao.updateStatusTime(site.getId());
                            if (link.contains(site.getUrl())) {
                                startIndexing(site, currentUrl, link);
                            } startIndexing(site, currentUrl, site.getUrl() + link);
                        }
                    });
        } catch (HttpStatusException e) {
            String warn = String.format(NOT_RECEIPTED_PAGE_HTTP_EXP_STRING_FORMAT_LAST_ERROR, currentUrl, e.getStatusCode(), e.getMessage());
            lastErrorBuilder.append(warn).append("\n");
            siteDao.updateLastError(site.getId(), lastErrorBuilder.toString());
            logger.warn(warn);
        } catch (IOException e) {
            String warn = String.format(NOT_RECEIPTED_PAGE_IO_EXP_STRING_FORMAT_LAST_ERROR, currentUrl, e.getMessage());
            lastErrorBuilder.append(warn).append("\n");
            siteDao.updateLastError(site.getId(), lastErrorBuilder.toString());
            logger.warn(warn);
        }
        if (!pool.isShutdown() && currentUrl.equals(site.getUrl())) {
            siteDao.updateStatus(site.getId(), Status.INDEXED);
        }
    }

    @Override
    public BaseRs stopIndexing() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
            siteDao.updateStatusAfterStopIndexing(Status.INDEXING, Status.FAILED, INDEXING_STOPPED_BY_USER_LAST_ERROR);
            isStartIndexing = false;
            return BaseRs.builder()
                    .result(true)
                    .build();
        }
        return BaseRs.builder()
                .result(false)
                .error(INDEXING_IS_NOT_RUNNING_ERROR)
                .build();
    }

    @Override
    public BaseRs indexPage(String url) {
        /*String siteName = siteListConfig.getSites().stream()
                .filter(siteConfig -> url.contains(siteConfig.getUrl()))
                .map(siteConfig -> siteConfig.getName());*/


        Domain domain = Domain.getInstance()
        return null;
    }
}

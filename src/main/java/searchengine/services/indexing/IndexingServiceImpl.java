package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.BaseRs;
import searchengine.dto.enums.Status;
import searchengine.dto.indexing.IndexPageRq;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.repositorys.PageDao;
import searchengine.repositorys.SiteDao;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static searchengine.constants.Constant.URL_REGEX;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private ForkJoinPool pool;
    private Map<String, Set<String>> sitesMap;
    private final SiteListConfig siteListConfig;

    private final SiteDao siteDao;
    private final PageDao pageDao;

    private static final StringBuilder lastErrorBuilder = new StringBuilder();

    @Override
    public BaseRs startIndexing() {
        if (pool != null && !pool.isShutdown()) return getBaseRs(false, "Индексация уже запущена");

        pageDao.deleteAll();
        siteDao.deleteAll();

        pool = new ForkJoinPool(siteListConfig.getSites().size());
        sitesMap = new HashMap<>(siteListConfig.getSites().size());

        siteListConfig.getSites()
                .forEach(siteConfig -> {
                    if (siteConfig.getUrl().matches(URL_REGEX)) {
                        try {
                            String currentUrl = new URI(siteConfig.getUrl()).normalize().toString();
                            Site site = siteDao.save(Site.builder()
                                    .name(siteConfig.getName())
                                    .url(currentUrl)
                                    .status(Status.INDEXING)
                                    .statusTime(Timestamp.from(Instant.now()))
                                    .build());
                            sitesMap.put(site.getName(), new HashSet<>());
                            pool.submit(new Thread(() -> startIndexing(site, site.getUrl(), site.getUrl())));
                        } catch (URISyntaxException e) {
                            saveSiteUrlValidationFailed(siteConfig,
                                    String.format("[URL_VALIDATION_FAILED] %s", e.getMessage()));
                        }
                    } else saveSiteUrlValidationFailed(siteConfig, String.format("[URL_VALIDATION_FAILED] %s %s", siteConfig.getName(), siteConfig.getUrl()));
                });
        return getBaseRs(true, null);
    }

    private void startIndexing(Site site, String previousUrl, String currentUrl) {
        if (pool.isShutdown()) return;

        Domain domain = getDomain(site, previousUrl, currentUrl);
        if (domain != null) {
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
                            }
                            startIndexing(site, currentUrl, site.getUrl() + link);
                        }
                    });
        }

        if (!pool.isShutdown() && site.getStatus() == Status.INDEXING && currentUrl.equals(site.getUrl())) {
            siteDao.updateStatus(site.getId(), Status.INDEXED);
        }
    }

    @Override
    public BaseRs stopIndexing() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
            siteDao.updateStatusAfterStopIndexing(Status.INDEXING, Status.FAILED,
                    "[STOPPED_INDEXING] [Индексация остановлена пользователем]");
            return getBaseRs(true, null);
        }
        return getBaseRs(false, "Индексация не запущена");
    }

    @Override
    public BaseRs indexPage(IndexPageRq rq) {
        String url = null;
        try {
            url = new URI(rq.getUrl()).normalize().toString();
        } catch (URISyntaxException e) {
            String warn = String.format("[URL_VALIDATION_FAILED] %s", e.getMessage());
            logger.warn(warn);
        }

        String siteName = findSiteNameConfig(url);
        if (siteName == null) {
            return getBaseRs(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        Site site = siteDao.findByName(siteName);
        String path = url.replaceAll(site.getUrl(), "/");
        ;
        System.out.println(path);
        Page page = pageDao.findByPath(path);
        if (page == null) page = new Page();

        Domain domain = getDomain(site, url, url);
        if (domain != null) {
            page.setSite(site);
            page.setCode(domain.getCode());
            page.setContent(domain.getHtml());
            page.setPath(path);
            pageDao.save(page);
            siteDao.updateStatusTime(site.getId());
            return getBaseRs(true, null);
        } else return getBaseRs(false, "Данная страница не получена");
    }

    private String findSiteNameConfig(String url) {
        for (SiteConfig siteConfig : siteListConfig.getSites()) {
            if (url.contains(siteConfig.getUrl())) {
                return siteConfig.getName();
            }
        }
        return null;
    }

    private Domain getDomain(Site site, String previousUrl, String currentUrl) {
        try {
            return Domain.getInstance(site.getUrl(), previousUrl, currentUrl);
        } catch (IOException e) {
            saveLastError(site, currentUrl, e.getMessage());
        }
        return null;
    }

    private void saveLastError(Site site, String currentUrl, String message) {
        String warn = String.format("[NOT_RECEIPTED] %s [Message: %s]", currentUrl, message);
        logger.warn(warn);
        lastErrorBuilder.append(warn).append("\n");
        if (site.getUrl().equals(currentUrl)) {
            site.setStatus(Status.FAILED);
            siteDao.updateStatus(site.getId(), Status.FAILED, lastErrorBuilder.toString());
        } else siteDao.updateLastError(site.getId(), lastErrorBuilder.toString());
    }

    private BaseRs getBaseRs(boolean result, String error) {
        logger.debug(String.format("[BaseRs] [result: %s] [error: %s]", result, error));
        return BaseRs.builder()
                .result(result)
                .error(error)
                .build();
    }

    private void saveSiteUrlValidationFailed(SiteConfig siteConfig, String warn) {
        logger.warn(warn);
        siteDao.save(Site.builder()
                .name(siteConfig.getName())
                .url(siteConfig.getUrl())
                .status(Status.FAILED)
                .lastError(warn)
                .statusTime(Timestamp.from(Instant.now()))
                .build());
    }
}

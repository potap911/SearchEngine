package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.BaseRs;
import searchengine.dto.enums.Status;
import searchengine.dto.indexing.IndexPageRq;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.repositorys.LemmaDao;
import searchengine.repositorys.PageDao;
import searchengine.repositorys.SiteDao;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static searchengine.config.BrowserConfig.URL_REGEX;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private ForkJoinPool pool;
    private HashMap<String, Set<String>> sitesMap;
    private final SiteListConfig siteListConfig;

    private final SiteDao siteDao;
    private final PageDao pageDao;
    private final LemmaDao lemmaDao;

    @Autowired
    private final Lemanizer lemanizer;

    @Override
    public BaseRs startIndexing() {
        if (pool != null && !pool.isShutdown()) return getBaseRs(false, "Индексация уже запущена");
        lemmaDao.deleteAll();
        pageDao.deleteAll();
        siteDao.deleteAll();
        pool = new ForkJoinPool(siteListConfig.getSites().size());
        sitesMap = new HashMap<>(siteListConfig.getSites().size());
        runSiteCrawling();
        return getBaseRs(true, null);
    }

    @Override
    public BaseRs stopIndexing() {
        if (pool != null && !pool.isShutdown()) {
            clearPoolAndSitesMap();
            siteDao.updateStatusAfterStopIndexing(Status.INDEXING, Status.FAILED,
                    "[STOPPED_INDEXING] Индексация остановлена пользователем");
            return getBaseRs(true, null);
        }
        return getBaseRs(false, "Индексация не запущена");
    }

    @Override
    public BaseRs indexPage(IndexPageRq rq) {
        String pageUrl = normalizeUrl(rq.getUrl());
        if (pageUrl == null) return getBaseRs(false, "Данная страница не прошла валидацию");
        String siteName = findSiteNameConfig(pageUrl);
        if (siteName == null) {
            return getBaseRs(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        Site site = siteDao.findByName(siteName);
        String path = pageUrl.replaceAll(site.getUrl(), "/");
        Page page = pageDao.findByPath(path);
        if (page == null) page = new Page();
        PageContent pageContent = getPageContent(site, pageUrl, pageUrl);
        if (pageContent != null) {
            page.setSite(site);
            page.setCode(pageContent.getCode());
            page.setContent(pageContent.getHtml());
            page.setPath(path);
            pageDao.save(page);
            saveLemma(site, pageContent.getHtml());
            siteDao.updateStatusTime(site.getId());
            return getBaseRs(true, null);
        } else return getBaseRs(false, "Данная страница не получена");
    }

    private void runSiteCrawling() {
        siteListConfig.getSites()
                .forEach(siteConfig -> {
                    String siteUrl = normalizeUrl(siteConfig.getUrl());
                    if (siteUrl != null) {
                        Site site = saveSiteToIndexing(siteConfig.getName(), siteUrl);
                        pool.submit(new Thread(() -> runPageCrawling(site, site.getUrl(), site.getUrl())));
                    } else
                        saveSiteUrlValidationFailed(siteConfig, String.format("[URL_VALIDATION_FAILED] %s %s", siteConfig.getName(), siteConfig.getUrl()));
                });
    }

    private void runPageCrawling(Site site, String previousUrl, String currentUrl) {
        if (pool.isShutdown()) return;
        PageContent pageContent = getPageContent(site, previousUrl, currentUrl);
        if (pageContent == null) return;
        savePageContent(site, pageContent, currentUrl.replaceFirst(site.getUrl(), "/"));
        saveLemma(site, pageContent.getHtml());
        siteDao.updateStatusTime(site.getId());
        pageContent.getInnerLinkSet()
                .forEach(link -> {
                    if (sitesMap.get(site.getName()).add(link)) {
                        if (link.startsWith(site.getUrl())) {
                            String nextUrl = normalizeUrl(link);
                            if (nextUrl != null) runPageCrawling(site, currentUrl, nextUrl);
                        } else if (!link.startsWith("http")) {
                            String nextUrl = normalizeUrl(site.getUrl() + link);
                            if (nextUrl != null) runPageCrawling(site, currentUrl, nextUrl);
                        }
                    }
                });
        if (!pool.isShutdown() && currentUrl.equals(site.getUrl())) {
            if (site.getStatus() == Status.INDEXING) {
                siteDao.updateStatus(site.getId(), Status.INDEXED);
            }
        }
    }

    private String findSiteNameConfig(String url) {
        for (SiteConfig siteConfig : siteListConfig.getSites()) {
            if (url.contains(siteConfig.getUrl())) {
                return siteConfig.getName();
            }
        }
        return null;
    }

    private PageContent getPageContent(Site site, String previousUrl, String currentUrl) {
        try {
            return PageContent.getContent(site.getUrl(), previousUrl, currentUrl);
        } catch (IOException e) {
            saveLastError(site, currentUrl, e.getMessage());
            return null;
        }
    }

    private Site saveSiteToIndexing(String name, String url) {
        Site site = siteDao.save(Site.builder()
                .name(name)
                .url(url)
                .status(Status.INDEXING)
                .statusTime(Timestamp.from(Instant.now()))
                .build());
        sitesMap.put(site.getName(), new HashSet<>());
        logger.info("[SAVED_SITE] {}", url);
        return site;
    }

    private void savePageContent(Site site, PageContent pageContent, String link) {
        if (link.length() > 255) link = link.substring(0, 255);
        pageDao.save(Page.builder()
                .site(site)
                .code(pageContent.getCode())
                .content(pageContent.getHtml())
                .path(link)
                .build());
        logger.info("[SAVED_PAGE] {}", link);
    }

    private void saveLemma(Site site, String html) {
        Map<String, Integer> lemmaMap = lemanizer.getLemmaMap(html);
        if (lemmaMap == null) return;
        lemmaMap.keySet().forEach(keyLemma -> {
            Lemma lemma = lemmaDao.findByLemma(keyLemma);
            if (lemma == null) {
                lemma = Lemma.builder()
                        .lemma(keyLemma)
                        .site(site)
                        .frequency(1)
                        .build();
                lemmaDao.save(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() + lemmaMap.get(keyLemma));
                lemmaDao.save(lemma);
            }
        });
        logger.info("[SAVED_LEMMAS] {}", site.getUrl());
    }

    private void saveLastError(Site site, String currentUrl, String message) {
        String error = String.format("[NOT_RECEIPTED] %s %s", currentUrl, message);
        logger.info(error);
        String lastError = siteDao.selectLastError(site.getId());
        lastError = lastError == null ? error + "\n" : lastError + error + "\n";
        lastError = lastError.length() > 65000 ? lastError.substring(1000, lastError.length() - 1) : lastError;
        if (currentUrl.equals(site.getUrl())) {
            site.setStatus(Status.FAILED);
            siteDao.updateStatus(site.getId(), Status.FAILED, lastError);
        } else siteDao.updateLastError(site.getId(), lastError);
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
        logger.info("[SAVED_SITE_VALIDATION_FAILED] {}", siteConfig.getUrl());
    }

    private void clearPoolAndSitesMap() {
        pool.shutdownNow();
        pool = null;
        sitesMap.clear();
        sitesMap = null;
    }

    private String normalizeUrl(String url) {
        if (url.matches(URL_REGEX)) {
            try {
                return new URI(url).normalize().toString();
            } catch (URISyntaxException e) {
                logger.warn(String.format("[URL_VALIDATION_FAILED] %s %s", url, e.getMessage()));
                return null;
            }
        }
        logger.warn(String.format("[URL_VALIDATION_FAILED] %s", url));
        return null;
    }

    private BaseRs getBaseRs(boolean result, String error) {
        BaseRs rs = BaseRs.builder()
                .result(result)
                .error(error)
                .build();
        logger.info("[RESPONSE] {}", rs.toString());
        return rs;
    }
}

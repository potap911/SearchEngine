package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;
import searchengine.dto.BaseRs;
import searchengine.dto.enums.Status;
import searchengine.dto.indexing.IndexPageRq;
import searchengine.entity.*;
import searchengine.repositorys.*;
import searchengine.utils.Lemanizer;
import searchengine.utils.PageContent;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

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
    private final AliasDao aliasDao;
    private final SearchIndexDao searchIndexDao;

    @Autowired
    private final Lemanizer lemanizer;

    @Override
    public BaseRs startIndexing() {
        logger.debug("[START_INDEXING]");
        if (pool != null && !pool.isShutdown()) return BaseRs.getBaseRs(false, "Indexing is already running");
        deleteAllIndex();
        pool = new ForkJoinPool(siteListConfig.getSites().size());
        sitesMap = new HashMap<>(siteListConfig.getSites().size());
        runSiteCrawling();
        return BaseRs.getBaseRs(true, null);
    }

    private void runSiteCrawling() {
        logger.debug("[RUN_SITE_CRAWLING]");
        siteListConfig.getSites()
                .forEach(siteConfig -> {
                    String siteUrl = SiteListConfig.normalizeUrl(siteConfig.getUrl());
                    if (siteUrl != null) {
                        Site site = saveSiteToIndexing(siteConfig.getName(), siteUrl);
                        sitesMap.put(site.getName(), new HashSet<>());
                        pool.submit(new Thread(() -> runPageCrawling(site, site.getUrl(), site.getUrl())));
                    } else
                        saveSiteUrlValidationFailed(siteConfig, String.format("[URL_VALIDATION_FAILED] %s %s", siteConfig.getName(), siteConfig.getUrl()));
                });
    }

    private void runPageCrawling(Site site, String previousUrl, String currentUrl) {
        logger.debug("[RUN_PAGE_CRAWLING] {}", currentUrl);
        if (pool.isShutdown()) return;
        PageContent pageContent = getPageContent(site, previousUrl, currentUrl);
        if (pageContent == null) return;
        Page page = savePage(site, pageContent, currentUrl);
        saveLemmaAndSearchIndex(site, page);
        siteDao.updateStatusTime(site.getId());
        pageContent.getInnerLinkSet()
                .forEach(link -> {
                    if (sitesMap.get(site.getName()).add(link)) {
                        if (link.startsWith(site.getUrl())) {
                            String nextUrl = SiteListConfig.normalizeUrl(link);
                            if (nextUrl != null) runPageCrawling(site, currentUrl, nextUrl);
                        } else if (!link.startsWith("http")) {
                            String nextUrl = SiteListConfig.normalizeUrl(site.getUrl() + link);
                            if (nextUrl != null) runPageCrawling(site, currentUrl, nextUrl);
                        }
                    }
                });
        if (!pool.isShutdown() && currentUrl.equals(site.getUrl())) {
            if (site.getStatus() == Status.INDEXING) {
                siteDao.updateStatus(site.getId(), Status.INDEXED);
                logger.info("[INDEXED] {}, {}", site.getName(), site.getUrl());
            }
        }
    }

    @Override
    public BaseRs stopIndexing() {
        logger.debug("[STOP_INDEXING]");
        if (pool != null && !pool.isShutdown()) {
            clearPoolAndSitesMap();
            siteDao.updateStatusAfterStopIndexing(Status.INDEXING, Status.FAILED,
                    "[STOPPED_INDEXING] Indexing stopped by the user");
            return BaseRs.getBaseRs(true, null);
        }
        return BaseRs.getBaseRs(false, "Indexing is not running");
    }

    @Override
    public BaseRs indexPage(IndexPageRq rq) {
        logger.debug("[INDEX_PAGE]");
        SiteConfig siteConfig = siteListConfig.findSiteNameConfig(rq.getUrl());
        if (siteConfig == null) {
            return BaseRs.getBaseRs(false, "This page is located outside the sites specified in the configuration file");
        }
        runUpdatePage(siteConfig);
        return BaseRs.getBaseRs(true, null);
    }

    @Override
    public BaseRs addAlias(String word, String aliasesInput) {
        Set<String> lemmaSet = lemanizer.getLemmaSet(word);
        if (lemmaSet.isEmpty()) return BaseRs.getBaseRs(false,
                "Couldn't get the normal form of the word");
        if (lemmaSet.size() > 1) return BaseRs.getBaseRs(false,
                "Too many word. Let's say you enter only one word");
        Lemma lemma = lemmaDao.findByLemma(lemmaSet.stream().findFirst().orElseThrow());
        if (lemma == null) return BaseRs.getBaseRs(false,
                "No lemmas were found for this word");

        Alias aliasObg = aliasDao.findByAlias(aliasesInput);
        if (aliasObg != null) {
            return BaseRs.getBaseRs(false,
                    "This alias has already been assigned for lemma '" + aliasObg.getLemma().getLemma() + "'");
        }
        aliasDao.save(Alias.builder()
                .alias(aliasesInput)
                .lemma(lemma)
                .build());
        logger.info("[SAVED_ALIAS] word: {} alias: {}", word, aliasesInput);
        return BaseRs.getBaseRs(true, null);
    }

    private void runUpdatePage(SiteConfig siteConfig) {
        logger.debug("[RUN_UPDATE_PAGE]");
        new Thread(() -> {
            Site site = siteDao.findByName(siteConfig.getName());

            if (site == null) {
                site = saveSiteToIndexing(siteConfig.getName(), siteConfig.getUrl());
            }

            String path = siteConfig.getUrl().replaceAll(site.getUrl(), "/");
            Page page = pageDao.findByPathAndSiteId(path, site.getId());
            deleteIndexPage(page);

            PageContent pageContent = getPageContent(site, siteConfig.getUrl(), siteConfig.getUrl());
            if (pageContent != null) {
                page = savePage(site, pageContent, siteConfig.getUrl());
                saveLemmaAndSearchIndex(site, page);
                siteDao.updateStatusTime(site.getId());
            }
        }).start();
    }

    private PageContent getPageContent(Site site, String previousUrl, String currentUrl) {
        logger.debug("[GET_PAGE_CONTENT]");
        try {
            return PageContent.getContent(site.getUrl(), previousUrl, currentUrl);
        } catch (IOException e) {
            saveLastError(site, currentUrl, e.getMessage());
            return null;
        }
    }

    private Site saveSiteToIndexing(String name, String url) {
        logger.debug("[SAVE_SITE_TO_INDEXING]");
        Site site = siteDao.save(Site.builder()
                .name(name)
                .url(url)
                .status(Status.INDEXING)
                .statusTime(Timestamp.from(Instant.now()))
                .build());
        logger.debug("[SAVED_SITE] {}", url);
        return site;
    }

    private Page savePage(Site site, PageContent pageContent, String pageUrl) {
        logger.debug("[SAVE_PAGE_CONTENT]");
        pageUrl = pageUrl.replaceFirst(site.getUrl(), "/");
        if (pageUrl.length() > 255) pageUrl = pageUrl.substring(0, 255);
        Page page = pageDao.findByPathAndSiteId(pageUrl, site.getId());
        if (page != null) return page;
        page = pageDao.save(Page.builder()
                .site(site)
                .code(pageContent.getCode())
                .title(pageContent.getTitle())
                .content(pageContent.getHtml())
                .path(pageUrl)
                .build());
        logger.debug("[SAVED_PAGE] {}", pageUrl);
        return page;
    }

    private void saveLemmaAndSearchIndex(Site site, Page page) {
        logger.debug("[SAVE_LEMMA_AND_SEARCH_INDEX] {} {}", site, page);
        if (page == null) return;
        Map<String, Integer> lemmaMap = lemanizer.getLemmaMap(Jsoup.parse(page.getContent()).text());
        if (lemmaMap == null) return;
        lemmaMap.keySet().forEach(keyLemma -> {
            Lemma lemma = saveLemma(site, keyLemma);
            saveSearchIndex(page, lemma, (float) lemmaMap.get(keyLemma));
        });
    }

    private Lemma saveLemma(Site site, String keyLemma) {
        logger.debug("[SAVE_LEMMA] {} for {}", keyLemma, site.getUrl());
        Lemma lemma = lemmaDao.findByLemma(keyLemma);
        if (lemma == null) {
            lemma = Lemma.builder()
                    .lemma(keyLemma)
                    .build();
            lemma = lemmaDao.save(lemma);
            logger.debug("[SAVED_LEMMA] {} for {}", keyLemma, site.getUrl());
        }
        return lemma;
    }

    private void saveSearchIndex(Page page, Lemma lemma, Float lemmaCnt) {
        logger.debug("[SAVE_SEARCH_INDEX]");
        if (searchIndexDao.findByPageAndLemma(page, lemma) == null) {
            searchIndexDao.save(SearchIndex.builder()
                    .lemmaRank((lemmaCnt / 100))
                    .snippet(findSnippet(lemma.getLemma(), page.getContent()))
                    .page(page)
                    .lemma(lemma)
                    .build());
            logger.debug("[SAVED_SEARCH_INDEX] Lemma id: {} for Page id: {}", lemma.getId(), page.getId());
        }
    }

    private void saveLastError(Site site, String currentUrl, String message) {
        logger.debug("[SAVE_LAST_ERROR] {}", message);
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
        logger.debug("[SAVE_SITE_URL_VALIDATION_FAILED] {}", warn);
        logger.warn(warn);
        siteDao.save(Site.builder()
                .name(siteConfig.getName())
                .url(siteConfig.getUrl())
                .status(Status.FAILED)
                .lastError(warn)
                .statusTime(Timestamp.from(Instant.now()))
                .build());
        logger.debug("[SAVED_SITE_URL_VALIDATION_FAILED] {}", siteConfig.getUrl());
    }

    private void clearPoolAndSitesMap() {
        logger.debug("[CLEAR_POOL_AND_SITES_MAP]");
        pool.shutdownNow();
        pool = null;
        sitesMap.clear();
        sitesMap = null;
    }

    private void deleteAllIndex() {
        logger.debug("[DELETE_ALL_INDEX]");
        lemmaDao.deleteAll();
        pageDao.deleteAll();
        siteDao.deleteAll();
        searchIndexDao.deleteAll();
    }

    private void deleteIndexPage(Page page) {
        logger.debug("[DELETE_INDEX_PAGE] {}", page);
        if (page == null) return;
        List<SearchIndex> searchIndexList = searchIndexDao.findAllByPage(page);
        searchIndexList.forEach(searchIndex -> {
            aliasDao.deleteByLemmaId(searchIndex.getLemma().getId());
            lemmaDao.delete(searchIndex.getLemma());
            searchIndexDao.delete(searchIndex);
        });
        pageDao.delete(page);
    }

    private String findSnippet(String lemma, String html) {
        String text = Jsoup.parse(html).text();
        int start = indexOfSnippet(lemma, text);

        if (start >= 0) {
            int end = start + 240;
            end = Math.min(end, text.length() - 1);
            text = text.substring(start, end);
        } else text = null;

        if (text != null) {
            return "<b>..." + text + "...</b>";
        }
        return text;
    }

    private int indexOfSnippet(String lemma, String text) {
        int start = text.indexOf(lemma);

        if (start < 0) {
            String upLemma = lemma.substring(0, 1).toUpperCase() + lemma.substring(1);
            start = text.indexOf(upLemma);
        }

        for (int i = 2; i <= 3 && start < 0; i++) {
            String halfLemma = lemma.substring(0, lemma.length() / i + 1);
            start = text.indexOf(halfLemma);
            if (start < 0) {
                String upLemma = halfLemma.substring(0, 1).toUpperCase() + halfLemma.substring(1);
                start = text.indexOf(upLemma);
            }
        }
        return start;
    }
}

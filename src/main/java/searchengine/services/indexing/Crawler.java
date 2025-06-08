package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import searchengine.config.SiteConfig;
import searchengine.config.SiteListConfig;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class Crawler {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final ForkJoinPool pool;

    private final Map<String, String> domainMap;

    private static final long t0 = System.currentTimeMillis();


    public Crawler(List<SiteConfig> siteConfigList) {
        pool = new ForkJoinPool();
        domainMap = new ConcurrentHashMap<>();

        siteConfigList.forEach(siteConfig -> domainMap.putIfAbsent(siteConfig.getName(), siteConfig.getUrl()));

        logger.info("");
        logger.info("Crawler initialized");
        logger.info("-------------------------");
    }

    public void start() {
        logger.info("Start indexing...");


        for (String name : domainMap.keySet()) {

        }
        try {
            Document document = Jsoup.connect(domainMap.get("Skillbox"))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 YaBrowser/25.4.0.0 Safari/537.36")
                    .get();

            String html = document.body().html();
            System.out.println(html);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public void start(String url) {
        logger.info("Start indexing...");

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 YaBrowser/25.4.0.0 Safari/537.36")
                    .get();

            String html = document.body().html();
            System.out.println(html);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}

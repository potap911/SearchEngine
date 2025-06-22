package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.BrowserConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class PageContent {
    private static final Logger logger = LoggerFactory.getLogger(PageContent.class);
    private Set<String> innerLinkSet;
    private String title;
    private String html;
    private int code;

    private PageContent(int code, String title, String html, Set<String> innerLinkSet) {
        this.code = code;
        this.title = title;
        this.html = html;
        this.innerLinkSet = innerLinkSet;
    }

    public static PageContent getContent(String rootUrl, String previousUrl, String currentUrl) throws IOException {
        Connection.Response response = request(currentUrl);
        Document document = response.parse();

        Set<String> innerLinkSet = new HashSet<>();
        document.select("a[href]")
                .forEach(element -> {
                    String link = element.attr("href");
                    if (isInnerLink(link, rootUrl, previousUrl, currentUrl)) {
                        innerLinkSet.add(link);
                    }
                });

        logger.info("[RECEIPTED] {}", currentUrl);
        return new PageContent(response.statusCode(), document.title(), document.html(), innerLinkSet);
    }

    @SneakyThrows
    public static Connection.Response request(String url) {
        return Jsoup.connect(url)
                .timeout(BrowserConfig.TIMEOUT)
                .userAgent(BrowserConfig.USER_AGENT)
                .referrer(BrowserConfig.REFERRER)
                .execute();
    }

    private static boolean isInnerLink(String link, String rootUrl, String previousUrl, String currentUrl) {
        return !link.isEmpty()
                && !link.equals(currentUrl)
                && !link.equals(rootUrl)
                && !link.equals(previousUrl)
                && !link.equals("/")
                && !link.contains(".jpg")
                && !link.contains(".mp3")
                && !link.contains(".webp")
                && !link.contains(".pdf")
                && !link.contains(".svg")
                && !link.contains(".js")
                && !link.contains(".json")
                && !link.contains(".jpeg")
                && !link.contains(".png")
                && !link.contains(".gif")
                && !link.contains(".exe")
                && !link.contains("#")
                && !link.contains("mailto:");
    }
}

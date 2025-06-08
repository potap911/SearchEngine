package searchengine.services.indexing;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.UserAgentConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Domain {
    private static final Logger logger = LoggerFactory.getLogger(Domain.class);
    private Set<String> innerLinkSet;
    private String html;
    private int code;

    private Domain(int code, String html, Set<String> innerLinkSet) {
        this.code = code;
        this.html = html;
        this.innerLinkSet = innerLinkSet;
    }

    public static Domain getInstance(String rootUrl, String previousUrl, String currentUrl) throws IOException, HttpStatusException {
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
        return new Domain(response.statusCode(), document.body().html(), innerLinkSet);
    }

    public static Connection.Response request(String url) throws IOException {
        return Jsoup.connect(url)
                .timeout(UserAgentConfig.TIMEOUT)
                .userAgent(UserAgentConfig.USER_AGENT)
                .referrer(UserAgentConfig.REFERRER)
                .execute();
    }

    public static boolean isAvailable(String url) {
        try {
            return request(url).statusCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isInnerLink(String link, String rootUrl, String previousUrl, String currentUrl) {
        return !link.isEmpty()
                && !link.equals(currentUrl)
                && !link.equals(rootUrl)
                && !link.equals(previousUrl)
                && !link.equals("/")
                && !link.contains(".jpg")
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

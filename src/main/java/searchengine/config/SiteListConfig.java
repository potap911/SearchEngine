package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static searchengine.config.BrowserConfig.URL_REGEX;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SiteListConfig {
    private static final Logger logger = LoggerFactory.getLogger(SiteListConfig.class);
    private List<SiteConfig> sites;

    public SiteConfig findSiteNameConfig(String url) {
        url = normalizeUrl(url);
        if (url == null) return null;
        for (SiteConfig siteConfig : sites) {
            if (url.contains(siteConfig.getUrl()) || url.replaceAll("https", "http").contains(siteConfig.getUrl())) {
                return siteConfig;
            }
        }
        return null;
    }

    public static String normalizeUrl(String url) {
        logger.debug("[NORMALIZE_URL] {}", url);
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
}

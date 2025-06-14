package searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrowserConfig {

    @Value("user-agent")
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 YaBrowser/25.4.0.0 Safari/537.36";

    @Value("referrer")
    public static final String REFERRER = "https://www.google.ru/";

    @Value("timeout")
    public static final int TIMEOUT = 5000;

    @Value("url-regex")
    public static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#!:,.;/%=~_|]";
}

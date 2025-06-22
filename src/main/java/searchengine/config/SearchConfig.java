package searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SearchConfig {

    @Value("${search-default-config.offset}")
    public static final int OFFSET = 0;

    @Value("${search-default-config.limit}")
    public static final int LIMIT = 20;
}

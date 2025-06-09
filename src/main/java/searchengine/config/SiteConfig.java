package searchengine.config;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;

import static searchengine.constants.Constant.URL_REGEX;

@Getter
@Setter
public class SiteConfig {
    private String url;
    private String name;
}

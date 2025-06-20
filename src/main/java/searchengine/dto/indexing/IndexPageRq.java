package searchengine.dto.indexing;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import static searchengine.config.BrowserConfig.URL_REGEX;

@Data
public class IndexPageRq {

    @NotBlank
    @Pattern(regexp = URL_REGEX)
    private String url;
}

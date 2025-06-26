package searchengine.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import searchengine.config.SiteConfig;
import searchengine.dto.BaseRs;
import searchengine.BaseTest;
import searchengine.dto.enums.Status;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.OK;

class IndexingServiceImplTest extends BaseTest {

    @Test
    @DisplayName("[startIndexing] Start indexing")
    void startIndexingTest1() {
        var actual = startIndexingStep();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(true, null));

        for (SiteConfig site : siteListConfig.getSites()) {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from site WHERE name = '%s'", site.getName()));
            assertTrue(rowSet.absolute(1));
            assertEquals(site.getUrl(), rowSet.getString("url"));
            assertEquals(Status.INDEXING.name(), rowSet.getString("status"));
        }
    }

    @Test
    @DisplayName("[startIndexing] Indexing is already running")
    void startIndexingTest2() {
        startIndexingStep();

        var actual = startIndexingStep();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(false, "Indexing is already running"));
    }

    @Test
    @DisplayName("[startIndexing] Restarting indexing after stopping")
    void startIndexingTest3() {
        startIndexingStep();
        stopIndexingStep();

        var actual = startIndexingStep();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(true, null));

        for (SiteConfig site : siteListConfig.getSites()) {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from site WHERE name = '%s'", site.getName()));
            assertTrue(rowSet.absolute(1));
            assertEquals(site.getUrl(), rowSet.getString("url"));
            assertEquals(Status.INDEXING.name(), rowSet.getString("status"));
        }
    }

    @Test
    @DisplayName("[stopIndexing] Indexing was not started")
    void stopIndexingTest1() {
        var actual = stopIndexingStep();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(false, "Indexing is not running"));
    }

    @Test
    @DisplayName("[stopIndexing] Start indexing. Stop indexing")
    void stopIndexingTest2() {
        startIndexingStep();

        var actual = stopIndexingStep();
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(true, null));

        for (SiteConfig site : siteListConfig.getSites()) {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from site WHERE name = '%s'", site.getName()));
            assertTrue(rowSet.absolute(1));
            assertEquals(site.getUrl(), rowSet.getString("url"));
            assertEquals(Status.FAILED.name(), rowSet.getString("status"));
            assertEquals("[STOPPED_INDEXING] Indexing stopped by the user",
                    rowSet.getString("last_error"));
        }
    }

    @Test
    @DisplayName("[indexPage] This page is located outside the sites specified in the configuration file")
    void indexPageTest1() {
        var actual = indexPageStep("http://www.wrong.ru/root1.html");
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(false, "This page is located outside the sites specified in the configuration file"));
    }

    @Test
    @DisplayName("[indexPage] Indexing individual pages")
    void indexPageTest2() throws InterruptedException {
        SiteConfig siteConfig = siteListConfig.getSites().get(0);

        var actual = indexPageStep(siteConfig.getUrl());
        assertThat(actual).isNotNull();
        assertThat(actual.getStatusCode()).isEqualTo(OK);
        var response = actual.getBody();
        assertThat(response).isEqualTo(BaseRs.getBaseRs(true, null));

        TimeUnit.SECONDS.sleep(3);

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from site WHERE name = '%s'", siteConfig.getName()));
        assertTrue(rowSet.absolute(1));
        assertEquals(siteConfig.getUrl(), rowSet.getString("url"));
        assertEquals(Status.INDEXING.name(), rowSet.getString("status"));
        Long siteId = rowSet.getLong("id");

        rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from page WHERE site_id = '%s'", siteId));
        assertTrue(rowSet.absolute(1));
        Long pageId = rowSet.getLong("id");

        rowSet = jdbcTemplate.queryForRowSet(String.format("SELECT * from search_index WHERE page_id = '%s'", pageId));
        assertTrue(rowSet.next());

        rowSet = jdbcTemplate.queryForRowSet("SELECT * from lemma");
        assertTrue(rowSet.next());

    }

    private ResponseEntity<BaseRs> startIndexingStep() {
        return restTemplate.exchange(
                HOST + PORT + prefix + startIndexing,
                HttpMethod.GET,
                new HttpEntity<>(null),
                BaseRs.class
        );
    }

    private ResponseEntity<BaseRs> stopIndexingStep() {
        return restTemplate.exchange(
                HOST + PORT + prefix + stopIndexing,
                HttpMethod.GET,
                new HttpEntity<>(null),
                BaseRs.class
        );
    }

    private ResponseEntity<BaseRs> indexPageStep(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("url", url);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        return restTemplate.postForEntity(
                HOST + PORT + prefix + indexPage,
                new HttpEntity<>(map, headers),
                BaseRs.class);
    }
}
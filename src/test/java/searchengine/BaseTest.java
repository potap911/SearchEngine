package searchengine;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.config.SiteListConfig;

import javax.sql.DataSource;

@Import({SiteListConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseTest {

    @Value("${endpoint.searchEngineApi.prefix}")
    protected String prefix;

    @Value("${endpoint.searchEngineApi.method.startIndexing}")
    protected String startIndexing;

    @Value("${endpoint.searchEngineApi.method.stopIndexing}")
    protected String stopIndexing;

    @Value("${endpoint.searchEngineApi.method.indexPage}")
    protected String indexPage;

    @Value("${endpoint.searchEngineApi.method.addAlias}")
    protected String addAlias;

    @Autowired
    protected SiteListConfig siteListConfig;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected final String HOST = "http://localhost:";

    @LocalServerPort
    protected int PORT;

    protected JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.driverClassName}")
    private String dbDriverClassName;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @BeforeEach
    public void setUp() {
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(dbDriverClassName)
                .url(dbUrl)
                .username(dbUserName)
                .password(dbPassword)
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("DELETE FROM search_index");
        jdbcTemplate.execute("DELETE FROM alias");
        jdbcTemplate.execute("DELETE FROM lemma");
        jdbcTemplate.execute("DELETE FROM page");
        jdbcTemplate.execute("DELETE FROM site");
    }
}

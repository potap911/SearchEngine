package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.BaseRs;
import searchengine.dto.indexing.IndexPageRq;
import searchengine.dto.search.SearchRs;
import searchengine.dto.statistics.StatisticsRs;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("${endpoint.searchEngineApi.prefix}")
@RequiredArgsConstructor
public class SearchEngineApiController {
    private static final Logger logger = LoggerFactory.getLogger(SearchEngineApiController.class);

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("${endpoint.searchEngineApi.method.statistics}")
    public ResponseEntity<StatisticsRs> statistics() {
        logger.info("[REQUEST] /statistics");
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("${endpoint.searchEngineApi.method.startIndexing}")
    public ResponseEntity<BaseRs> startIndexing() {
        logger.info("[REQUEST] /startIndexing");
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("${endpoint.searchEngineApi.method.stopIndexing}")
    public ResponseEntity<BaseRs> stopIndexing() {
        logger.info("[REQUEST] /stopIndexing");
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("${endpoint.searchEngineApi.method.indexPage}")
    public ResponseEntity<BaseRs> indexPage(@Valid @ModelAttribute IndexPageRq rq) {
        logger.info("[REQUEST] /indexPage {}", rq.getUrl());
        return ResponseEntity.ok(indexingService.indexPage(rq));
    }

    @GetMapping("${endpoint.searchEngineApi.method.search}")
    public ResponseEntity<SearchRs> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit
    ) {
        logger.info("[REQUEST] /search");
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}

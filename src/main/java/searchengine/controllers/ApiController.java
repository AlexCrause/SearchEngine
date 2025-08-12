package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search_data.SearchService;
import searchengine.services.statistics.StatisticsService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestParam("url") String url) {
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam("query") String query,
                                 @RequestParam(value = "site", required = false) String site,
                                 @RequestParam(value = "offset", defaultValue = "0") int offset,
                                 @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return searchService.search(query, site, offset, limit);
    }
}

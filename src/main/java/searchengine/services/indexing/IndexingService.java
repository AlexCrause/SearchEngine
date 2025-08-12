package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingResponse;

import java.util.Optional;

public interface IndexingService {

    IndexingResponse startIndexing();

    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String url);
}

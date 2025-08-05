package searchengine.services.indexing;

import java.util.Optional;

public interface IndexingService {

    Optional<?> startIndexing();

    Optional<?> stopIndexing();

    Optional<?> indexPage(String url);
}

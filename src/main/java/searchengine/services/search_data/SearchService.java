package searchengine.services.search_data;

import java.util.Optional;

public interface SearchService {
    Optional<?> search(String query, String site, int offset, int limit);
}

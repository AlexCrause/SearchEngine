package searchengine.services.search_data;

import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface SearchService {
    ResponseEntity<?> search(String query, String site, int offset, int limit);
}

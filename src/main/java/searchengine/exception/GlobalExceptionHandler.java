package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.indexing.IndexingResponseError;
import searchengine.dto.search.SearchResponseError;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<SearchResponseError> catchResourceNotFoundException(SearchException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new SearchResponseError(false, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<IndexingResponseError> catchIndexingProcessException(IndexingException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new IndexingResponseError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}

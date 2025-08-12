package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponseError {

    private boolean result;
    private String error;

    public IndexingResponseError(boolean b, String message) {
        this.result = b;
        this.error = message;
    }
}

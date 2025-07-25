package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponseError {

    private boolean result;
    private String error;

    public SearchResponseError(boolean b, String message) {
        this.result = b;
        this.error = message;
    }
}

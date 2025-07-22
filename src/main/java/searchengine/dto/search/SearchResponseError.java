package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponseError {

    private boolean result;
    private String error;
}

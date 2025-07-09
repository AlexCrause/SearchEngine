package searchengine.dto.statistics;

import lombok.Data;

@Data
public class IndexingResponseError {

    private boolean result;
    private String error;
}

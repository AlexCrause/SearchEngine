package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.AppConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@RequiredArgsConstructor
public class IndexingPage {


    public static void indexPage(String url,
                                 PageIndexingService pageIndexingService) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(AppConfig.getUserAgent())
                    .referrer(AppConfig.getReferrer())
                    .execute();
            Document document = response.parse();
            pageIndexingService.saveHTMLPage(url, document);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

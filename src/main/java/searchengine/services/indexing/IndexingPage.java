package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.AppConfig;
import searchengine.lemmatizer.Lemmatizer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

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
            String stringRes = Lemmatizer.clearWebPageFromHtmlTags(document);
            HashMap<String, Integer> lemmasWithCount = Lemmatizer.lemmatize(stringRes);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

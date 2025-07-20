package searchengine.services.indexing.page_indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.AppConfig;
import searchengine.services.indexing.lemma_indexing.LemmaIndexingService;
import searchengine.services.indexing.lemma_indexing.Lemmatizer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class IndexingPage {

    public static void indexPage(String urlPage,
                                 String urlSite,
                                 PageIndexingService pageIndexingService,
                                 LemmaIndexingService lemmaIndexingService) {
        try {
            Connection.Response response = Jsoup.connect(urlPage)
                    .userAgent(AppConfig.getUserAgent())
                    .referrer(AppConfig.getReferrer())
                    .execute();
            Document document = response.parse();
            int statusCode = response.statusCode();

            pageIndexingService.saveHTMLPage(urlSite, urlPage, document, statusCode);

            System.out.println("URLSite " + urlSite);
            System.out.println("URLPage " + urlPage);
            Lemmatizer lemmatizer = new Lemmatizer(
                    urlSite,
                    urlPage,
                    lemmaIndexingService);
            String stringRes = lemmatizer.clearWebPageFromHtmlTags(document);
            System.out.println(stringRes);
            Map<String, Integer> lemmatized = lemmatizer.lemmatize(stringRes);
            for (Map.Entry<String, Integer> stringIntegerEntry : lemmatized.entrySet()) {
                String word = stringIntegerEntry.getKey();
                Integer count = stringIntegerEntry.getValue();
                System.out.println("word: " + word + " count: " + count);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

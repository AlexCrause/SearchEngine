package searchengine.services.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.AppConfig;
import searchengine.services.lemmatizer.Lemmatizer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class IndexingPage {

    public static void indexPage(String urlPage,
                                 String urlSite,
                                 PageIndexingService pageIndexingService,
                                 SiteIndexingService siteIndexingService,
                                 LemmaIndexingService lemmaIndexingService,
                                 IndexIndexingService indexIndexingService) {
        try {
            Connection.Response response = Jsoup.connect(urlPage)
                    .userAgent(AppConfig.getUserAgent())
                    .referrer(AppConfig.getReferrer())
                    .execute();
            Document document = response.parse();

            pageIndexingService.saveHTMLPage(urlPage, document);


            Lemmatizer lemmatizer = new Lemmatizer(urlSite, urlPage, siteIndexingService,
                    lemmaIndexingService, indexIndexingService);
            String stringRes = lemmatizer.clearWebPageFromHtmlTags(document);
            HashMap<String, Integer> lemmatized = lemmatizer.lemmatize(stringRes);
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

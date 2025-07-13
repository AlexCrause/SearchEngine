package searchengine.services.indexing.page_indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.AppConfig;
import searchengine.services.indexing.IndexIndexingService;
import searchengine.services.indexing.lemma_indexing.LemmaIndexingService;
import searchengine.services.indexing.lemma_indexing.Lemmatizer;
import searchengine.services.indexing.site_indexing.SiteIndexingService;

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


            Lemmatizer lemmatizer = new Lemmatizer(
                    urlSite,
                    urlPage,
                    siteIndexingService,
                    pageIndexingService,
                    lemmaIndexingService,
                    indexIndexingService);
            String stringRes = lemmatizer.clearWebPageFromHtmlTags(document);
            System.out.println(stringRes);
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

package searchengine.services.indexing.lemma_indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class Lemmatizer {

    private String urlSite;
    private String urlPage;
    private LemmaIndexingService lemmaIndexingService;

    private final LuceneMorphology luceneMorph1;
    private final LuceneMorphology luceneMorph2;

    {
        try {
            luceneMorph1 = new RussianLuceneMorphology();
            luceneMorph2 = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //private static final Pattern WORD_PATTERN = Pattern.compile("[а-яА-ЯёЁa-zA-Z`-]+");
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}&&[^\\p{So}]]+(?:-[\\p{L}&&[^\\p{So}]]+)*");

    private final Map<String, Integer> lemmatizedText = new ConcurrentHashMap<>();
    private final Map<String, String> lemmaCache = new ConcurrentHashMap<>();


    public Lemmatizer(String urlSite,
                      String urlPage,
                      LemmaIndexingService lemmaIndexingService) {
        this.urlSite = urlSite;
        this.urlPage = urlPage;
        this.lemmaIndexingService = lemmaIndexingService;
    }

    public Lemmatizer() {
    }

    public Map<String, Integer> lemmatize(String text) {
        processText(text);
        saveLemmasToDB();
        return lemmatizedText;
    }

    private void processText(String text) {
        if (text == null || text.isEmpty()) return;

        WORD_PATTERN.matcher(text).results()
                .map(match -> match.group().toLowerCase(Locale.ROOT))
                .filter(word -> word.length() > 2)
                .filter(word -> !word.matches("[а-яё]{1,2}|[a-z]{1,2}"))
                .forEach(lowerCase -> {
                    String lemma = lemmaCache.computeIfAbsent(lowerCase, this::getLemma);

                    if (lemma != null && lemma.length() > 2) {
                        lemmatizedText.merge(lemma, 1, Integer::sum);
                    }
                });
    }

    private String getLemma(String word) {
        try {
            LuceneMorphology morphology = word.matches("[а-яё-]+") ? luceneMorph1 : luceneMorph2;
            return morphology.getMorphInfo(word).stream()
                    .filter(this::isAllowedPartOfSpeech)
                    .findFirst()
                    .map(this::extractLemma)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAllowedPartOfSpeech(String morphInfo) {
        return !(morphInfo.contains("|l") ||   // Частица
                morphInfo.contains("|e") ||   // Предлог
                morphInfo.contains("|c") ||
                morphInfo.contains("|r") ||   // Местоимение
                morphInfo.contains("|1 ADJECTIVE") ||   //smart, cg и др.
                morphInfo.contains("|o") ||   // Междометие(точно)
                morphInfo.contains("|a Г") ||   //
                morphInfo.contains("|f МС-П") ||   //
                morphInfo.contains("|n") ||  // Союз(точно)
                morphInfo.contains("|1 PN") ||
                morphInfo.contains("|s") ||
                morphInfo.contains("|h") ||
                morphInfo.contains("|g") ||
                morphInfo.contains("arda|1 NOUN narr,pl")
        );
    }

    private String extractLemma(String morphInfo) {
        int index = morphInfo.indexOf('|');
        return index > 0 ? morphInfo.substring(0, index) : morphInfo;
    }

    private void saveLemmasToDB() {
        if (lemmaIndexingService == null || lemmatizedText.isEmpty()) return;

        try {
            String pathPage = new URL(urlPage).getPath();
            lemmaIndexingService.saveLemmasBatch(urlSite, pathPage, lemmatizedText);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String clearWebPageFromHtmlTags(Document doc) {
        if (doc == null) return "";
        String html = doc.html();
        Document document = Jsoup.parse(html);
        return document.text();
    }

    public String clearWebPageFromHtmlTags(String html) {
        if (html == null) return "";
        return Jsoup.parse(html).text();
    }
}

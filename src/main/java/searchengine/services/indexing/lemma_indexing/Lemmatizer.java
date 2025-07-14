package searchengine.services.indexing.lemma_indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.services.indexing.page_indexing.PageIndexingService;
import searchengine.services.indexing.site_indexing.SiteIndexingService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lemmatizer {

    private String urlSite;
    private String urlPage;
    private final HashMap<String, Integer> lemmatizedText = new HashMap<>();
    private SiteIndexingService siteIndexingService;
    private LemmaIndexingService lemmaIndexingService;


    public Lemmatizer(String urlSite,
                      String urlPage,
                      SiteIndexingService siteIndexingService,
                      PageIndexingService pageIndexingService,
                      LemmaIndexingService lemmaIndexingService) {
        this.urlSite = urlSite;
        this.urlPage = urlPage;
        this.siteIndexingService = siteIndexingService;
        this.lemmaIndexingService = lemmaIndexingService;
    }

    public Lemmatizer(){}

    public HashMap<String, Integer> lemmatize(String text) throws IOException {
        return splitTextIntoWords(text);
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


    private HashMap<String, Integer> splitTextIntoWords(String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return null;
        }
        HashMap<String, Integer> hashMap = new HashMap<>();
        String regex = "[а-яА-ЯёЁa-zA-Z`-]+";

        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            LuceneMorphology luceneMorph1 = new RussianLuceneMorphology();
            LuceneMorphology luceneMorph2 = new EnglishLuceneMorphology();
            String lowerCase = matcher.group().toLowerCase(Locale.ROOT);
            if (lowerCase.matches("[а-яА-ЯёЁ-]+")) {
                List<String> wordBaseForms = luceneMorph1.getMorphInfo(lowerCase);
                hashMap = sortWordsByMorph(wordBaseForms);
            } else if (lowerCase.matches("[a-zA-Z`-]+")) {
                List<String> wordBaseForms = luceneMorph2.getMorphInfo(lowerCase);
                hashMap = sortWordsByMorph(wordBaseForms);
            }
        }
        return hashMap;
    }

    private HashMap<String, Integer> sortWordsByMorph(List<String> wordBaseForms) {
        for (String s : wordBaseForms) {
            if (!(s.contains("|l") || s.contains("|n") || s.contains("|f") || s.contains("|e") ||
                    (s.contains("|Y КР_ПРИЛ") || s.contains("|o")))) {
                return cutWord(s);
            }
        }
        return null;
    }

    private HashMap<String, Integer> cutWord(String s) {
        System.out.println(s);
        String pureWord = "";
        int index = s.indexOf('|');
        pureWord = s.substring(0, index);
        return assembleHashMapWords(pureWord);
    }

    private HashMap<String, Integer> assembleHashMapWords(String lemma) {
        if (!lemmatizedText.containsKey(lemma)) {
            lemmatizedText.put(lemma, 1);
        } else {
            lemmatizedText.put(lemma, lemmatizedText.get(lemma) + 1);
        }
        saveToDB(lemma);
        return lemmatizedText;
    }

    private void saveToDB(String lemma) {
        try {
            String pathPage = new URL(urlPage).getPath();
            lemmaIndexingService.saveLemmaToDB(lemma, urlSite, pathPage);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

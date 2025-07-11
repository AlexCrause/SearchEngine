package searchengine.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lemmatizer {

    private static final HashMap<String, Integer> lemmatizedText = new HashMap<>();

    public static HashMap<String, Integer> lemmatize(String text) throws IOException {
        return splitTextIntoWords(text);
    }

    public static String clearWebPageFromHtmlTags(Document doc){
        if (doc == null) return "";
        String html = doc.html();
        Document document = Jsoup.parse(html);
        return document.text();
    }
    public static String clearWebPageFromHtmlTags(String html){
        if (html == null) return "";
        return Jsoup.parse(html).text();
    }



    private static HashMap<String, Integer> splitTextIntoWords(String text) throws IOException {
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
            System.out.println(matcher.group());
            String lowerCase = matcher.group().toLowerCase(Locale.ROOT);
            if (lowerCase.matches("[а-яА-ЯёЁ-]+")){
                List<String> wordBaseForms = luceneMorph1.getMorphInfo(lowerCase);
                hashMap = sortWordsByMorph(wordBaseForms);
            } else if (lowerCase.matches("[a-zA-Z`-]+")){
                List<String> wordBaseForms = luceneMorph2.getMorphInfo(lowerCase);
                hashMap = sortWordsByMorph(wordBaseForms);
            }
        }
        return hashMap;
    }

    private static HashMap<String, Integer> sortWordsByMorph(List<String> wordBaseForms) {
        for (String s : wordBaseForms) {
            if (!(s.contains("|l") || s.contains("|n") || s.contains("|f") || s.contains("|e") ||
                    (s.contains("|Y КР_ПРИЛ"))))
            {
                return cutWord(s);
            }
        }
        return null;
    }

    private static HashMap<String, Integer> cutWord(String s) {
        String pureWord = "";
        int index = s.indexOf('|');
        pureWord = s.substring(0, index);
        return assembleHashMapWords(pureWord);
    }

    private static HashMap<String, Integer> assembleHashMapWords(String pureWord) {
        if (!lemmatizedText.containsKey(pureWord)) {
            lemmatizedText.put(pureWord, 1);
        } else {
            lemmatizedText.put(pureWord, lemmatizedText.get(pureWord) + 1);
        }
        return lemmatizedText;
    }
}

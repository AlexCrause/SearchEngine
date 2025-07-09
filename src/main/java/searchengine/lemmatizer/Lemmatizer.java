package searchengine.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lemmatizer {

    private final HashMap<String, Integer> lemmatizedText = new HashMap<>();


    public HashMap<String, Integer> lemmatize(String text) throws IOException {

        return splitTextIntoWords(text);
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
            List<String> wordBaseForms = luceneMorph1.getMorphInfo(matcher.group().toLowerCase(Locale.ROOT));
            hashMap = sortWordsByMorph(wordBaseForms);
        }
        return hashMap;

    }

    private HashMap<String, Integer> sortWordsByMorph(List<String> wordBaseForms) {

        for (String s : wordBaseForms) {
            if (!(s.contains("|l") || s.contains("|n") || s.contains("|f") || s.contains("|e") ||
                    (s.contains("|Y КР_ПРИЛ"))))
            {
                return cutWord(s);
            }
        }
        return null;
    }

    private HashMap<String, Integer> cutWord(String s) {
        String pureWord = "";
        int index = s.indexOf('|');
        pureWord = s.substring(0, index);
        return assembleHashMapWords(pureWord);
    }

    private HashMap<String, Integer> assembleHashMapWords(String pureWord) {
        if (!lemmatizedText.containsKey(pureWord)) {
            lemmatizedText.put(pureWord, 1);
        } else {
            lemmatizedText.put(pureWord, lemmatizedText.get(pureWord) + 1);

        }
        return lemmatizedText;
    }

}

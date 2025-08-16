package searchengine.services.search_data;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.services.indexing.lemma_indexing.Lemmatizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SnippetGenerator {

    private static final int CONTEXT_LENGTH = 100;
    private final Lemmatizer lemmatizer = new Lemmatizer();

    public String generate(String htmlContent, List<String> lemmas) {
        if (lemmas.isEmpty() || htmlContent == null || htmlContent.isBlank()) {
            return "";
        }

        String text = extractTextFromHtml(htmlContent);
        if (text.isBlank()) return "";

        List<WordPosition> words = splitTextWithPositions(text);

        Map<String, String> wordToLemma = mapWordsToLemmas(words);

        Set<String> queryLemmas = lemmas.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Integer> hitIndexes = findMatchingWordIndexes(words, wordToLemma, queryLemmas);

        if (hitIndexes.isEmpty()) return "";

        int centerIndex = hitIndexes.get(0);
        String snippet = extractSnippetAroundCenter(text, words.get(centerIndex));

        return highlightWordForms(snippet, wordToLemma, queryLemmas);
    }


    private String extractTextFromHtml(String html) {
        try {
            Document doc = Jsoup.parse(html);
            return doc.body().text();
        } catch (Exception e) {
            return "";
        }
    }

    private List<WordPosition> splitTextWithPositions(String text) {
        List<WordPosition> result = new ArrayList<>();
        Matcher m = Pattern.compile("\\p{L}+(?:-\\p{L}+)*").matcher(text);
        while (m.find()) {
            result.add(new WordPosition(m.group(), m.start(), m.end()));
        }
        return result;
    }

    private Map<String, String> mapWordsToLemmas(List<WordPosition> words) {
        Map<String, String> wordToLemma = new HashMap<>();
        for (WordPosition wp : words) {
            String lemma = lemmatizer.lemmatize(wp.word)
                    .keySet()
                    .stream()
                    .findFirst()
                    .orElse("");
            wordToLemma.put(wp.word, lemma.toLowerCase());
        }
        return wordToLemma;
    }

    private List<Integer> findMatchingWordIndexes(List<WordPosition> words,
                                                  Map<String, String> wordToLemma,
                                                  Set<String> queryLemmas) {
        List<Integer> hitIndexes = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String lemma = wordToLemma.get(words.get(i).word);
            if (queryLemmas.contains(lemma)) {
                hitIndexes.add(i);
            }
        }
        return hitIndexes;
    }

    private String extractSnippetAroundCenter(String text, WordPosition centerWord) {
        int start = Math.max(0, centerWord.start - CONTEXT_LENGTH);
        int end = Math.min(text.length(), centerWord.end + CONTEXT_LENGTH);
        return "..." + text.substring(start, end).trim() + "...";
    }

    private String highlightWordForms(String snippet,
                                      Map<String, String> wordToLemma,
                                      Set<String> queryLemmas) {
        Matcher m = Pattern.compile("\\p{L}+(?:-\\p{L}+)*").matcher(snippet);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String word = m.group();
            String lemma = wordToLemma.getOrDefault(word, word).toLowerCase();
            if (queryLemmas.contains(lemma)) {
                m.appendReplacement(sb, "<b>" + word + "</b>");
            } else {
                m.appendReplacement(sb, word);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }




    private static class WordPosition {
        String word;
        int start;
        int end;

        WordPosition(String word, int start, int end) {
            this.word = word;
            this.start = start;
            this.end = end;
        }
    }
}

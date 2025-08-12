package searchengine.services.search_data;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SnippetGenerator {

    private static final int CONTEXT_LENGTH = 100;

    public String generate(String htmlContent, List<String> lemmas) {
        // 1. Извлекаем текст из HTML
        String text = extractTextFromHtml(htmlContent);

        // 2. Получаем срез текста
        String snippet = extractSnippet(text, lemmas, CONTEXT_LENGTH);

        // 3. Подсвечиваем леммы
        return highlightLemmas(snippet, lemmas);
    }

    private String extractTextFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("p, div, span, li, article");
        StringBuilder sb = new StringBuilder();
        for (Element el : elements) {
            String txt = el.text();
            if (!txt.isEmpty() && !txt.contains("Подробнее")) {
                sb.append(txt).append(" ");
            }
        }
        return sb.toString().trim();
    }


    public String extractSnippet(String text, List<String> lemmas, int contextLength) {
        String lowerText = text.toLowerCase();

        for (String lemma : lemmas) {
            int index = lowerText.indexOf(lemma);
            if (index == -1) {
                return "";
            }
            int start = Math.max(0, index - contextLength);
            int end = Math.min(text.length(), index + lemma.length() + contextLength);

            String snippet = text.substring(start, end).trim();
            return "..." + snippet + "...";
        }
        return "";
    }


    private String highlightLemmas(String snippet, List<String> lemmas) {
        if (snippet.isEmpty()) return "";

        // Создаем регулярное выражение для поиска лемм
        String pattern = lemmas.stream()
                .map(lemma -> "\\b(" + lemma + ")\\b")
                .collect(Collectors.joining("|", "(?iu)", ""));

        return snippet.replaceAll(pattern, "<b>$1</b>");
    }
}

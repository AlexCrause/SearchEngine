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

    public String generate(String htmlContent, List<String> lemmas) {
        // 1. Получаем текст из HTML
        String text = extractTextFromHtml(htmlContent);

        // 2. Получаем предложения с леммами
        String context = getSurroundingContext(text, lemmas);

        // 3. Подсвечиваем леммы
        return highlightLemmas(context, lemmas);
    }

    private String extractTextFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("p, div, span, li, article");
        StringBuilder sb = new StringBuilder();
        for (Element el : elements) {
            String txt = el.text();
            if (!txt.isEmpty()) {
                sb.append(txt).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String getSurroundingContext(String fullText, List<String> lemmas) {
        String[] sentences = fullText.split("(?<=[.!?])\\s+");
        List<String> result = new ArrayList<>();

        for (String sentence : sentences) {
            for (String lemma : lemmas) {
                if (sentence.toLowerCase().contains(lemma.toLowerCase())) {
                    result.add(sentence.trim());
                    break;
                }
            }
            if (result.size() >= 2) break; // Ограничиваем двумя предложениями
        }

        return String.join(" ", result);
    }

    private String highlightLemmas(String text, List<String> lemmas) {
        if (text.isEmpty()) return "";

        // Создаём одно регулярное выражение для всех лемм
        String pattern = lemmas.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|", "(?i)\\b(", ")\\b"));

        return text.replaceAll(pattern, "<b>$1</b>");
    }
}

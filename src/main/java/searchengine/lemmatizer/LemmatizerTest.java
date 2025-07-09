package searchengine.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.List;

public class LemmatizerTest {

    public static void main(String[] args) throws IOException {

        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);
    }
}

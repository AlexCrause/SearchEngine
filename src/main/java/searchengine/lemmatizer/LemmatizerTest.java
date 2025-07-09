package searchengine.lemmatizer;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LemmatizerTest {

    public static void main(String[] args) throws IOException {

        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//        List<String> wordBaseForms = luceneMorph.getNormalForms("леса");
//        wordBaseForms.forEach(System.out::println);


        //и|o МЕЖД
        //копал|A С мр,ед,им
        //копать|a Г дст,прш,мр,ед
        //хитро|j Н
        //хитрый|Y КР_ПРИЛ ср,ед,од,но
        //синий|Y П мр,ед,вн,но
//        LuceneMorphology luceneMorph1 = new RussianLuceneMorphology();
//        List<String> wordBaseForms1 = luceneMorph1.getMorphInfo("или");
//        wordBaseForms1.forEach(System.out::println);


        Lemmatizer lemmatizer = new Lemmatizer();
        HashMap<String, Integer> lemmatize = lemmatizer.lemmatize(text);
        for (Map.Entry<String, Integer> stringIntegerEntry : lemmatize.entrySet()) {
            String key = stringIntegerEntry.getKey();
            Integer value = stringIntegerEntry.getValue();
            System.out.println(key + " - " + value);
        }

    }
}

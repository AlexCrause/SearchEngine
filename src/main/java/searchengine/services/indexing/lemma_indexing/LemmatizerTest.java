package searchengine.services.indexing.lemma_indexing;

import java.io.IOException;

public class LemmatizerTest {

    public static void main(String[] args) throws IOException {

        String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

        String html = "<div>Пример <b>текста</b></div>";



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


        String s = lemmatizer.clearWebPageFromHtmlTags(html);
        System.out.println(s);

    }
}

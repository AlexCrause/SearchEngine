package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaIndexingService {
    
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Transactional
    public void saveLemmaToDB(String lemmaWord, String urlSite) {
        int frequency = 1;
        System.out.println(urlSite);
        String normalizeUrl = UrlUtils.normalizeUrl(urlSite);
        Optional<Site> siteByUrl = siteRepository.findSiteByUrl(normalizeUrl);
        System.out.println(siteByUrl);
        if (siteByUrl.isEmpty()) return;

        Optional<Lemma> lemmaBySiteId = lemmaRepository.findLemmaByLemmaAndSiteId(
                lemmaWord, siteByUrl.get());

        Lemma lemma;
        if (lemmaBySiteId.isPresent()) {
            lemma = lemmaBySiteId.get();
            lemma.setFrequency(lemmaBySiteId.get().getFrequency() + 1);
        } else {
            lemma = new Lemma();
            lemma.setLemma(lemmaWord);
            lemma.setFrequency(frequency);
            lemma.setSiteId(siteByUrl.get());
        }
        lemmaRepository.save(lemma);
    }
}

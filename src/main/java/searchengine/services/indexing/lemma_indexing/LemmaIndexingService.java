package searchengine.services.indexing.lemma_indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaIndexingService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Transactional
    public void saveLemmaToDB(String lemmaWord, String urlSite, String pathPage) {
        //int frequency = 1;
//        System.out.println(urlSite);
        String normalizeUrl = UrlUtils.normalizeUrl(urlSite);
        Optional<Site> siteByUrl = siteRepository.findSiteByUrl(normalizeUrl);
//        System.out.println(siteByUrl);
        if (siteByUrl.isEmpty()) return;

        Optional<Page> pageOpt = pageRepository.findPageByPath(pathPage);
        if (pageOpt.isEmpty()) return;
        Optional<Lemma> lemmaOpt = lemmaRepository.findLemmaByLemmaAndSiteId(
                lemmaWord, siteByUrl.get());
        if (lemmaOpt.isEmpty()) {
            Lemma lemma = new Lemma();
            lemma.setLemma(lemmaWord);
            lemma.setFrequency(1);
            lemma.setSiteId(pageOpt.get().getSiteId());
            lemmaRepository.save(lemma);
        }
//        Optional<Index> indexOpt = indexRepository.findIndexByLemmaIdAndPageId(
//                lemmaOpt.get().getId(), pageOpt.get().getId());






    }
}

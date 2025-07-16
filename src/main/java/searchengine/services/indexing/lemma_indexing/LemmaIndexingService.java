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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaIndexingService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Transactional
    public void saveLemmaToDB(String lemmaWord,
                              String urlSite,
                              String pathPage) throws MalformedURLException {

        String normalizeUrl = UrlUtils.normalizeUrl(urlSite);
        String urlWithWWW = UrlUtils.normalizeUrlWithWWW(normalizeUrl);
        System.out.println("UrlSiteWithWWW = " + urlWithWWW);
        Optional<Site> siteByUrl = siteRepository.findSiteByUrl(urlWithWWW);
        System.out.println(siteByUrl);
        if (siteByUrl.isEmpty()) return;

        Optional<Page> pageOpt = pageRepository.findPageByPathAndSiteId(pathPage, siteByUrl.get());
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

        saveIndexToDB(lemmaWord, siteByUrl.get(), pageOpt.get());
    }

    private void saveIndexToDB(String lemmaWord,
                               Site site,
                               Page page) {
        Optional<Lemma> lemmaOpt = lemmaRepository.findLemmaByLemmaAndSiteId(
                lemmaWord, site);
        if (lemmaOpt.isEmpty()) return;

        Optional<Index> indexOpt = indexRepository.findIndexByLemmaIdAndPageId(
                lemmaOpt.get(), page);

        Index index;
        if (indexOpt.isPresent()) {
            index = indexOpt.get();
            index.setRank(index.getRank() + 1);
        } else {
            index = new Index();
            index.setLemmaId(lemmaOpt.get());
            index.setPageId(page);
            index.setRank(1);
        }
        indexRepository.save(index);
        updateLemmaFrequency(lemmaOpt.get(), page);
    }

    private void updateLemmaFrequency(Lemma lemma,
                                      Page page) {
        Optional<Index> indexOpt = indexRepository.findIndexByLemmaIdAndPageId(
                lemma, page);
        Optional<Integer> countConnections = indexRepository.findCountConnectionsLemmaIdWithPagesId(lemma);
        if (countConnections.isEmpty()) return;
        if (indexOpt.isPresent()) {
            if (indexOpt.get().getRank() == 1 && countConnections.get() > 1) {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
            }
        }
    }
}

package searchengine.services.indexing.lemma_indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaIndexingService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    private final Map<Integer, Map<String, Lemma>> lemmaCache = new ConcurrentHashMap<>();
    private final Map<Integer, Object> siteLocks = new ConcurrentHashMap<>();

    @Transactional
    public void saveLemmasBatch(String urlSite,
                                String pathPage,
                                Map<String, Integer> lemmas) throws MalformedURLException {
        long startTime = System.currentTimeMillis();
        try {
            String urlWithWWW = UrlUtils.normalizeUrlWithWWW(urlSite);
            System.out.println("urlSite = " + urlWithWWW);
            Site site = siteRepository.findSiteByUrl(urlWithWWW)
                    .orElseThrow(() -> new IllegalArgumentException("Site not found: " + urlWithWWW));

            Page page = pageRepository.findPageByPathAndSiteId(pathPage, site)
                    .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pathPage));


            Object siteLock = siteLocks.computeIfAbsent(site.getId(), k -> new Object());
            synchronized (siteLock){
                Map<String, Lemma> siteLemmas = lemmaCache.computeIfAbsent(
                        site.getId(),
                        siteId -> loadLemmasForSite(site)
                );

                List<Lemma> lemmasToSave = new ArrayList<>();
                List<Index> indicesToSave = new ArrayList<>();

                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemmaText = entry.getKey();
                    Lemma lemma = siteLemmas.get(lemmaText);

                    if (lemma == null) {
                        lemma = new Lemma();
                        lemma.setLemma(lemmaText);
                        lemma.setFrequency(1);
                        lemma.setSiteId(site);
                        siteLemmas.put(lemmaText, lemma);
                        lemmasToSave.add(lemma);
                    } else {
                        lemma.setFrequency(lemma.getFrequency() + 1);
                        lemmasToSave.add(lemma);
                    }
                }
                saveInBatches(lemmasToSave, lemmaRepository::saveAll, 100);

                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemmaText = entry.getKey();
                    float rank = entry.getValue();

                    Lemma lemma = siteLemmas.get(lemmaText);

                    Index index = new Index();
                    index.setPageId(page);
                    index.setLemmaId(lemma);
                    index.setRank(rank);
                    indicesToSave.add(index);
                }
                saveInBatches(indicesToSave, indexRepository::saveAll, 100);

                //updateSiteStatusTime(site);
            }

            log.info("Processed {} lemmas for {} in {} ms",
                    lemmas.size(), pathPage, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("Error processing lemmas for {}: {}", pathPage, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Lemma> loadLemmasForSite(Site site) {
        return lemmaRepository.findLemmaBySite(site).stream()
                .collect(Collectors.toConcurrentMap(
                        Lemma::getLemma,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private <T> void saveInBatches(List<T> entities, Consumer<List<T>> saver, int batchSize) {
        if (entities.isEmpty()) return;

        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<T> batch = entities.subList(i, end);
            saver.accept(batch);
        }
    }

//    private void updateSiteStatusTime(Site site) {
//        try {
//            site.setStatusTime(new Date());
//            siteRepository.save(site);
//        } catch (Exception e) {
//            log.error("Error updating site status time: {}", e.getMessage(), e);
//        }
//    }
}

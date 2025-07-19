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

            String normalizedUrl = UrlUtils.normalizeUrl(urlSite);
            String urlWithWWW = UrlUtils.normalizeUrlWithWWW(normalizedUrl);
            Site site = siteRepository.findSiteByUrl(urlWithWWW)
                    .orElseThrow(() -> new IllegalArgumentException("Site not found: " + normalizedUrl));

            Page page = pageRepository.findPageByPathAndSiteId(pathPage, site)
                    .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pathPage));


            Object siteLock = siteLocks.computeIfAbsent(site.getId(), k -> new Object());
            synchronized (siteLock){
                Map<String, Lemma> siteLemmas = lemmaCache.computeIfAbsent(
                        site.getId(),
                        siteId -> loadLemmasForSite(site)
                );

                List<Lemma> newLemmas = new ArrayList<>();
                //List<Lemma> updatedLemmas = new ArrayList<>();
                List<Index> newIndices = new ArrayList<>();


                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemmaText = entry.getKey();
                    Lemma lemma = siteLemmas.get(lemmaText);

                    if (lemma == null) {
                        lemma = createNewLemma(lemmaText, site);
                        siteLemmas.put(lemmaText, lemma);
                        newLemmas.add(lemma);
                    }
                }

                saveInBatches(newLemmas, lemmaRepository::saveAll, 100);


                for (Lemma lemma : newLemmas) {
                    if (lemma.getId() == null) {
                        log.error("ОШИБКА: Лемма '{}' не получила ID после сохранения!", lemma.getLemma());
                    }
                }


                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemmaText = entry.getKey();
                    float rank = entry.getValue();
                    Lemma lemma = siteLemmas.get(lemmaText);

                    if (lemma.getId() == null) {
                        lemma = lemmaRepository.findLemmaByLemmaAndSiteId(lemmaText, site)
                                .orElseThrow(() -> new IllegalStateException("Лемма не найдена после сохранения"));
                        siteLemmas.put(lemmaText, lemma); // Обновляем кэш
                    }

                    Index index = createIndex(page, lemma, rank);
                    newIndices.add(index);
                }


                saveInBatches(newIndices, indexRepository::saveAll, 100);

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

    private Lemma createNewLemma(String lemmaText, Site site) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaText);
        lemma.setFrequency(1);
        lemma.setSiteId(site);
        return lemma;
    }

    private Index createIndex(Page page, Lemma lemma, float rank) {
        Index index = new Index();
        index.setPageId(page);
        index.setLemmaId(lemma);
        index.setRank(rank);
        return index;
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

package searchengine.services.search_data;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.ResponseData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseError;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;
import searchengine.services.indexing.lemma_indexing.Lemmatizer;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SitesList sites;
    private final Lemmatizer lemmatizer;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;

    @Override
    public ResponseEntity<?> search(String query,
                                    String site,
                                    int offset,
                                    int limit) {
        if (limit == 0) limit = 20;
        System.out.println("Query: " + query);
        System.out.println("Site: " + site);
        System.out.println("Offset: " + offset);
        System.out.println("Limit: " + limit);
        if (query == null || query.isEmpty()) {
            SearchResponseError error = new SearchResponseError();
            error.setResult(false);
            error.setError("Задан пустой поисковый запрос");
            return ResponseEntity.ok(error);
        } else {
            if (site == null || site.isEmpty()) {
                for (Site siteEl : sites.getSites()) {
                    String siteUrl = siteEl.getUrl();
                    String siteName = siteEl.getName();
                }
            } else {
                for (Site sitesSite : sites.getSites()) {
                    if (sitesSite.getUrl().equals(site)) {

                        List<String> lemmasString = getLemmas(query);
                        List<Lemma> lemmaObjects = getLemmasBySite(lemmasString, sitesSite);
                        List<Lemma> rareLemma = excludeFrequentLemmasByPage(lemmaObjects);
                        List<Lemma> sortLemmasByFrequency = sortLemmasByFrequency(rareLemma);
                        for (Lemma lemma : sortLemmasByFrequency) {
                            System.out.println("Lemma: " + lemma.getLemma() + " | frequency: "
                                    + lemma.getFrequency() + " | id: " + lemma.getId());
                        }
                        findPagesByLemmas(sortLemmasByFrequency);
                        SearchResponse response = new SearchResponse();
                        response.setResult(true);
                        response.setCount(0);

                        List<ResponseData> listData = new ArrayList<>();
                        ResponseData data = new ResponseData();
                        data.setSite(sitesSite.getUrl());
                        data.setSiteName(sitesSite.getName());
                        listData.add(data);

                        response.setData(listData);
                        return ResponseEntity.ok(response);
                    }
                }
            }
        }
        return null;
    }


    private List<String> getLemmas(String query) {
        List<String> setLemmas = new ArrayList<>();
        Map<String, Integer> lemmas = lemmatizer.lemmatize(query);
        for (Map.Entry<String, Integer> stringIntegerEntry : lemmas.entrySet()) {
            String lemma = stringIntegerEntry.getKey();
            Integer count = stringIntegerEntry.getValue();
            System.out.println(lemma + " " + count);
            setLemmas.add(lemma);
        }
        return setLemmas;
    }

    private List<Lemma> getLemmasBySite(List<String> lemmasString, Site site) {
        List<Lemma> lemmasAtSite = new ArrayList<>();
        String normalizeUrl = UrlUtils.normalizeUrl(site.getUrl());
        Optional<searchengine.model.Site> siteOpt = siteRepository.findSiteByUrl(normalizeUrl);
        if (siteOpt.isPresent()) {
            lemmasAtSite = lemmaRepository.findLemmasAtSite(lemmasString, siteOpt.get());
            for (Lemma lemma : lemmasAtSite) {
                System.out.println("Lemma: " + lemma.getLemma() + " "
                        + lemma.getId() + " siteId: " + siteOpt.get().getId());
            }
        }
        return lemmasAtSite;
    }

    private List<Lemma> excludeFrequentLemmasByPage(List<Lemma> lemmaObjects) {
        int frequency = 50;
        Map<Lemma, Integer> mapLemmas = new HashMap<>();
        List<Lemma> rareLemmas = new ArrayList<>();
        for (Lemma lemma : lemmaObjects) {
            Optional<Integer> countPages = indexRepository.foundCountPagesByLemmaId(lemma);
            countPages.ifPresent(integer -> mapLemmas.put(lemma, integer));
        }
        for (Map.Entry<Lemma, Integer> lemmaIntegerEntry : mapLemmas.entrySet()) {
            Integer countPages = lemmaIntegerEntry.getValue();
            if (countPages < frequency) {
                rareLemmas.add(lemmaIntegerEntry.getKey());
            }
        }
        return rareLemmas;
    }

    private List<Lemma> sortLemmasByFrequency(List<Lemma> lemmaObjects) {
        return lemmaObjects.stream()
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private void findPagesByLemmas(List<Lemma> lemmas) {
        for (Lemma lemma : lemmas) {
            System.out.println("Lemma: " + lemma.getLemma() + " " + lemma.getId());
        }
        List<Page> pages = new ArrayList<>();
        for (Lemma lemma : lemmas.subList(0, 1)) {
            pages = indexRepository.findPagesByLemma(lemma);
        }

        for (int lemma = 1; lemma < lemmas.size(); lemma++) {
            List<Index> indexes = indexRepository.findIndexByLemma(lemmas.get(lemma));
            int counter = 1;
            for (Index index : indexes) {
                System.out.println(index.getId() + " " +
                        index.getLemmaId().getId() + " " +
                        index.getPageId().getId() + " " + counter++);
                pages.removeIf(page -> Objects.equals(
                        page.getId(),
                        index.getPageId().getId()));
            }
        }
        int count = 1;
        if (pages.isEmpty()) {
            System.out.println("No results");
        }
        for (Page page : pages) {
            System.out.println("Page: " + page.getId() + " " + count++);
        }
        calculatingRelevance(pages, lemmas);
    }

    private void calculatingRelevance(List<Page> pages, List<Lemma> lemmas) {
        float relativeRelevance = 0;
        float maxRelevanceAllPages = Float.MIN_VALUE;
        Map<Integer, Map<Float, Float>> map = new HashMap<>();
        for (Page page : pages) {
            float absRelevancePage = 0;
            List<Index> optionalIndex = indexRepository.findListIndexesByPageAndLemmaList(page, lemmas);
            for (Index index : optionalIndex) {
                System.out.println("Index: " + index.getId() + " | "
                        + "rank: " + index.getRank() + " | "
                        + " lemmaId: " + index.getLemmaId().getId() + " | "
                        + " pageId: " + index.getPageId().getId());
                float rank = index.getRank();
                absRelevancePage += rank;
            }
            float finalAbsRelevance = absRelevancePage;
            map.put(page.getId(), new HashMap<>() {{ put(finalAbsRelevance, relativeRelevance); }});
        }
        for (Map.Entry<Integer, Map<Float, Float>> pagesWithRelevance : map.entrySet()) {
            Map<Float, Float> relevance = pagesWithRelevance.getValue();
            for (Float relevancePage : relevance.keySet()) {
                if (relevancePage > maxRelevanceAllPages) {
                    maxRelevanceAllPages = relevancePage;
                }
            }
        }
        for (Map.Entry<Integer, Map<Float, Float>> pagesWithRelevance : map.entrySet()) {
            for (Map.Entry<Float, Float> relevance : pagesWithRelevance.getValue().entrySet()) {
                float relevancePage = relevance.getKey();
                float relativeRelevancePage = relevancePage / maxRelevanceAllPages;
                relevance.setValue(relativeRelevancePage);
            }
        }
        for (Map.Entry<Integer, Map<Float, Float>> integerMapEntry : map.entrySet()) {
            Map<Float, Float> value = integerMapEntry.getValue();
            for (Map.Entry<Float, Float> floatFloatEntry : value.entrySet()) {
                System.out.println("Page: " + integerMapEntry.getKey() + " | "
                        + "relevanceAbs: " + floatFloatEntry.getKey() + " | "
                        + "relativeRelevance: " + floatFloatEntry.getValue());
            }
        }

    }
}

package searchengine.services.search_data;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.ResponseData;
import searchengine.dto.search.SearchResponse;
import searchengine.exception.SearchException;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;
import searchengine.services.indexing.lemma_indexing.Lemmatizer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SitesList sites;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private static final double FREQUENCY = 0.8;

    @Override
    public SearchResponse search(String query,
                                   String site,
                                   int offset,
                                   int limit) {
        if (query == null || query.isBlank()) {
            throw new SearchException("Задан пустой поисковый запрос");
        }

        limit = (limit == 0) ? 20 : limit;

        if (site == null || site.isBlank()) {
            List<ResponseData> aggregatedResults = new ArrayList<>();
            for (Site s : sites.getSites()) {
                SearchResponse response = search(query, s.getUrl(), offset, limit);
                aggregatedResults.addAll(response.getData());
            }
            aggregatedResults.sort(Comparator.comparing(ResponseData::getRelevance).reversed());

            SearchResponse mergedResponse = new SearchResponse();
            mergedResponse.setResult(true);
            mergedResponse.setCount(aggregatedResults.size());
            mergedResponse.setData(aggregatedResults.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList()));

            return mergedResponse;
        }

        Optional<Site> optionalConfiguredSite = sites.getSites().stream()
                .filter(s -> s.getUrl().equals(site))
                .findFirst();

        if (optionalConfiguredSite.isEmpty()) {
            throw new SearchException("Сайт не найден в конфигурации");
        }

        Site configuredSite = optionalConfiguredSite.get();
        List<String> lemmaStrings = getLemmas(query);
        List<Lemma> lemmas = getLemmasBySite(lemmaStrings, configuredSite);
        List<Lemma> filteredLemmas = excludeFrequentLemmasByPage(lemmas, configuredSite);
        List<Lemma> sortedLemmas = sortLemmasByFrequency(filteredLemmas);

        if (sortedLemmas.isEmpty()) {
            throw new SearchException("Нет подходящих лемм для поиска");
        }

        List<Page> pages = findPagesByLemmas(sortedLemmas);
        Map<Integer, Float> relevanceMap = calculatingRelevance(pages, sortedLemmas);
        List<ResponseData> responseData = sortingAndFormingResponse(
                relevanceMap, sortedLemmas, configuredSite);

        System.out.println("Offset: " + offset + ", Limit: " + limit + ", Total: " + responseData.size());

        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(responseData.size());
        response.setData(responseData.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList()));

        return response;
    }


    private List<String> getLemmas(String query) {
        List<String> setLemmas = new ArrayList<>();
        Lemmatizer lemmatizer = new Lemmatizer();
        Map<String, Integer> lemmas = lemmatizer.lemmatize(query);
        for (Map.Entry<String, Integer> stringIntegerEntry : lemmas.entrySet()) {
            String lemma = stringIntegerEntry.getKey();
            setLemmas.add(lemma);
        }
        for (String setLemma : setLemmas) {
            System.out.println("Метод getLemmas");
            System.out.println("Lemma: " + setLemma);
        }
        System.out.println();
        return setLemmas;
    }


    private List<Lemma> getLemmasBySite(List<String> lemmasString, Site site) {
        List<Lemma> lemmasAtSite = new ArrayList<>();
        String normalizeUrl = UrlUtils.normalizeUrl(site.getUrl());
        Optional<searchengine.model.Site> siteOpt = siteRepository.findSiteByUrl(normalizeUrl);
        if (siteOpt.isPresent()) {
            lemmasAtSite = lemmaRepository.findBySiteAndLemmaIn(siteOpt.get(), lemmasString);
        }
        for (Lemma lemma : lemmasAtSite) {
            System.out.println("Метод getLemmasBySite");
            System.out.println("Lemma: " + lemma.getLemma() + " | "
                    + "LemmaId: " + lemma.getId() + " | " +
                    "LemmaFrequency: " + lemma.getFrequency() + " | " +
                    "SiteId: " + lemma.getSite().getId());
        }
        System.out.println();
        return lemmasAtSite;
    }


    private List<Lemma> excludeFrequentLemmasByPage(List<Lemma> lemmaObjects, Site site) {
        Map<Lemma, Integer> mapLemmas = new HashMap<>();
        List<Lemma> rareLemmas = new ArrayList<>();

        for (Lemma lemma : lemmaObjects) {
            int countPages = indexRepository.countByLemma(lemma);
            mapLemmas.put(lemma, countPages);
        }

        for (Map.Entry<Lemma, Integer> lemmaIntegerEntry : mapLemmas.entrySet()) {
            Integer countPages = lemmaIntegerEntry.getValue();
            System.out.println("countPages: " + countPages);
            Optional<searchengine.model.Site> siteByUrl = siteRepository.findSiteByUrl(
                    UrlUtils.normalizeUrl(site.getUrl()));
            System.out.println("siteByUrl: " + siteByUrl);
            if (siteByUrl.isPresent()) {
                int totalPages = pageRepository.countPagesBySite(siteByUrl.get());

                int threshold = (int) (totalPages * FREQUENCY);
                if (countPages < threshold) {
                    rareLemmas.add(lemmaIntegerEntry.getKey());
                }

            }
        }
        for (Lemma rareLemma : rareLemmas) {
            System.out.println("Метод excludeFrequentLemmasByPage");
            System.out.println("Lemma: " + rareLemma.getLemma() + " | "
                    + "LemmaId: " + rareLemma.getId() + " | " +
                    "LemmaFrequency: " + rareLemma.getFrequency() + " | " +
                    "SiteId: " + rareLemma.getSite().getUrl());
        }
        System.out.println();
        return rareLemmas;
    }


    private List<Lemma> sortLemmasByFrequency(List<Lemma> lemmaObjects) {
        return lemmaObjects.stream()
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .collect(Collectors.toCollection(LinkedList::new));
    }


    private List<Page> findPagesByLemmas(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) return Collections.emptyList();

        // Получаем страницы для первой леммы
        List<Page> pages = pageRepository.findPagesByLemma(lemmas.get(0));

        // Фильтруем страницы по остальным леммам
        for (int i = 1; i < lemmas.size(); i++) {
            Lemma lemma = lemmas.get(i);
            List<Page> pagesForLemma = pageRepository.findPagesByLemma(lemma);

            // Используем временный set для быстрого поиска
            Set<Integer> pageIds = pagesForLemma.stream()
                    .map(Page::getId)
                    .collect(Collectors.toSet());

            // Оставляем только страницы, содержащие текущую лемму
            pages.removeIf(page -> !pageIds.contains(page.getId()));
        }
        for (Page page : pages) {
            System.out.println("Метод findPagesByLemmas");
            System.out.println("Page: " + page.getId() + " | " +
                    "Path: " + page.getPath());
        }
        System.out.println();
        return pages;
    }


    private Map<Integer, Float> calculatingRelevance(List<Page> pages,
                                                     List<Lemma> lemmas) {
        Map<Integer, Float> relevanceMap = new HashMap<>();
        float maxRelevance = 0;

        // Вычисляем абсолютную релевантность
        for (Page page : pages) {
            float absRelevance = indexRepository.sumRankForPageAndLemmas(page, lemmas);
            relevanceMap.put(page.getId(), absRelevance);
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        // Преобразуем в относительную релевантность
        final float finalMaxRelevance = maxRelevance;
        relevanceMap.replaceAll((pageId, absRelevance) ->
                finalMaxRelevance > 0 ? absRelevance / finalMaxRelevance : 0
        );


        for (Map.Entry<Integer, Float> integerFloatEntry : relevanceMap.entrySet()) {
            System.out.println("Метод calculatingRelevance");
            System.out.println("Page: " + integerFloatEntry.getKey() + " | " +
                    "Relevance: " + integerFloatEntry.getValue());
        }
        System.out.println();
        return relevanceMap;
    }

    private List<ResponseData> sortingAndFormingResponse(Map<Integer, Float> relevanceMap,
                                                         List<Lemma> lemmas,
                                                         Site siteConfig) {
        SnippetGenerator snippetGenerator = new SnippetGenerator();
        List<ResponseData> responseDataList = new ArrayList<>();
        List<String> lemmaStrings = lemmas.stream()
                .map(Lemma::getLemma)
                .toList();

        List<Integer> sortedPageIds = relevanceMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<Page> pages = pageRepository.findAllById(sortedPageIds);

        Map<Integer, Page> pageMap = pages.stream()
                .collect(Collectors.toMap(Page::getId, Function.identity()));

        for (Integer pageId : sortedPageIds) {
            Page page = pageMap.get(pageId);
            if (page == null) continue;

            Document document = Jsoup.parse(page.getContent());
            String title = document.select("title").text();
            String snippet = "";

            snippet = snippetGenerator.generate(document.html(), lemmaStrings);

            if (snippet.isBlank()) continue;
            ResponseData data = new ResponseData();
            data.setUri(page.getPath());
            data.setTitle(title);
            data.setSnippet(snippet);
            data.setRelevance(relevanceMap.get(pageId));
            data.setSite(siteConfig.getUrl());
            data.setSiteName(siteConfig.getName());

            responseDataList.add(data);
        }
        return responseDataList;
    }
}

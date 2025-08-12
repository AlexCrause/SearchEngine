package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        long countPages = pageRepository.count();
        total.setPages((int) countPages);
        long countLemmas = lemmaRepository.count();
        total.setLemmas((int) countLemmas);


        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Optional<searchengine.model.Site> infoSite = findInfoSite(site);
            if (infoSite.isPresent()) {
                item.setPages(findCountPagesSite(infoSite.get()));
                item.setLemmas(findCountLemmasSite(infoSite.get()));
                item.setStatus(infoSite.get().getStatus().toString());
                if (infoSite.get().getLastError() != null) {
                    item.setError(infoSite.get().getLastError());
                }
                item.setStatusTime(infoSite.get().getStatusTime().getTime());
            }
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private Optional<searchengine.model.Site> findInfoSite(Site site) {
        String urlSite = site.getUrl();
        //String normalizeUrl = UrlUtils.normalizeUrl(urlSite);
        String urlWithWWW = UrlUtils.normalizeUrlWithWWW(urlSite);
        return siteRepository.findSiteByUrl(urlWithWWW);
    }

    private int findCountPagesSite(searchengine.model.Site site) {
        return pageRepository.countPagesBySite(site);
    }

    private int findCountLemmasSite(searchengine.model.Site site) {
        return lemmaRepository.countBySite(site);
    }
}

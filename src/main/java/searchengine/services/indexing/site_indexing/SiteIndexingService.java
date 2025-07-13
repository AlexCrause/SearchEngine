package searchengine.services.indexing.site_indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;

import java.net.MalformedURLException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SiteIndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Transactional
    void writeToDb(String startUrl, String name) throws MalformedURLException {
        String normalizedWithWWW = UrlUtils.normalizeUrlWithWWW(startUrl);
        removeSiteData(normalizedWithWWW);

        Site site = new Site();
        site.setName(name);
        site.setUrl(normalizedWithWWW);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    @Transactional
    private void removeSiteData(String normalizedWithWWW) {
        pageRepository.deletePagesBySiteUrl(normalizedWithWWW);
        siteRepository.deleteSiteByUrl(normalizedWithWWW);
    }

    @Transactional
    void updateSiteStatusTime(String domain) throws MalformedURLException {

        String urlWithWWW = UrlUtils.normalizeUrlWithWWW(domain);

//        if (siteRepository.existsByUrl(urlWithWWW)) {
//            siteRepository.updateStatusTimeByUrl(urlWithWWW);
//        }
        siteRepository.updateStatusTimeByUrl(urlWithWWW);

    }

    @Transactional
    public void updateSiteStatus(String baseHost) throws MalformedURLException {
        String urlWithWWW = UrlUtils.normalizeUrlWithWWW(baseHost);
        siteRepository.updateStatusByUrl(urlWithWWW, Status.INDEXED);
    }

    @Transactional
    public void updateSiteLastError() {
        for (Site site : siteRepository.findAll()) {
            site.setStatus(Status.FAILED);
            site.setStatusTime(new Date());
            site.setLastError("Индексация остановлена пользователем");
            siteRepository.save(site);
        }
    }
}

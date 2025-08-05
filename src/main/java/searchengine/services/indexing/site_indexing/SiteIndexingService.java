package searchengine.services.indexing.site_indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteIndexingService {

    private final SiteRepository siteRepository;

    @Transactional
    void writeToDb(String normalizedUrl, String name) {
        removeSiteData(normalizedUrl);

        Site site = new Site();
        site.setName(name);
        site.setUrl(normalizedUrl);
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    @Transactional
    private void removeSiteData(String normalizedUrl) {
        Optional<Site> siteOptional = siteRepository.findSiteByUrl(normalizedUrl);
        if (siteOptional.isPresent()) {
            Site site = siteOptional.get();
            siteRepository.delete(site);
        }
    }

    @Transactional
    void updateSiteStatusTime(String siteUrl) throws MalformedURLException {
        siteRepository.updateStatusTimeByUrl(siteUrl);
    }

    @Transactional
    public void updateSiteStatus(String baseHost) {
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

    public void writeMistakeToDb(String url, String message) {
        Optional<Site> siteByUrl = siteRepository.findSiteByUrl(url);
        if (siteByUrl.isPresent()) {
            Site site = siteByUrl.get();
            site.setLastError(message);
            siteRepository.save(site);
        }
    }
}

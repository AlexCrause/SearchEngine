package searchengine.services.indexing.page_indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.UrlUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageIndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Transactional
    public void findSiteIdAndSavePages(String url,
                                       Document doc,
                                       int statusCode) {
        try {
            System.out.println("Сохранение страницы: " + url);
            String siteUrlWithWWW = UrlUtils.normalizeUrlWithWWW(url);
            System.out.println("siteUrlWithWWW: " + siteUrlWithWWW);
            String siteUrl = UrlUtils.getSiteUrl(siteUrlWithWWW);
            Optional<Site> siteOpt = siteRepository.findSiteByUrl(siteUrl);
            if (siteOpt.isEmpty()) return;

            String path = new URI(url).getPath();
            Optional<Page> pageOpt = pageRepository.findPageByPathAndSiteId(path, siteOpt.get());
            if (pageOpt.isPresent()) {
                return;
            }

            Page page = new Page();
            page.setSiteId(siteOpt.get());
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(UrlUtils.cleanContent(doc.html()));

            pageRepository.save(page);

        } catch (Exception e) {
            System.err.println("Ошибка при сохранении страницы: " + url + " | " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void saveHTMLPage(String siteUrl,
                             String pageUrl,
                             Document doc,
                             int statusCode) throws MalformedURLException, URISyntaxException {
        String urlWithWWW = UrlUtils.normalizeUrlWithWWW(siteUrl);
        Optional<Site> siteByUrl = siteRepository.findSiteByUrl(urlWithWWW);
        if (siteByUrl.isEmpty()) return;
        String path = new URI(pageUrl).getPath();
        Optional<Page> pageOptional = pageRepository.findPageByPath(path);

        if (pageOptional.isEmpty()) {
            Page page = new Page();
            page.setSiteId(siteByUrl.get());
            page.setContent(doc.html());
            page.setPath(path);
            page.setCode(statusCode);
            pageRepository.save(page);
        } else {
            Page page = pageOptional.get();
            page.setContent(doc.html());
            pageRepository.save(page);
        }
    }
}

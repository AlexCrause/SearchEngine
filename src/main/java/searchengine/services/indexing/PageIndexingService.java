package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

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
    void findSiteIdAndSavePages(String url,
                                Document doc,
                                String domain,
                                int statusCode) {
        try {
            String siteUrlWithWWW = UrlUtils.normalizeUrlWithWWW(domain);
            Optional<Site> siteOpt = siteRepository.findSiteByUrl(siteUrlWithWWW);
            if (siteOpt.isEmpty()) return;

            String path = new URI(url).getPath();
            System.out.println("path: " + path);
            Optional<String> pathOpt = pageRepository.findPathByPath(path);
            System.out.println(pathOpt);
            if (pathOpt.isPresent()) {
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
    void saveHTMLPage(String url,
                      Document doc) throws MalformedURLException, URISyntaxException {

        String path = new URI(url).getPath();

        Optional<Page> pageOptional = pageRepository.findPageByPath(path);
        if (pageOptional.isEmpty()) {
            return;
        }

        Page page = pageOptional.get();
        page.setContent(doc.html());

        pageRepository.save(page);
    }
}

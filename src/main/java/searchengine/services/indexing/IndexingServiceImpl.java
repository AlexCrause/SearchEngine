package searchengine.services.indexing;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception.IndexingException;
import searchengine.services.indexing.lemma_indexing.LemmaIndexingService;
import searchengine.services.indexing.page_indexing.IndexingPage;
import searchengine.services.indexing.page_indexing.PageIndexingService;
import searchengine.services.indexing.site_indexing.SiteCrawler;
import searchengine.services.indexing.site_indexing.SiteIndexingService;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.*;


@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteIndexingService siteIndexingService;
    private final PageIndexingService pageIndexingService;
    private final LemmaIndexingService lemmaIndexingService;
    private boolean isIndexing = false;
    private final ExecutorService executor;
    private final Map<String, ForkJoinTask<?>> crawlingTasks = new ConcurrentHashMap<>();
    private ForkJoinPool forkJoinPool;


    public IndexingServiceImpl(SitesList sites,
                               SiteIndexingService siteIndexingService,
                               PageIndexingService pageIndexingService,
                               LemmaIndexingService lemmaIndexingService) {
        this.sites = sites;
        this.siteIndexingService = siteIndexingService;
        this.pageIndexingService = pageIndexingService;
        this.lemmaIndexingService = lemmaIndexingService;
        executor = Executors.newFixedThreadPool(sites.getSites().size());
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public IndexingResponse startIndexing() {

        if (isIndexing) {
            throw new IndexingException("Индексация уже запущена");
        }

        isIndexing = true;
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        crawlingTasks.clear();

        for (Site site : sites.getSites()) {
            String url = site.getUrl();
            String name = site.getName();
            System.out.println("Передаю url сайта в обработчик сайтов: " + url);

            executor.execute(() -> {
                try {
                    ForkJoinTask<?> task = SiteCrawler.crawSite(url, name, siteIndexingService,
                            pageIndexingService, crawlingTasks, forkJoinPool, lemmaIndexingService);
                    crawlingTasks.put(url, task);
                    if (task.isDone()) {
                        System.out.println("Сайт успешно проиндексирован");
                        siteIndexingService.updateSiteStatus(url);
                        isIndexing = false;
                        stopForkJoinPool();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при индексации сайта " + url + ": " + e.getMessage());
                }
            });
        }
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {

        if (!isIndexing) {
            throw new IndexingException("Индексация не запущена");
        }

        isIndexing = false;

        for (ForkJoinTask<?> task : crawlingTasks.values()) {
            if (!task.isDone()) {
                task.cancel(true);
            }
        }
        crawlingTasks.clear();
        //stopForkJoinPool();
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
            System.out.println("ForkJoinPool shutdownNow");
        }
        siteIndexingService.updateSiteLastError();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse indexPage(String urlPage) {

        Site targetSite = null;

        for (Site site : sites.getSites()) {
            String urlSite = site.getUrl();
            try {
                String domainHost = UrlUtils.getDomainHost(urlSite);
                System.out.println("domainHost: " + domainHost);
                System.out.println("urlPage: " + urlPage);
                if (urlPage.contains(domainHost)) {
                    targetSite = site;
                    break;
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if (targetSite != null){
            try {
                Site finalTargetSite = targetSite;
                executor.execute(() -> IndexingPage.indexPage(urlPage, finalTargetSite.getUrl(),
                        pageIndexingService,
                        lemmaIndexingService));
                IndexingResponse response = new IndexingResponse();
                response.setResult(true);
                return response;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IndexingException("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }
    }

    private void stopForkJoinPool() {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            System.out.println("ForkJoinPool shutdown");
        }
    }
}

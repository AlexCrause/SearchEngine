package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseError;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.*;


@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteIndexingService siteIndexingService;
    private final PageIndexingService pageIndexingService;
    private final LemmaIndexingService lemmaIndexingService;
    private final IndexIndexingService indexIndexingService;
    private volatile boolean isIndexing = false;
    private final ExecutorService executor;
    private final Map<String, ForkJoinTask<?>> crawlingTasks = new ConcurrentHashMap<>();
    private ForkJoinPool forkJoinPool;


    public IndexingServiceImpl(SitesList sites,
                               SiteIndexingService siteIndexingService,
                               PageIndexingService pageIndexingService,
                               LemmaIndexingService lemmaIndexingService,
                               IndexIndexingService indexIndexingService) {
        this.sites = sites;
        this.siteIndexingService = siteIndexingService;
        this.pageIndexingService = pageIndexingService;
        this.lemmaIndexingService = lemmaIndexingService;
        this.indexIndexingService = indexIndexingService;
        executor = Executors.newFixedThreadPool(sites.getSites().size());
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public synchronized ResponseEntity<?> startIndexing() {

        if (isIndexing) {
            IndexingResponseError error = new IndexingResponseError();
            error.setResult(false);
            error.setError("Индексация уже запущена");
            return ResponseEntity.ok(error);
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
                            pageIndexingService, crawlingTasks, forkJoinPool);
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
        return ResponseEntity.ok(response);
    }

    @Override
    public synchronized ResponseEntity<?> stopIndexing() {

        if (!isIndexing) {
            IndexingResponseError error = new IndexingResponseError();
            error.setResult(false);
            error.setError("Индексация не запущена");
            return ResponseEntity.ok(error);
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
        return ResponseEntity.ok(response);
    }

    @Override
    public synchronized ResponseEntity<?> indexPage(String urlPage) {

        for (Site site : sites.getSites()) {
            String urlSite = site.getUrl();
            try {
                String domainHost = UrlUtils.getDomainHost(urlSite);
                System.out.println("domainHost: " + domainHost);
                System.out.println("urlPage: " + urlPage);
                if (urlPage.contains(domainHost)) {

                    IndexingPage.indexPage(urlPage, urlSite,
                            pageIndexingService, siteIndexingService,
                            lemmaIndexingService, indexIndexingService);
                    IndexingResponse response = new IndexingResponse();
                    response.setResult(true);
                    return ResponseEntity.ok(response);
                }
                IndexingResponseError error = new IndexingResponseError();
                error.setResult(false);
                error.setError("Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
                return ResponseEntity.ok(error);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void stopForkJoinPool() {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            System.out.println("ForkJoinPool shutdown");
        }
    }
}

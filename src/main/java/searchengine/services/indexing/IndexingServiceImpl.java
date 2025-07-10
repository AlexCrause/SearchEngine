package searchengine.services.indexing;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseError;

import java.util.Map;
import java.util.concurrent.*;


@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteIndexingService siteIndexingService;
    private final PageIndexingService pageIndexingService;
    private volatile boolean isIndexing = false;
    private final ExecutorService executor;
    private final Map<String, ForkJoinTask<?>> crawlingTasks = new ConcurrentHashMap<>();
    private ForkJoinPool forkJoinPool;


    public IndexingServiceImpl(SitesList sites,
                               SiteIndexingService siteIndexingService,
                               PageIndexingService pageIndexingService) {
        this.sites = sites;
        this.siteIndexingService = siteIndexingService;
        this.pageIndexingService = pageIndexingService;
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
    public ResponseEntity<?> indexPage() {
        return null;
    }

    private void stopForkJoinPool() {
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            System.out.println("ForkJoinPool shutdown");
        }
    }


}

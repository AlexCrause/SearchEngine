package searchengine.services.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.AppConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;


public class SiteCrawler extends RecursiveTask<Set<String>> {

    private final String url;
    private final String domainHost;
    private final int depth;
    private final int maxDepth;
    private final Set<String> visitedUrls;
    private final PageIndexingService pageIndexingService;
    private final SiteIndexingService siteIndexingService;

    public SiteCrawler(String url,
                       String domainHost,
                       int depth,
                       int maxDepth,
                       Set<String> visitedUrls,
                       PageIndexingService pageIndexingService,
                       SiteIndexingService siteIndexingService) {
        this.url = url;
        this.domainHost = domainHost;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.visitedUrls = visitedUrls;
        this.pageIndexingService = pageIndexingService;
        this.siteIndexingService = siteIndexingService;
    }

    public static ForkJoinTask<?> crawSite(String startUrl,
                                           String name,
                                           SiteIndexingService siteIndexingService,
                                           PageIndexingService pageIndexingService,
                                           Map<String, ForkJoinTask<?>> tasksMap,
                                           ForkJoinPool fjpool) {

        int maxDepth = 1;
        ForkJoinTask<Set<String>> task = null;
        try {
            String normalizedStartUrl = UrlUtils.normalizeUrl(startUrl); //с www.
            String baseHost = UrlUtils.getDomainHost(normalizedStartUrl); //без www.

            siteIndexingService.writeToDb(normalizedStartUrl, name);

            Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

            task = fjpool.submit(new SiteCrawler(normalizedStartUrl, baseHost,
                    0, maxDepth, visitedUrls, pageIndexingService, siteIndexingService));
            tasksMap.put(baseHost, task);
            task.get(2, TimeUnit.HOURS);

        } catch (TimeoutException e) {
            System.err.println("Превышено время индексации сайта: " + startUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Неправильный URL: "
                    + startUrl + " | " + e.getMessage());
        }
        return task;
    }


    @Override
    protected Set<String> compute() {

        if (isCancelled() || Thread.currentThread().isInterrupted()) return Collections.emptySet();

        ForkJoinPool pool = ForkJoinTask.getPool();
        System.out.printf("Task Count: %d\n", pool.getQueuedTaskCount());

        if (depth > maxDepth || !visitedUrls.add(url)) {
            return Collections.emptySet();
        }

        try {
            Thread.sleep(1000);
            if (isCancelled() || Thread.currentThread().isInterrupted()) return Collections.emptySet();
            Connection.Response response = connectToUrl();
            if (isCancelled() || Thread.currentThread().isInterrupted()) return Collections.emptySet();

            int statusCode = response.statusCode();
            Document doc = response.parse();
            Elements links = doc.select("a[href]");

            siteIndexingService.updateSiteStatusTime(domainHost);
            pageIndexingService.findSiteIdAndSavePages(url, doc, domainHost, statusCode);

            if (isCancelled() || Thread.currentThread().isInterrupted()) return Collections.emptySet();
            parseDocument(links);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptySet();
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) return Collections.emptySet();
            System.err.println("Ошибка при загрузке: " + url + " | " + e.getMessage());
        }
        System.out.println("Всего URL после обхода: " + visitedUrls.size());
        return visitedUrls;
    }

    private Connection.Response connectToUrl() throws IOException {

        System.out.println("Загрузка: " + url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(AppConfig.getUserAgent())
                .referrer(AppConfig.getReferrer())
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .timeout(10_000)
                .execute();
        System.out.println("Успешно: " + url);
        return response;
    }

    private void parseDocument(Elements links) throws MalformedURLException {

        List<SiteCrawler> subTasks = new ArrayList<>();

        for (Element link : links) {
            String nextUrl = UrlUtils.normalizeUrl(link.absUrl("href"));
            if (nextUrl.isBlank()) continue;
            if (nextUrl.matches("(?i).*\\.(pdf|jpg|jpeg|png|css|js|zip|webp)$")) continue;
            if (!nextUrl.startsWith(new URL(url).getProtocol())) continue;

            String linkHost = UrlUtils.getDomainHost(nextUrl);
            if (!linkHost.equals(domainHost)) continue;

            subTasks.add(new SiteCrawler(nextUrl, domainHost, depth + 1,
                    maxDepth, visitedUrls, pageIndexingService, siteIndexingService));
        }
        recursiveInvokeAll(subTasks);
    }

    private void recursiveInvokeAll(List<SiteCrawler> subTasks) {

        if (isCancelled() || Thread.currentThread().isInterrupted()) return;

        Collection<SiteCrawler> tasks = ForkJoinTask.invokeAll(subTasks);
        for (SiteCrawler task : tasks) {
            if (task.isCompletedNormally()) {
                visitedUrls.addAll(task.join());
            }
        }
    }
}

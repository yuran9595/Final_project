package searchengine.services.impl;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class WebCrawler extends RecursiveAction {
    private String url;
    private SiteEntity siteEntity;
    public static volatile ConcurrentHashMap<String, PageEntity> pageSetLinks = new ConcurrentHashMap<>();
    public static volatile Set<String> urls = new HashSet<>();
    public static String agentUser = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
    public static String referrer = "http://www.google.com";


    public WebCrawler(String url, SiteEntity siteEntity) {
        this.url = url;
        this.siteEntity = siteEntity;
    }

    @Override
    protected void compute() {
        processPageLinks(url);
    }

    private void processPageLinks(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(0).userAgent(agentUser).referrer(referrer).get();
            Elements elementLines = doc.getElementsByTag("a");
            if (elementLines.size() == 0) {
                return;
            }
            List<WebCrawler> tasks = elementLines.stream()
                    .map(element -> element.attr("href"))
                    .filter(href -> href.startsWith("/") && !urls.contains(siteEntity.getUrl() + href))
                    .map(href -> {
                        urls.add(siteEntity.getUrl() + href);
                        return new WebCrawler(siteEntity.getUrl() + href, siteEntity);
                    })
                    .collect(Collectors.toList());

            ForkJoinTask.invokeAll(tasks);

            PageEntity page;
            for (Element element : elementLines) {
                String href = element.attr("href");
                if (!IndexingServiceImpl.isRun) {
                    break;
                }
                if (pageSetLinks.containsKey(siteEntity.getUrl() + href)) {
                    continue;
                }
                if (href.startsWith("/")) {
                    page = new PageEntity();
                    page.setSite(siteEntity);
                    page.setPath(href);
                    page.setContent(doc.toString());
                    page.setCode(doc.connection().response().statusCode());
                    pageSetLinks.put(siteEntity.getUrl() + href, page);
                    urls.add(siteEntity.getUrl() + href);
                    siteEntity.getPages().add(page);
                    System.out.println(siteEntity.getUrl() + href + " страница для вывода " + pageSetLinks.size() + " - размер  " + Thread.currentThread().getName() + " " + Thread.activeCount());
                    processPageLinks(siteEntity.getUrl() + href);
                }
            }
        } catch (UnsupportedMimeTypeException exp) {
            siteEntity.setLastError(String.valueOf(exp));
        } catch (HttpStatusException ex) {
            siteEntity.setLastError(String.valueOf(ex));
        } catch (IOException e) {
            siteEntity.setLastError(String.valueOf(e));
            // throw new RuntimeException(e);
        } catch (Exception exception) {
            siteEntity.setLastError(String.valueOf(exception));
        }
    }
}


package searchengine.services.impl;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

public class WebCrawler extends RecursiveAction {
    private String url;
    private SiteEntity siteEntity;
    @Autowired
    private SiteRepository siteRepository;
    public static volatile ConcurrentHashMap<String, PageEntity> pageSetLinks = new ConcurrentHashMap<>();

    public WebCrawler(String url, SiteEntity siteEntity) {
        this.url = url;
        this.siteEntity = siteEntity;
    }

    @Override
    protected void compute() {
        String agentUser = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";
        String referrer = "http://www.google.com";
        PageEntity page = null;
        Document doc = null;
        try {
            doc = Jsoup.connect(url).userAgent(agentUser).referrer(referrer).get();
            Elements elementLines = doc.getElementsByTag("a");
            if (elementLines.size() == 0) {
                return;
            }
            processPageLinks(doc, elementLines);
        } catch (UnsupportedMimeTypeException exp) {
            setLastErrorToSite(siteEntity.getId(), String.valueOf(exp));
            System.out.println(" UnsupportedMimeTypeException ");
        } catch (HttpStatusException ex) {
            System.out.println("HttpStatusException");
        } catch (IOException e) {
            System.out.println("RunTime error");
            throw new RuntimeException(e);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    private void processPageLinks(Document doc, Elements elementLines) {
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
                siteEntity.getPages().add(page);
                System.out.println(siteEntity.getUrl() + href + " страница для вывода " + pageSetLinks.size() + " - размер");
                WebCrawler forkJoin = new WebCrawler(siteEntity.getUrl() + href, siteEntity);
                forkJoin.fork();
                forkJoin.join();
            }
        }
    }
    private void setLastErrorToSite(Integer siteId, String lastError) {
        SiteEntity byId = siteRepository.findById(siteId).orElse(null);
        if (byId != null) {
            byId.setLastError(lastError);
            siteRepository.save(byId);
        }
    }
}


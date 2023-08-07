package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.threads.MyThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    @Value("${user-agent}")
    private String agentUser;
    @Value("${referrer}")
    private String referrer;

    @Override
    public void indexing() {
        deleteAllInformationFromSite();
        List<Site> sites = sitesList.getSites();
        for (Site site : sites) {
            MyThread myThread = new MyThread(site, siteRepository, pageRepository);
            myThread.start();
        }
    }
    @Transactional
    private void deleteAllInformationFromSite() {
        indexRepository.deleteAll();
        siteRepository.deleteAll();
    }
    private List<PageEntity> getPageName(SiteEntity siteEntity) {
        List<PageEntity> result = new ArrayList<>();
        PageEntity page;
        int count = 0;
        int count1 = 0;
        try {
            Document doc = Jsoup.connect(siteEntity.getUrl()).userAgent(agentUser)
                    .referrer(referrer).get();
            Elements elementLines = doc.getElementsByTag("a");
            System.out.println(elementLines.size() + " -  Кол-во элементов ");
            for (Element element : elementLines) {
                String href = element.attr("href");
                count++;
                System.out.println(href + " - полученная страница " + count);
                System.out.println("кол-во полученных страниц - " + count);
                if (href.startsWith("/") || href.startsWith("http")) {
                    page = new PageEntity();
                    page.setSite(siteEntity);
                    page.setPath(href);
                    if (page.getPath().startsWith("http")) {
                        href = page.getPath();
                    } else {
                        href = siteEntity.getUrl() + page.getPath();
                    }
                    try {
                        Document pageDoc = Jsoup.connect(href).userAgent(agentUser)
                                .referrer(referrer).get();
                        String stringContent = pageDoc.toString();

                        page.setContent(stringContent);
                        page.setCode(pageDoc.connection().response().statusCode());
                        if (pageRepository.findByPathAndSiteId(page.getPath(), page.getSite().getId()).isEmpty()) {
                            result.add(page);
                            count1++;
                            System.out.println(page.getPath() + " - сохраненная страница" + count1);
                            System.out.println("кол-во сохраненных страниц - " + count1);
                        }
                    } catch (UnsupportedMimeTypeException exp) {
                        setLastErrorToSite(siteEntity.getId(), String.valueOf(exp));
                    } catch (HttpStatusException ex) {
                        page.setCode(ex.getStatusCode());
                        page.setContent("No content");
                        pageRepository.save(page);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    private void setLastErrorToSite(Integer siteId, String lastError) {
        SiteEntity byId = siteRepository.findById(siteId).orElse(null);
        if (byId != null) {
            byId.setLastError(lastError);
            siteRepository.save(byId);
        }
    }
}

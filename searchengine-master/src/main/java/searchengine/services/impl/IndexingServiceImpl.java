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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.enums.Status;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    @Value("${user-agent}")
    private String agentUser;
    @Value("${referrer}")
    private String referrer;


    @Override
    public void indexing() {
        deleteAllInformationFromSite();
        List<Site> sites = sitesList.getSites();
        for (Site site : sites) {
            try {
                getPageName(site);

                setStatus(site, Status.INDEXED);
            }catch (Exception e){
                setStatus(site, Status.FAILED);
            }
        }
    }

    private void deleteAllInformationFromSite() {
        siteRepository.deleteAll();
    }

    private void getPageName(Site site) {
        SiteEntity siteEntity = setStatus(site, Status.INDEXING);
        PageEntity page;
        int count=0;
        int count1 = 0;
        try {
            Document doc = Jsoup.connect(site.getUrl()).userAgent(agentUser)
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
                    }else {
                        href = site.getUrl()+page.getPath();
                    }
                    try {
                        Document pageDoc = Jsoup.connect(href).userAgent(agentUser)
                                .referrer(referrer).get();
                        page.setContent(pageDoc.toString());
                        page.setCode(pageDoc.connection().response().statusCode());
                        if (pageRepository.findByPathAndSiteId(page.getPath(), page.getSite().getId()).isEmpty()) {
                            pageRepository.save(page);
                            count1++;
                            System.out.println(page.getPath() + " - сохраненная страница" + count1);
                            System.out.println("кол-во сохраненных страниц - " + count1);
                        }
                    } catch (UnsupportedMimeTypeException exp){
                        setLastErrorToSite(siteEntity.getId(), String.valueOf(exp));
                    } catch (HttpStatusException ex){
                        page.setCode(ex.getStatusCode());
                        page.setContent("No content");
                        pageRepository.save(page);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private SiteEntity setStatus(Site site, Status status) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteRepository.save(siteEntity);
    }

//    private void deleteSiteInformation(Site site) {
//        Optional<SiteEntity> byUrl = siteRepository.findByUrl(site.getUrl());
//
//        if (byUrl.isPresent()) {
//            siteRepository.delete(byUrl.get());
//            log.info("Удалены записи по сайту - " + byUrl.get().getUrl());
//        }
//    }
    private void setLastErrorToSite(Integer siteId, String lastError){
        SiteEntity byId = siteRepository.findById(siteId).orElse(null);
        if (byId!=null){
            byId.setLastError(lastError);
            siteRepository.save(byId);
        }
    }
}

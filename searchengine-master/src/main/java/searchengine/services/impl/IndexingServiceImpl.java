package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.enums.Status;
import searchengine.models.IndexEntity;
import searchengine.models.LemmaEntity;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    @Value("${user-agent}")
    private String agentUser;
    @Value("${referrer}")
    private String referrer;


    @Override
    public void indexing() {
        deleteAllInformationFromSite();
        List<Site> sites = sitesList.getSites();
        for (Site site : sites) {
            SiteEntity siteEntity = transformSiteToSiteEntity(site);
            try {
                List<PageEntity> pageNames = getPageName(site);
                siteEntity.setPages(pageNames);
                parseLemmas(siteEntity);
                saveSiteAndSetSiteStatus(siteEntity, Status.INDEXED);
            } catch (Exception e) {
                saveSiteAndSetSiteStatus(siteEntity, Status.FAILED);
            }
        }
    }

    private void deleteAllInformationFromSite() {
        siteRepository.deleteAll();
    }

    private List<PageEntity> getPageName(Site site) {
        List<PageEntity> result = new ArrayList<>();
//        LuceneMorphology luceneMorph;
//        try {
//            luceneMorph = new RussianLuceneMorphology();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        LemmaService lemmaService = new LemmaService(luceneMorph);
        SiteEntity entity = transformSiteToSiteEntity(site);
        SiteEntity siteEntity = saveSiteAndSetSiteStatus(entity, Status.INDEXING);
        PageEntity page;
        int count = 0;
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
                    } else {
                        href = site.getUrl() + page.getPath();
                    }
                    try {
                        Document pageDoc = Jsoup.connect(href).userAgent(agentUser)
                                .referrer(referrer).get();
                        String stringContent = pageDoc.toString();

                        page.setContent(stringContent);
                        page.setCode(pageDoc.connection().response().statusCode());
                        if (pageRepository.findByPathAndSiteId(page.getPath(), page.getSite().getId()).isEmpty()) {
                            // PageEntity save = pageRepository.save(page);
                            result.add(page);
                            //  String resultReg = stringContent.replaceAll("<[^>]*>", "");
                            // System.out.println(result);
//                               Map<String, Integer> map = lemmaService.collectLemmas(resultReg);
                            //  parseLemmas(map, siteEntity, page); // - парсинг лемас
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

    private SiteEntity saveSiteAndSetSiteStatus(SiteEntity siteEntity, Status status) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteRepository.save(siteEntity);
    }

    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        return siteEntity;
    }

    //    private void deleteSiteInformation(Site site) {
//        Optional<SiteEntity> byUrl = siteRepository.findByUrl(site.getUrl());
//
//        if (byUrl.isPresent()) {
//            siteRepository.delete(byUrl.get());
//            log.info("Удалены записи по сайту - " + byUrl.get().getUrl());
//        }
//    }
    private void setLastErrorToSite(Integer siteId, String lastError) {
        SiteEntity byId = siteRepository.findById(siteId).orElse(null);
        if (byId != null) {
            byId.setLastError(lastError);
            siteRepository.save(byId);
        }
    }

    private void parseLemmas(SiteEntity site) {
        LuceneMorphology luceneMorph;
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LemmaService lemmaService = new LemmaService(luceneMorph);
        List<LemmaEntity> lemmas = site.getLemmas();
        Map<String, Integer> lemmaListToMap = lemmas
                .stream()
                .collect(Collectors.toMap(LemmaEntity::getLemma, LemmaEntity::getFrequency));
        for (PageEntity page : site.getPages()) {
            String stringContent = page.getContent();
            String resultReg = stringContent.replaceAll("<[^>]*>", "");
            Map<String, Integer> map = lemmaService.collectLemmas(resultReg);
            List<IndexEntity> indexEntityList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (lemmaListToMap.containsKey(entry.getKey())) {
                    lemmaListToMap.put(entry.getKey(), lemmaListToMap.get(entry.getKey()) + 1);
                } else {
                    lemmaListToMap.put(entry.getKey(), 1);
                }
                LemmaEntity lemmaEntity = new LemmaEntity();
                lemmaEntity.setLemma(entry.getKey());
                lemmaEntity.setFrequency(entry.getValue());
                lemmaEntity.setSite(site);
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(lemmaEntity);
                indexEntity.setPage(page);
                indexEntity.setRank(entry.getValue().floatValue());
                lemmaEntity.getIndexes().add(indexEntity);
                lemmaEntities.add(lemmaEntity);
                page.getIndexes().add(indexEntity);
            }
            System.out.println();
        }
        System.out.println();
//        for (Map.Entry<String, Integer> entry : lemmaListToMap.entrySet()) {
//            LemmaEntity lemmaEntity = new LemmaEntity();
//            lemmaEntity.setLemma(entry.getKey());
//            lemmaEntity.setFrequency(entry.getValue());
//            lemmaEntity.setSite(site);
//            lemmaEntities.add(lemmaEntity);
//            //todo:  lemmaEntity.setIndexes(); Лист IndexEntity где его взять
//        }
        site.setLemmas(lemmaEntities);
    }
}

package searchengine.services.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.config.Site;
import searchengine.enums.Status;
import searchengine.models.IndexEntity;
import searchengine.models.LemmaEntity;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;


public class SiteIndexerThread extends Thread {
    private Site site;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;

    public SiteIndexerThread(Site site, SiteRepository siteRepository, PageRepository pageRepository) {
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }
    @Override
    public void run() {
        SiteEntity siteEntity = transformSiteToSiteEntity(site);
        try {
            saveSiteAndSetSiteStatus(siteEntity, Status.INDEXING);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            WebCrawler forkJoin = new WebCrawler(siteEntity.getUrl(), siteEntity);
            forkJoinPool.invoke(forkJoin);
            forkJoinPool.shutdown();
            if (!siteRepository.findById(siteEntity.getId()).get().getStatus().equals(Status.FORCED_STOP)) {
                parseLemmas(siteEntity);
                saveSiteAndSetSiteStatus(siteEntity, Status.INDEXED);
            }
        } catch (Exception e) {
            saveSiteAndSetSiteStatus(siteEntity, Status.FAILED);
        }
    }
    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        return siteEntity;
    }
    private SiteEntity saveSiteAndSetSiteStatus(SiteEntity siteEntity, Status status) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteRepository.save(siteEntity);
    }
    private void parseLemmas(SiteEntity site) {
        LuceneMorphology luceneMorph;
        Set<LemmaEntity> lemmaEntities = new HashSet<>();
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LemmaService lemmaService = new LemmaService(luceneMorph);
        Set<LemmaEntity> lemmas = site.getLemmas();
        Map<String, LemmaEntity> lemmaListToMap = lemmas
                .stream()
                .collect(Collectors.toMap(LemmaEntity::getLemma, Function.identity()));
        for (PageEntity page : site.getPages()) {
            String stringContent = page.getContent();
            String resultReg = stringContent.replaceAll("<[^>]*>", "");
            Map<String, Integer> map = lemmaService.collectLemmas(resultReg);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                LemmaEntity lemmaEntity = new LemmaEntity();
                if (lemmaListToMap.containsKey(entry.getKey())) {
                    lemmaEntity = lemmaListToMap.get(entry.getKey());
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                    lemmaListToMap.put(entry.getKey(), lemmaEntity);
                } else {
                    lemmaEntity.setLemma(entry.getKey());
                    lemmaEntity.setFrequency(entry.getValue());
                    lemmaEntity.setSite(site);
                    lemmaListToMap.put(entry.getKey(), lemmaEntity);
                }
                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(lemmaEntity);
                indexEntity.setPage(page);
                indexEntity.setRank(entry.getValue().floatValue());
                lemmaEntity.getIndexes().add(indexEntity);
                lemmaEntities.add(lemmaEntity);
                page.getIndexes().add(indexEntity);
            }
        }
        site.setLemmas(lemmaEntities);
    }
}

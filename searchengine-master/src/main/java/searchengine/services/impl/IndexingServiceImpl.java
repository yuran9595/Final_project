package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.enums.Status;
import searchengine.models.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.threads.ForkJoinImpl;
import searchengine.threads.SiteThreadImpl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    public static volatile boolean isRun = false;

    @Value("${user-agent}")
    private String agentUser;
    @Value("${referrer}")
    private String referrer;

    @Override
    public void indexing() {
        ForkJoinImpl.pageSetLinks = new ConcurrentHashMap<>();
        deleteAllInformationFromSite();
        List<Site> sites = sitesList.getSites();
        List<String> siteUrl = sites.stream()
                .map(site -> site.getUrl()).toList();
        List<SiteEntity> sitesFromBase = siteRepository.findAll();
        for (SiteEntity siteFromBase : sitesFromBase) {
            if (!siteUrl.contains(siteFromBase.getUrl())) {
                sites.add(new Site(siteFromBase.getUrl(), siteFromBase.getName()));
            }
        }
        int count = sites.size();
        SiteThreadImpl[] threads = new SiteThreadImpl[count];
        isRun = true;
        try {
            for (int i = 0; i < count; i++) {
                threads[i] = new SiteThreadImpl(sites.get(i), siteRepository, pageRepository);
                threads[i].start();
            }
            for (SiteThreadImpl thread : threads) {
                thread.join();
            }
            isRun = false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Transactional
    private void deleteAllInformationFromSite() {
        indexRepository.deleteFromIndex();
        pageRepository.deleteFromPage();
        lemmaRepository.deleteFromLemma();
        siteRepository.deleteFromSite();

    }

    @Override
    public void stopIndexing() {
        isRun = false;
        for (SiteEntity siteEntity : siteRepository.findAll()) {
            try {
                if (siteEntity.getStatus().equals(Status.INDEXING)) {
                    siteEntity.setStatus(Status.FORCED_STOP);
                    siteRepository.save(siteEntity);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage() + siteEntity.getUrl());
            }
        }
    }
}

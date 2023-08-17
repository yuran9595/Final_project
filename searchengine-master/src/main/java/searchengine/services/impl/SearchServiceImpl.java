package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.SearchDTO;
import searchengine.models.LemmaEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public SearchDTO searching(String query, int offset, int limit, String site) {
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LemmaService lemmaService = new LemmaService(luceneMorph);
        Set<String> lemmasFromQuery = lemmaService.collectLemmas(query).keySet();
        List<Site> sites = sitesList.getSites();
        List<SiteEntity> result = new ArrayList<>();
        if (!site.equals("All sites")) {
            sites.stream()
                    .filter(site1 -> site1.equals(site))
                    .map(this::transformSiteToSiteEntity)
                    .collect(Collectors.toList());
        } else {
            result = sites.stream()
                    .map(this::transformSiteToSiteEntity)
                    .collect(Collectors.toList());
        }

        for (SiteEntity siteEntity : result) {
            List<LemmaEntity> lemmas = new ArrayList<>();
            for (String lemma : lemmasFromQuery) {
                System.out.println();
            }

        }
        return null;
    }

    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        return siteEntity;
    }
}

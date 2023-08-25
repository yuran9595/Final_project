package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.SearchDTO;
import searchengine.dto.SearchInfoDTO;
import searchengine.models.IndexEntity;
import searchengine.models.LemmaEntity;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageServiceImpl pageService;
    private final IndexRepository indexRepository;

    @Override
    public SearchDTO searching(String query, int offset, int limit, String site) {
        SearchDTO searchDTO = new SearchDTO();
        List<SearchInfoDTO> searchInfoDTOS = new ArrayList<>();
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LemmaService lemmaService = new LemmaService(luceneMorph);
        Set<String> lemmasFromQuery = lemmaService.collectLemmas(query).keySet();
        List<Site> sites = sitesList.getSites();
        List<SiteEntity> siteEntityList = new ArrayList<>();
        if (!site.equals("All sites")) {
            siteEntityList = sites.stream()
                    .filter(site1 -> site1.getUrl().equals(site))
                    .map(this::transformSiteToSiteEntity)
                    .collect(Collectors.toList());
        } else {
            siteEntityList = sites.stream()
                    .map(this::transformSiteToSiteEntity)
                    .collect(Collectors.toList());
        }
        for (SiteEntity siteEntity : siteEntityList) {
            List<LemmaEntity> lemmas = new ArrayList<>();
            for (String lemma : lemmasFromQuery) {
                if (lemmaRepository.findBySiteAndLemma(siteEntity, lemma).isPresent()) {
                    lemmas.add(lemmaRepository.findBySiteAndLemma(siteEntity, lemma).get());
                }
            }
            lemmas = lemmas.stream()
                    .sorted(Comparator.comparingLong(LemmaEntity::getFrequency))
                    .collect(Collectors.toList());
            Integer size = lemmas.size();
            Map<PageEntity, Integer> pageEntityIntegerMap = new HashMap<>();
            for (int i = 0; i < lemmas.size(); i++) {
                for (IndexEntity index : lemmas.get(i).getIndexes()) {
                    PageEntity page = index.getPage();
                    if (i == 0) {
                        pageEntityIntegerMap.put(page, 1);
                    } else {
                        if (pageEntityIntegerMap.containsKey(page)) {
                            pageEntityIntegerMap.put(page, pageEntityIntegerMap.get(page) + 1);
                        }
                    }
                }
            }
            System.out.println();
            List<PageEntity> collect = pageEntityIntegerMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(size))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            searchDTO.setResult(true);
            for (PageEntity page : collect) {
                SearchInfoDTO searchInfoDTO = new SearchInfoDTO();
                searchInfoDTO.setSite(siteEntity.getUrl());
                searchInfoDTO.setSiteName(siteEntity.getName());
                searchInfoDTO.setUri(page.getPath());
                searchInfoDTO.setTitle(pageService.getTitle(page.getContent()));
                String pageContent = page.getContent();
                int lemmaRef = 0;
                List<String> snippets = new ArrayList<>();
                Set<String> lemmaForms = new HashSet<>();
                StringBuilder snippet = new StringBuilder();
                String[] strings = lemmaService.arrayContainsRussianWords(pageContent);
                List<String> lemmaStringList = lemmas.stream()
                        .map(LemmaEntity::getLemma).toList();
                for (String normalForm : strings) {
                    String russianWordFromContent = normalForm.toLowerCase(Locale.ROOT);
                    String lemmaForm = luceneMorph.getNormalForms(russianWordFromContent).get(0);
                    if (!normalForm.isEmpty()) {
                        if (lemmaStringList.contains(lemmaForm)) {
                            normalForm = "<b>" + normalForm + "</b>";
                            snippets.add(normalForm);
                            lemmaForms.add(lemmaForm);
                            lemmaRef = snippets.size();
                        } else {
                            snippets.add(normalForm);
                        }
                    }
                }
                int count = 0;
                for (String element : lemmasFromQuery) {
                    if (lemmaForms.contains(element)) {
                        count++;
                    }
                }
                if (count == lemmasFromQuery.size()) {
                    if (lemmaRef > 0) {
                        int start = 0;
                        if (lemmaRef > 20) {
                            start = lemmaRef - 20;
                        }
                        int finish = 0;
                        if ((snippets.size() - lemmaRef) > 20) {
                            finish = lemmaRef + 20;
                        } else {
                            finish = snippets.size();
                        }
                        for (int i = start; i < finish; i++) {
                            snippet.append(snippets.get(i)).append(" ");
                        }
                    }
                    searchInfoDTO.setSnippet(snippet.toString());
                    searchInfoDTO.setRelevance(0.0);
                    //   int i = indexRepository.findRelevance("курс", 152L, 54076L);
                    searchInfoDTOS.add(searchInfoDTO);
                }
            }
            searchDTO.setData(searchInfoDTOS);
        }
        searchDTO.setCount(searchInfoDTOS.size());
        return searchDTO;
    }
    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        return siteEntity;
    }
}

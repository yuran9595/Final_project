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
    private Set<String> lemmasFromQuery = new HashSet<>();

    @Override
    public SearchDTO searching(String query, int offset, int limit, String site) {
        SearchDTO searchDTO = new SearchDTO();
        List<SearchInfoDTO> searchInfoDTOS = new ArrayList<>();
        LuceneMorphology luceneMorph = getLuceneMorphology();
        LemmaService lemmaService = new LemmaService(getLuceneMorphology());
        lemmasFromQuery = lemmaService.collectLemmas(query).keySet();
        List<Site> sites = sitesList.getSites();
        List<SiteEntity> siteEntityList = getSiteEntities(site, sites);
        searchFromSitesList(searchDTO, searchInfoDTOS, luceneMorph, lemmaService, siteEntityList);
        searchDTO.setCount(searchInfoDTOS.size());
        return searchDTO;
    }

    private void searchFromSitesList(SearchDTO searchDTO, List<SearchInfoDTO> searchInfoDTOS, LuceneMorphology luceneMorph, LemmaService lemmaService, List<SiteEntity> siteEntityList) {
        for (SiteEntity siteEntity : siteEntityList) {
            List<LemmaEntity> lemmas = new ArrayList<>();
            lemmas = getLemmaEntities(siteEntity, lemmas);
            Integer size = lemmas.size();
            Map<PageEntity, Integer> pageEntityIntegerMap = new HashMap<>();
            for (int i = 0; i < lemmas.size(); i++) {
                creationMapPageEntityWithCount(lemmas, pageEntityIntegerMap, i);
            }
            List<PageEntity> collect = pageEntityIntegerMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(size))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            searchDTO.setResult(true);
            int[] maxRelevance = getMaxRelevance(siteEntity, lemmas);
            searchForPage(searchInfoDTOS, luceneMorph, lemmaService, siteEntity, lemmas, collect, maxRelevance);
            searchDTO.setData(searchInfoDTOS);
        }
    }

    private int[] getMaxRelevance(SiteEntity siteEntity, List<LemmaEntity> lemmas) {
        int[] maxRelevance = new int[lemmas.size()];
        for (int i = 0; i < lemmas.size(); i++) {
            maxRelevance[i] = indexRepository.findMaxRelevance(lemmas.get(i).getLemma(), siteEntity.getId()).orElse(0);
        }
        return maxRelevance;
    }

    private void searchForPage(List<SearchInfoDTO> searchInfoDTOS, LuceneMorphology luceneMorph, LemmaService lemmaService, SiteEntity siteEntity, List<LemmaEntity> lemmas, List<PageEntity> collect, int[] maxRelevance) {
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
            List<String> lemmaStringList = lemmas.stream().map(LemmaEntity::getLemma).toList();
            lemmaRef = snippetCreation(luceneMorph, lemmaRef, snippets, lemmaForms, strings, lemmaStringList);
            int count = (int) lemmasFromQuery.stream().filter(lemmaForms::contains).count();
            if (count == lemmasFromQuery.size()) {
                snippetLengthCreation(lemmaRef, snippets, snippet);
                searchInfoDTO.setSnippet(snippet.toString());
                double relevance =  calculateRelevance(maxRelevance, getRelevanceForPage(siteEntity, lemmas, page));
                searchInfoDTO.setRelevance(relevance);
                searchInfoDTOS.add(searchInfoDTO);
            }
        }
    }

    private int[] getRelevanceForPage(SiteEntity siteEntity, List<LemmaEntity> lemmas, PageEntity page) {
        int [] relevanceForPage = new int[lemmas.size()];
        for (int i = 0; i < lemmas.size(); i++) {
            relevanceForPage[i] = indexRepository.findRelevanceForPage(lemmas.get(i).getLemma(), siteEntity.getId(), page.getId()).orElse(0);
        }
        return relevanceForPage;
    }

    private double calculateRelevance(int[] maxRelevance, int[] relevanceForPage) {
        double result = 0;
        for (int i = 0; i < maxRelevance.length; i++) {
            result+= relevanceForPage[i]/(1.0 * maxRelevance[i]);
        }
        return result;
    }
    private void snippetLengthCreation(int lemmaRef, List<String> snippets, StringBuilder snippet) {
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
    }
    private int snippetCreation(LuceneMorphology luceneMorph, int lemmaRef, List<String> snippets, Set<String> lemmaForms, String[] strings, List<String> lemmaStringList) {
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
        return lemmaRef;
    }
    private void creationMapPageEntityWithCount(List<LemmaEntity> lemmas, Map<PageEntity, Integer> pageEntityIntegerMap, int i) {
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

    private List<LemmaEntity> getLemmaEntities(SiteEntity siteEntity, List<LemmaEntity> lemmas) {
        for (String lemma : lemmasFromQuery) {
            if (lemmaRepository.findBySiteAndLemma(siteEntity, lemma).isPresent()) {
                lemmas.add(lemmaRepository.findBySiteAndLemma(siteEntity, lemma).get());
            }
        }
        lemmas = lemmas.stream()
                .sorted(Comparator.comparingLong(LemmaEntity::getFrequency))
                .collect(Collectors.toList());
        return lemmas;
    }
    private List<SiteEntity> getSiteEntities(String site, List<Site> sites) {
        List<SiteEntity> siteEntityList;
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
        return siteEntityList;
    }
    private LuceneMorphology getLuceneMorphology() {
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return luceneMorph;
    }
    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        return siteEntity;
    }
}

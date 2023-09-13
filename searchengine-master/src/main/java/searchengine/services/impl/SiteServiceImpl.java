package searchengine.services.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.enums.Status;
import searchengine.models.SiteEntity;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteService;

@Service
@AllArgsConstructor
public class SiteServiceImpl implements SiteService {
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    @Override
    public SiteEntity addSite(String url) {
        SiteEntity entity = siteRepository.save(transformSiteToSiteEntity(new Site(url, url)));
        sitesList.getSites().add(new Site(url, url));
        return entity;
    }
    private SiteEntity transformSiteToSiteEntity(Site site) {
        SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl()).orElse(new SiteEntity());
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(Status.NEW);
        return siteEntity;
    }
}

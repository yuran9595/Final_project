package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.models.PageEntity;
import searchengine.models.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional <PageEntity> findByPathAndSiteId(String path, Integer siteId);
    @Transactional
    @Modifying
    @Query(value="delete FROM search_engine.page where id>=1", nativeQuery=true)
    void deleteFromPage();

   int countBySite(SiteEntity site);

}

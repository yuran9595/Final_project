package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.LemmaEntity;
import searchengine.models.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Transactional
    @Modifying
    @Query(value="delete FROM search_engine.lemma where id>=1", nativeQuery=true)
    void deleteFromLemma();
    int countBySite(SiteEntity site);
    Optional<LemmaEntity> findBySiteAndLemma(SiteEntity siteEntity, String lemma);
    Optional<LemmaEntity> findLemmaEntityByLemmaAndSite (SiteEntity site, String lemma);
    Optional<LemmaEntity> findLemmaEntityByLemmaAndSite (String lemma ,SiteEntity site );
}

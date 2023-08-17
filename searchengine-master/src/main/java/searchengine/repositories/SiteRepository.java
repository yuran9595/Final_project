package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.SiteEntity;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    Optional <SiteEntity> findByUrl(String url);

    @Transactional
    @Modifying
    @Query(value="delete FROM search_engine.site where id>=1", nativeQuery=true)
    void deleteFromSite();
}

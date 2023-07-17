package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.PageEntity;

import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional <PageEntity> findByPathAndSiteId(String path, Integer siteId);

}

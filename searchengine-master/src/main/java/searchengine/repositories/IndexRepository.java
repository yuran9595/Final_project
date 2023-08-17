package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.IndexEntity;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    @Modifying
    @Query(value="delete FROM search_engine.index_table where id>=1", nativeQuery=true)
    void deleteFromIndex();

}

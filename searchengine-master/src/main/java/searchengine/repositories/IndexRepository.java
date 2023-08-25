package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.IndexEntity;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    @Modifying
    @Query(value="delete FROM search_engine.index_table where id>=1", nativeQuery=true)
    void deleteFromIndex();
    @Query(value = "Select max(rank_index) FROM search_engine.index_table  as ind\n" +
            "Join search_engine.lemma as l on l.id = ind.lemma_id\n" +
            "where l.lemma = ?1 and site_id=?2 and ind.page_id = ?3", nativeQuery = true)
    Integer findRelevance(String lemma, long siteId, long pageId);



}

package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.IndexEntity;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
}

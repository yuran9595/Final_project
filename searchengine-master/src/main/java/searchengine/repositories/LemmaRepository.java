package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.models.LemmaEntity;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
}

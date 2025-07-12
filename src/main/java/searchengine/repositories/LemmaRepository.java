package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("SELECT l FROM Lemma l WHERE l.lemma = ?1 AND l.siteId = ?2")
    Optional<Lemma> findLemmaByLemmaAndSiteId(String lemmaWord, Site id);
}

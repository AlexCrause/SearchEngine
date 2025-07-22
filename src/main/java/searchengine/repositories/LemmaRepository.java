package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.siteId = :siteId")
    Optional<Integer> countLemmasBySite(Site siteId);

    @Query("SELECT l FROM Lemma l WHERE l.siteId = :siteId")
    List<Lemma> findLemmaBySite(Site siteId);


    @Query("SELECT l FROM Lemma l WHERE l.lemma IN :lemmas AND l.siteId = :siteId")
    List<Lemma> findLemmasAtSite(List<String> lemmas, Site siteId);
}

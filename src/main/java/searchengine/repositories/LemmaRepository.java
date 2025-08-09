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

    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.site = :site")
    Optional<Integer> countLemmasBySite(Site site);

    @Query("SELECT l FROM Lemma l WHERE l.site = :site")
    List<Lemma> findLemmaBySite(Site site);


    @Query("SELECT l FROM Lemma l WHERE l.lemma IN :lemmas AND l.site = :site")
    List<Lemma> findLemmasAtSite(List<String> lemmas, Site site);
}

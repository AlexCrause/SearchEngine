package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    int countBySite(Site site);

    List<Lemma> findAllLemmaBySite(Site site);

    List<Lemma> findBySiteAndLemmaIn(Site site, List<String> lemmas);
}

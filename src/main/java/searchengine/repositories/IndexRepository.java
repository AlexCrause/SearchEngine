package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query("SELECT COUNT(*) FROM Index i WHERE i.lemma = ?1")
    Optional<Integer> foundCountPagesByLemmaId(Lemma lemma);

    @Query("SELECT p FROM Page p JOIN p.indexes i WHERE i.lemma = ?1")
    List<Page> findPagesByLemma(Lemma lemma);


    @Query("SELECT i FROM Index i WHERE i.lemma = ?1")
    List<Index> findIndexByLemma(Lemma lemma);

    @Query("SELECT i FROM Index i WHERE i.page = ?1 AND i.lemma IN ?2")
    List<Index> findListIndexesByPageAndLemmaList(Page page, List<Lemma> lemmas);

    @Query("SELECT SUM(i.rank) FROM Index i WHERE i.page = ?1 AND i.lemma IN ?2")
    float sumRankForPageAndLemmas(Page page, List<Lemma> lemmas);
}

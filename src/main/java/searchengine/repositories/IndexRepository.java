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

    @Query("SELECT COUNT(*) FROM Index i WHERE i.lemmaId = ?1")
    Optional<Integer> foundCountPagesByLemmaId(Lemma lemmaId);

    @Query("SELECT p FROM Page p JOIN p.indexes i WHERE i.lemmaId = ?1")
    List<Page> findPagesByLemma(Lemma lemmaId);


    @Query("SELECT i FROM Index i WHERE i.lemmaId = ?1")
    List<Index> findIndexByLemma(Lemma lemmaId);

    @Query("SELECT i FROM Index i WHERE i.pageId = ?1 AND i.lemmaId IN ?2")
    List<Index> findListIndexesByPageAndLemmaList(Page pageId, List<Lemma> lemmas);
}

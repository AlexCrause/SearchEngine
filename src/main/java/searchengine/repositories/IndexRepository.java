package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query("select i from Index i where i.lemmaId = ?1 and i.pageId = ?2")
    Optional<Index> findIndexByLemmaIdAndPageId(Lemma id, Page id1);

    @Query("select count(i) from Index i where i.lemmaId = ?1")
    Optional<Integer> findCountConnectionsLemmaIdWithPagesId(Lemma lemma);
}

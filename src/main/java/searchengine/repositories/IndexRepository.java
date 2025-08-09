package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    int countByLemma(Lemma lemma);

    @Query("SELECT COALESCE(SUM(i.rank), 0) FROM Index i " +
            "WHERE i.page = :page AND i.lemma IN :lemmas")
    float sumRankForPageAndLemmas(
            @Param("page") Page page,
            @Param("lemmas") List<Lemma> lemmas
    );
}

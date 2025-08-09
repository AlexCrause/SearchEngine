package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Optional<Page> findBySiteAndPath(Site site, String path);

    Optional<Page> findPageByPath(String path);

    int countPagesBySite(Site site);

    @Query("SELECT p FROM Page p " +
            "JOIN p.indexes i " +
            "WHERE i.lemma = :lemma")
    List<Page> findPagesByLemma(@Param("lemma") Lemma lemma);
}

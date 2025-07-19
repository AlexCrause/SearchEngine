package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    @Modifying
    @Query("DELETE FROM Page p WHERE p.siteId.id IN (SELECT s.id FROM Site s WHERE s.url = :url)")
    void deletePagesBySiteUrl(@Param("url") String url);

    @Query("SELECT p FROM Page p WHERE p.path = :path AND p.siteId = :siteId")
    Optional<Page> findPageByPathAndSiteId(String path, Site siteId);


    @Query("SELECT p FROM Page p WHERE p.path = :path")
    Optional<Page> findPageByPath(String path);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.siteId = :siteId")
    Optional<Integer> countPagesBySite(Site siteId);
}

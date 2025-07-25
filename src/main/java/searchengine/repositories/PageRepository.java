package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query("SELECT p FROM Page p WHERE p.path = :path AND p.siteId = :siteId")
    Optional<Page> findPageByPathAndSiteId(String path, Site siteId);

    @Query("SELECT p FROM Page p WHERE p.path = :path")
    Optional<Page> findPageByPath(String path);

    @Query("SELECT COUNT(p) FROM Page p WHERE p.siteId = :siteId")
    Optional<Integer> countPagesBySite(Site siteId);

    @Query("SELECT p FROM Page p WHERE p.id = :key")
    Optional<Page> findPageById(Integer key);
}

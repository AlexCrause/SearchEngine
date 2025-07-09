package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query("SELECT s FROM Site s WHERE s.url = ?1")
    Optional<Site> findSiteByUrl(String url);

    @Transactional
    @Modifying
    @Query("DELETE FROM Site s WHERE s.url = :url")
    void deleteSiteByUrl(String url);


    boolean existsByUrl(String url);

    @Transactional
    @Modifying
    @Query("UPDATE Site s SET s.statusTime = CURRENT_TIMESTAMP WHERE s.url = :url")
    void updateStatusTimeByUrl(@Param("url") String url);

    @Transactional
    @Modifying
    @Query("UPDATE Site s SET s.status = :status WHERE s.url = :url")
    void updateStatusByUrl(String url, Status status);
}

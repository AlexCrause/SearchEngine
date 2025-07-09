package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query(value = "SELECT path FROM Page WHERE path = ?1", nativeQuery = true)
    Optional<String> findPathByPath(String path);

    @Transactional
    @Modifying
    @Query("DELETE FROM Page p WHERE p.siteId.id IN (SELECT s.id FROM Site s WHERE s.url = :url)")
    void deletePagesBySiteUrl(@Param("url") String url);
}

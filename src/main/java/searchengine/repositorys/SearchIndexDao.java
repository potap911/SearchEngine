package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.SearchIndex;

import java.util.List;

@Repository
@Transactional
public interface SearchIndexDao extends JpaRepository<SearchIndex, Long> {

    SearchIndex findByPageAndLemma(Page page, Lemma lemma);

    List<SearchIndex> findAllByPage(Page page);

    @Query("""
            SELECT s
            FROM SearchIndex s
            JOIN Lemma l ON s.lemma = l.id
            LEFT JOIN Alias a ON l.id = a.lemma
            WHERE l.lemma = :lemma OR a.alias = :lemma
            """)
    List<SearchIndex> selectByLemma(@Param("lemma") String lemma);

    @Query("""
            SELECT si
            FROM SearchIndex si
            JOIN Lemma l ON si.lemma = l.id
            LEFT JOIN Alias a ON l.id = a.lemma
            JOIN Page p ON p.id = si.page
            JOIN Site s ON s.id = p.site
            WHERE s.id = :siteId AND (l.lemma = :lemma OR a.alias = :lemma)
            """)
    List<SearchIndex> selectByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") Long siteId);
}

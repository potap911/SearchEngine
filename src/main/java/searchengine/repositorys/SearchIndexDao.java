package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.SearchIndex;
import searchengine.entity.Site;

import java.util.List;

@Repository
@Transactional
public interface SearchIndexDao extends JpaRepository<SearchIndex, Long> {

    SearchIndex findByPageAndLemma(Page page, Lemma lemma);

    List<SearchIndex> findAllByPage(Page page);

    List<SearchIndex> findAllByLemma(Lemma lemma);

    void deleteByPage(Page page);

    @Query("""
            SELECT s
            FROM SearchIndex s
            JOIN Lemma l ON s.lemma = l.id
            WHERE l.lemma = :lemma
            """)
    List<SearchIndex> selectByLemma(@Param("lemma") String lemma);

    @Query("""
            SELECT s
            FROM SearchIndex s
            JOIN Page p ON s.page = p.id
            WHERE p.site = :site
            """)
    List<SearchIndex> selectCountLemmaBySite(@Param("site") Site site);
}

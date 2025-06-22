package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Lemma;
import searchengine.entity.Site;

@Repository
@Transactional
public interface LemmaDao extends JpaRepository<Lemma, Long> {

    @Query("""
            SELECT count(p)
            FROM Lemma p
            """)
    int selectCountLemmas();

    <S extends Lemma, R> R findByLemma(String lemma);

    @Query("""
            SELECT COUNT(DISTINCT l)
            FROM Lemma l
            JOIN SearchIndex s ON s.lemma = l.id
            JOIN Page p ON p.id = s.page
            WHERE p.site = :site
            """)
    int selectCountLemmasBySite(@Param("site") Site site);
}

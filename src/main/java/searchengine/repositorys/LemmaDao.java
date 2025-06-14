package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Lemma;

@Repository
@Transactional
public interface LemmaDao extends JpaRepository<Lemma, Long> {

    @Query("""
            SELECT COUNT(*)
            FROM Lemma p
            """)
    int selectCountLemmas();

    <S extends Lemma, R> R findByLemma(String lemma);
}

package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Alias;

@Repository
@Transactional
public interface AliasDao extends JpaRepository<Alias, Long> {

    Alias findByAlias(String alias);

    void deleteByLemmaId(Long lemmaId);
}

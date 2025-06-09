package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Page;

@Repository
@Transactional
public interface PageDao extends JpaRepository<Page, Long> {
    Page findByPath(String path);
}

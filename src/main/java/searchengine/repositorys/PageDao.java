package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.entity.Page;

@Repository
@Transactional
public interface PageDao extends JpaRepository<Page, Long> {

    Page findByPathAndSiteId(String path, Long siteId);

    @Query("""
            SELECT COUNT(*)
            FROM Page p
            """)
    int selectCountPages();
}

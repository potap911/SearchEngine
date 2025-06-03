package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.entity.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
}

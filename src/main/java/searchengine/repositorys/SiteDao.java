package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.enums.Status;
import searchengine.entity.Site;

@Repository
@Transactional
public interface SiteDao extends JpaRepository<Site, Long> {

    Site findByName(String name);

    @Query("""
            SELECT lastError
            FROM Site s
            WHERE s.id = :id
            """)
    String selectLastError(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Site s
            SET s.status = :status, s.statusTime = NOW()
            WHERE s.id = :id
            """)
    void updateStatus(@Param("id") Long id, @Param("status") Status status);

    @Modifying
    @Query("""
            UPDATE Site s
            SET s.status = :status, s.statusTime = NOW(), s.lastError = :lastError
            WHERE s.id = :id
            """)
    void updateStatus(@Param("id") Long id, @Param("status") Status status, @Param("lastError") String lastError);

    @Modifying
    @Query("""
            UPDATE Site s
            SET s.status = :toStatus, s.statusTime = NOW(), s.lastError = :lastError
            WHERE s.status = :fromStatus
            """)
    void updateStatusAfterStopIndexing(@Param("fromStatus") Status fromStatus, @Param("toStatus") Status toStatus, @Param("lastError") String lastError);

    @Modifying
    @Query("""
            UPDATE Site s
            SET s.statusTime = NOW()
            WHERE s.id = :id
            """)
    void updateStatusTime(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Site s
            SET s.statusTime = NOW(), s.lastError = :lastError
            WHERE s.id = :id
            """)
    void updateLastError(@Param("id") Long id, @Param("lastError") String lastError);
}

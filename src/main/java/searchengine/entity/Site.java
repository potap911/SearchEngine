package searchengine.entity;

import lombok.*;
import searchengine.dto.enums.Status;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NonNull
    private Status status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    @NonNull
    private Timestamp statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError = null;

    @Column(nullable = false)
    @NonNull
    private String url;

    @Column(nullable = false)
    @NonNull
    private String name;

    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "site_id")
    private List<Page> pages;

    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "site_id")
    private List<Lemma> lemmas;
}

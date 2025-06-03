package searchengine.entity;

import lombok.*;

import javax.persistence.*;
import javax.persistence.Index;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NonNull
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NonNull
    private String path;

    @Column(nullable = false)
    @NonNull
    private Short code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne()
    @JoinColumn(name = "site_id", insertable = false, updatable = false, nullable = false)
    private Site site;
}

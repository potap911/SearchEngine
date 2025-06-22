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
    private Long id;

    @Column(nullable = false)
    @NonNull
    private String path;

    @Column(nullable = false)
    @NonNull
    private Integer code;

    @Column
    @NonNull
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
}

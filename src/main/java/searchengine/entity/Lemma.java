package searchengine.entity;

import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(columnNames = {"lemma"}))
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*@ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;*/

    @Column(nullable = false)
    @NonNull
    private String lemma;

    /*@Column(nullable = false)
    private Integer frequency;*/
}


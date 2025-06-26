package searchengine.entity;

import lombok.*;

import javax.persistence.*;
import java.util.List;

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

    @Column(nullable = false)
    @NonNull
    private String lemma;

    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "lemma_id")
    private List<Alias> aliases;
}


package searchengine.models;

import lombok.*;

import javax.persistence.*;

@Table(name = "index_table")
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",nullable = false)
    private Integer id;
    @Column(name = "rank_index", nullable = false)
    private Float rank;
    @ManyToOne(cascade = CascadeType.REFRESH)
    private PageEntity page;
    @ManyToOne(cascade = CascadeType.REFRESH)
    private LemmaEntity lemma;
}

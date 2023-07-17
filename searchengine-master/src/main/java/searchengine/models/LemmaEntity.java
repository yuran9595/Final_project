package searchengine.models;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @NotNull
    private Integer id;
    @Column(name = "lemma")
    @NotNull
    private String lemma;
    @Column(name = "frequency")
    @NotNull
    private Integer frequency;
    @ManyToOne(cascade = CascadeType.ALL)
    private SiteEntity site;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lemma")
    private List<IndexEntity> indexes = new ArrayList<>();

}

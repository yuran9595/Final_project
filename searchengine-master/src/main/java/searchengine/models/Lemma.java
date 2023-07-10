package searchengine.models;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma")
public class Lemma {

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
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
    private Site site;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "lemma")
    private List<IndexEntity> indexes = new ArrayList<>();

}

package searchengine.models;

import com.sun.istack.NotNull;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @NotNull
    private Integer id;
    @Column(name = "site_id")
    @NotNull
    private Integer siteId;
    @Column(name = "lemma")
    @NotNull
    private String lemma;
    @Column(name = "frequency")
    @NotNull
    private Integer frequency;
}

package searchengine.models;


import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @ManyToOne(cascade = CascadeType.ALL)
    private SiteEntity site;
    @Column(name = "path", columnDefinition = "TEXT", length = 1000, nullable = false)
    private String path;
    @Column(name = "code", nullable = false)
    private Integer code;
    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "page")
    private List<IndexEntity> indexes = new ArrayList<>();



}

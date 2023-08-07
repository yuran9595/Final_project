package searchengine.models;

import com.sun.istack.NotNull;
import lombok.*;
import searchengine.enums.Status;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "site")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    @Column(name = "status_time")
    @NotNull
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "url")
    @NotNull
    private String url;
    @Column(name = "name")
    @NotNull
    private String name;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site")
    private Set<PageEntity> pages = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site")
    private Set<LemmaEntity> lemmas = new HashSet<>();


}

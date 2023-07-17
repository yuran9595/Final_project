package searchengine.models;

import com.sun.istack.NotNull;
import lombok.*;
import searchengine.enums.Status;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private List<PageEntity> pages = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "site")
    private List<LemmaEntity> lemmas = new ArrayList<>();


}

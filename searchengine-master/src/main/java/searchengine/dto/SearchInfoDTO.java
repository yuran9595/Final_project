package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchInfoDTO {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    Double relevance;
}

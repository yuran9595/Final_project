package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.dto.SearchInfoDTO;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchDTO {
    Boolean result;
    Integer count;
    List<SearchInfoDTO> data = new ArrayList<>();
}

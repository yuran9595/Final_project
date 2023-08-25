package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.SearchDTO;
import searchengine.services.impl.SearchServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SearchController {

    private final SearchServiceImpl searchService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(name = "query", defaultValue = "") String query,
                                    @RequestParam(name = "offset", defaultValue = "0") Integer offset,
                                    @RequestParam(name = "limit", defaultValue = "10") Integer limit,
                                    @RequestParam(name = "site", defaultValue = "All sites")String site) {
        SearchDTO searching = searchService.searching(query, offset, limit, site);

        return ResponseEntity.ok(searching);
    }
}

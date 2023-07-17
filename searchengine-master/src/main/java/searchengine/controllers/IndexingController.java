package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.impl.IndexingServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class IndexingController {

  private final IndexingServiceImpl indexingService;

    @GetMapping("/startIndexing")
    public void startIndexing(){
        indexingService.indexing();
    }


}

package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.ApiResponseDTO;
import searchengine.services.impl.IndexingServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class IndexingController {

  private final IndexingServiceImpl indexingService;

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing(){

        if (!IndexingServiceImpl.isRun) {
            indexingService.indexing();
            Boolean result = true;
            return new  ResponseEntity<>(result, HttpStatus.OK);
        }
        return new ResponseEntity<>(new ApiResponseDTO(false, "Индексация уже запущена"), HttpStatus.OK);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing(){
        if (IndexingServiceImpl.isRun) {
            indexingService.stopIndexing();
            Boolean result = true;
            return new  ResponseEntity<>(result, HttpStatus.OK);
        }
        return new ResponseEntity<>(new ApiResponseDTO(false, "Индексация не запущена"), HttpStatus.OK);
    }
}

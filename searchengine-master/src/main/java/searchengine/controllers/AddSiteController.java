package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.ApiResponseDTO;
import searchengine.services.impl.SiteServiceImpl;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")

public class AddSiteController {
    private final SiteServiceImpl siteService;

    @PostMapping("/indexPage")
    public ResponseEntity<?> addPage(String url) {
        if (!url.startsWith("http")) {
            return new ResponseEntity<>(new ApiResponseDTO(false, "Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле\n"), HttpStatus.BAD_REQUEST);
        }
        siteService.addSite(url);
        return ResponseEntity.ok("true");
    }
}

package searchengine.services;

import searchengine.dto.SearchDTO;

public interface SearchService {

    SearchDTO searching(String query, int offset, int limit, String site);

}

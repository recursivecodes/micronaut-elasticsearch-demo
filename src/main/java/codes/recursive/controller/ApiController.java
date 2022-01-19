package codes.recursive.controller;

import codes.recursive.command.SearchCommand;
import codes.recursive.domain.Favorite;
import codes.recursive.repository.FavoriteRepository;
import codes.recursive.service.SearchService;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import org.elasticsearch.action.search.SearchResponse;

import java.io.IOException;
import java.util.Optional;

@Controller("/api")
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ApiController {

    private final FavoriteRepository favoriteRepository;
    private final SearchService searchService;
    private final String indexName;

    public ApiController(
            FavoriteRepository favoriteRepository,
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.favoriteRepository = favoriteRepository;
        this.searchService = searchService;
        this.indexName = indexName;
    }

    @Post(uri = "/favorite", produces = MediaType.APPLICATION_JSON)
    public HttpResponse saveFavorite(@Body Favorite favorite) {
        return HttpResponse.created(
                favoriteRepository.save(favorite)
        );
    }

    @Put(uri = "/favorite", produces = MediaType.APPLICATION_JSON)
    public HttpResponse updateFavorite(@Body Favorite favorite) {
        return HttpResponse.ok(
                favoriteRepository.update(favorite)
        );
    }

    @Get(uri="/delete/{id}")
    public HttpResponse deleteFavorite(Long id) {
        Optional<Favorite> favorite = favoriteRepository.findById(id);
        favorite.ifPresent(favoriteRepository::delete);
        return HttpResponse.noContent();
    }

    @Post(uri = "/search", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse searchPost(SearchCommand searchCommand) throws IOException {
        SearchResponse searchResponse = searchService.search(searchCommand, indexName);
        return HttpResponse.ok(
                CollectionUtils.mapOf(
                        "searchCommand",
                        searchCommand,
                        "searchResponse",
                        searchResponse
                )
        );
    }
}
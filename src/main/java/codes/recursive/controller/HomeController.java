package codes.recursive.controller;

import codes.recursive.command.SearchCommand;
import codes.recursive.domain.Favorite;
import codes.recursive.repository.FavoriteRepository;
import codes.recursive.service.SearchService;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.*;
import io.micronaut.views.ModelAndView;
import org.elasticsearch.action.search.SearchResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Controller()
@SuppressWarnings({ "unchecked", "rawtypes" })
public class HomeController {

    private final FavoriteRepository favoriteRepository;
    private final SearchService searchService;
    private final String indexName;

    public HomeController(
            FavoriteRepository favoriteRepository,
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.favoriteRepository = favoriteRepository;
        this.searchService = searchService;
        this.indexName = indexName;
    }

    @Get()
    public HttpResponse index() {
        return HttpResponse.redirect(URI.create("/search"));
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

    @Get("/favorite/edit")
    public ModelAndView create() {
        return new ModelAndView("edit", CollectionUtils.mapOf());
    }

    @Post(value = "/favorite/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public ModelAndView create(@Body Favorite favorite) {
        favoriteRepository.save(favorite);
        return new ModelAndView("edit", CollectionUtils.mapOf(
                "saved", true
        ));
    }

    @Get(uri="/delete/{id}")
    public HttpResponse<String> deleteFavorite(Long id) {
        Optional<Favorite> favorite = favoriteRepository.findById(id);
        favorite.ifPresent(favoriteRepository::delete);
        return HttpResponse.redirect(URI.create("/search"));
    }

    @Get(uri = "/search")
    public ModelAndView searchGet() {
        return new ModelAndView("search", CollectionUtils.mapOf());
    }

    @Post(uri = "/search", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public ModelAndView searchPost(@Body SearchCommand searchCommand) throws IOException {
        SearchResponse searchResponse = searchService.search(searchCommand, indexName);
        return new ModelAndView("search", CollectionUtils.mapOf("searchCommand", searchCommand, "searchResponse", searchResponse));
    }

    @Error(global = true)
    public HttpResponse notFound(HttpRequest request, Exception e) {
        return HttpResponse.badRequest(
                CollectionUtils.mapOf(
                        "message",
                        e.getMessage(),
                        "description",
                        e.getCause(),
                        "stackTrace",
                        e.getStackTrace()
                )
        );
    }

}
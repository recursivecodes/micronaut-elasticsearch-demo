package codes.recursive.controller;

import codes.recursive.command.SearchCommand;
import codes.recursive.domain.Favorite;
import codes.recursive.repository.FavoriteRepository;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.*;
import io.micronaut.views.ModelAndView;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Controller("/")
public class HomeController {

    private final FavoriteRepository favoriteRepository;
    private final RestHighLevelClient searchClient;
    private final String indexName;

    public HomeController(
            FavoriteRepository favoriteRepository,
            RestHighLevelClient searchClient,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.favoriteRepository = favoriteRepository;
        this.searchClient = searchClient;
        this.indexName = indexName;
    }

    @Get(uri="/", produces="text/plain")
    public HttpResponse index() {
        return HttpResponse.redirect(URI.create("/search"));
    }

    @Post(uri = "/", produces = MediaType.APPLICATION_JSON)
    public HttpResponse saveFavorite(@Body Favorite favorite) {
        return HttpResponse.created(
                favoriteRepository.save(favorite)
        );
    }

    @Put(uri = "/", produces = MediaType.APPLICATION_JSON)
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
        if (favorite.isPresent()) {
            favoriteRepository.delete(favorite.get());
        }
        return HttpResponse.redirect(URI.create("/search"));
    }

    @Get(uri = "/search")
    public ModelAndView searchGet() throws IOException {
        return new ModelAndView("search", CollectionUtils.mapOf());
    }

    @Post(uri = "/search", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public ModelAndView searchPost(@Body SearchCommand searchCommand) throws IOException {
        SearchResponse searchResponse = null;
        if (searchCommand.getSearchString().length() > 0 ) {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(searchCommand.getSearchString());
            queryStringQueryBuilder.defaultOperator(Operator.OR);
            searchSourceBuilder.query(queryStringQueryBuilder);
            searchSourceBuilder.from(searchCommand.getOffset());
            searchSourceBuilder.size(searchCommand.getMax() > 25 ? 25 : searchCommand.getMax());
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices(indexName);
            searchRequest.source(searchSourceBuilder);
            searchResponse = searchClient.search(searchRequest, RequestOptions.DEFAULT);
        }
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
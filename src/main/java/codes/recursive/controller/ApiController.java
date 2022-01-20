package codes.recursive.controller;

import codes.recursive.command.SearchCommand;
import codes.recursive.domain.BlogPost;
import codes.recursive.repository.BlogPostRepository;
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
@SuppressWarnings({"rawtypes" })
public class ApiController {

    private final BlogPostRepository blogPostRepository;
    private final SearchService searchService;
    private final String indexName;

    public ApiController(
            BlogPostRepository blogPostRepository,
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.blogPostRepository = blogPostRepository;
        this.searchService = searchService;
        this.indexName = indexName;
    }

    @Post(uri = "/blogPost", produces = MediaType.APPLICATION_JSON)
    public HttpResponse saveFavorite(@Body BlogPost blogPost) {
        return HttpResponse.created(
                blogPostRepository.save(blogPost)
        );
    }

    @Put(uri = "/blogPost", produces = MediaType.APPLICATION_JSON)
    public HttpResponse updateFavorite(@Body BlogPost blogPost) {
        return HttpResponse.ok(
                blogPostRepository.update(blogPost)
        );
    }

    @Get(uri="/delete/{id}")
    public HttpResponse deleteFavorite(Long id) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        blogPost.ifPresent(blogPostRepository::delete);
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
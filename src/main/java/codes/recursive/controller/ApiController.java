package codes.recursive.controller;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import codes.recursive.command.SearchCommand;
import codes.recursive.domain.BlogPost;
import codes.recursive.repository.BlogPostRepository;
import codes.recursive.service.SearchService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.io.IOException;
import java.util.Map;
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
    public HttpResponse saveBlogPost(@Body BlogPost blogPost) {
        return HttpResponse.created(
                blogPostRepository.save(blogPost)
        );
    }

    @Put(uri = "/blogPost", produces = MediaType.APPLICATION_JSON)
    public HttpResponse updateBlogPost(@Body BlogPost blogPost) {
        return HttpResponse.ok(
                blogPostRepository.update(blogPost)
        );
    }

    @Get(uri="/delete/{id}")
    public HttpResponse deleteBlogPost(Long id) {
        Optional<BlogPost> blogPost = blogPostRepository.findById(id);
        blogPost.ifPresent(blogPostRepository::delete);
        return HttpResponse.noContent();
    }

    @Post(uri = "/search", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse searchPost(SearchCommand searchCommand) throws IOException {
        SearchResponse searchResponse = searchService.search(searchCommand, indexName);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        Map response = CollectionUtils.mapOf(
                "searchCommand", searchCommand,
                "searchResponse", searchResponse
        );
        return HttpResponse.ok(mapper.writeValueAsString(response));
    }
}
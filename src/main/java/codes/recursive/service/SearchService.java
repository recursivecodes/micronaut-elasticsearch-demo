package codes.recursive.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import codes.recursive.command.SearchCommand;
import codes.recursive.domain.BlogPost;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Introspected
@Singleton
@SuppressWarnings({"unused"})
public class SearchService {
    private final ElasticsearchClient searchClient;
    private final ElasticsearchAsyncClient asyncSearchClient;

    public SearchService(ElasticsearchClient searchClient, ElasticsearchAsyncClient asyncSearchClient) {
        this.searchClient = searchClient;
        this.asyncSearchClient = asyncSearchClient;
    }

    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexResponse deleteIndexResponse = searchClient
                .indices()
                .delete(builder ->
                        builder.index(indexName)
                );
        return deleteIndexResponse.acknowledged();
    }

    public CreateIndexResponse createIndex(String indexName) throws IOException {
        return searchClient.indices().create(builder -> builder
                .index(indexName)
                // schema for documents
                .mappings(mappingsBuilder -> mappingsBuilder
                        .properties("id", p -> p.long_(b -> b.nullValue(null)))
                        .properties("title", p -> p.text(b -> b))
                        .properties("description", p -> p.text(b -> b))
                        .properties("article", p -> p.text(b -> b))
                )
        );
    }

    public boolean indexExists(String indexName) throws IOException {
        return searchClient.indices().exists(b -> b.index(indexName)).value();
    }

    public CompletableFuture<IndexResponse> indexBlogPost(BlogPost blogPost, String indexName) throws IOException {
        return asyncSearchClient.index(b -> b
                .index(indexName)
                .id(blogPost.getId().toString())
                .document(blogPost)
        );
    }

    public CompletableFuture<DeleteResponse> deleteIndexedBlogPost(BlogPost blogPost, String indexName) throws IOException {
        return asyncSearchClient.delete(b -> b
                .index(indexName)
                .id(blogPost.getId().toString())
        );
    }

    public SearchResponse<BlogPost> search(String searchString, String indexName, Integer offset, Integer max) throws IOException {
        return search( new SearchCommand(searchString, offset, max), indexName);
    }

    public SearchResponse<BlogPost> search(SearchCommand searchCommand, String indexName) throws IOException {
        return searchClient.search(s -> s
                .index(indexName)
                .from(searchCommand.getOffset())
                .size(searchCommand.getMax() > 25 ? 25 : searchCommand.getMax())
                .query(q -> q
                        .queryString(qs -> qs
                                .defaultOperator(Operator.Or)
                                .query(searchCommand.getSearchString())
                        )
                ), BlogPost.class);
    }
}

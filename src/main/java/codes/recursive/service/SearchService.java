package codes.recursive.service;

import codes.recursive.command.SearchCommand;
import codes.recursive.domain.BlogPost;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Singleton;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Introspected
@Singleton
/**
 * SearchService.java
 * Uses deprecated RestHighLevelClient - replace with ElasticsearchClient when `io.micronaut.elasticsearch:micronaut-elasticsearch:4.2.0` is available
 */
public class SearchService {

    private final RestHighLevelClient searchClient;

    public SearchService(RestHighLevelClient searchClient) {
        this.searchClient = searchClient;
    }

    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        AcknowledgedResponse deleteResponse = searchClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        return deleteResponse.isAcknowledged();
    }

    public CreateIndexResponse createIndex(String indexName) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        // schema for documents
        Map<String, Object> schema = new HashMap<>();
        schema.put("id", CollectionUtils.mapOf("type", "long"));
        schema.put("title", CollectionUtils.mapOf("type", "text"));
        schema.put("description", CollectionUtils.mapOf("type", "text"));
        schema.put("article", CollectionUtils.mapOf("type", "text"));

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", schema);
        createIndexRequest.mapping(mapping);

        return searchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    public boolean indexExists(String indexName) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        return searchClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    public void indexBlogPost(BlogPost blogPost, String indexName, ActionListener<IndexResponse> listener) {
        ObjectMapper mapper = new ObjectMapper();
        IndexRequest indexRequest = new IndexRequest(indexName);
        try {
            indexRequest.id(blogPost.getId().toString()).source(
                    mapper.writeValueAsString(blogPost),
                    XContentType.JSON
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        searchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
    }

    public void deleteBlogPost(BlogPost blogPost, String indexName, ActionListener<DeleteResponse> listener) {
        DeleteRequest deleteIndexRequest = new DeleteRequest(indexName, blogPost.getId().toString());
        searchClient.deleteAsync(deleteIndexRequest, RequestOptions.DEFAULT, listener);
    }

    public SearchResponse search(SearchCommand searchCommand, String indexName) throws IOException {
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
        return searchResponse;
    }
}

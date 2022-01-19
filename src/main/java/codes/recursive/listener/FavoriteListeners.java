package codes.recursive.listener;

import codes.recursive.domain.Favorite;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.data.event.listeners.*;
import jakarta.inject.Singleton;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Factory
public class FavoriteListeners {
    private static final Logger LOG = LoggerFactory.getLogger(FavoriteListeners.class);
    private final RestHighLevelClient searchClient;
    private final String indexName;
    private ActionListener<IndexResponse> indexListener;

    public FavoriteListeners(
            RestHighLevelClient searchClient,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.searchClient = searchClient;
        this.indexName = indexName;

        this.indexListener = new ActionListener<>() {
             @Override
             public void onResponse(IndexResponse indexResponse) {
                 DocWriteResponse.Result indexResponseResult = indexResponse.getResult();
                 if (indexResponseResult == DocWriteResponse.Result.CREATED) {
                     LOG.info("Index created: {}", indexResponse.getId());
                 }
                 else if (indexResponseResult == DocWriteResponse.Result.UPDATED) {
                     LOG.info("Index updated: {}", indexResponse.getId());
                 }
             }
             @Override
             public void onFailure(Exception e) {
                 e.printStackTrace();
             }
        };
    }

    @Singleton
    PrePersistEventListener<Favorite> beforeFavoritePersist() {
        return (favorite) -> true;
    }

    @Singleton
    PostPersistEventListener<Favorite> afterFavoritePersist() {
        return (favorite) -> {
            LOG.info("Indexing favorite: {}", favorite.getId() );
            ObjectMapper mapper = new ObjectMapper();
            IndexRequest indexRequest = new IndexRequest(indexName);
            try {
                indexRequest.id(favorite.getId().toString()).source(
                        mapper.writeValueAsString(favorite),
                        XContentType.JSON
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            searchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, indexListener);
        };
    }

    @Singleton
    PreUpdateEventListener<Favorite> beforeFavoriteUpdate() {
        return (favorite) -> true;
    }

    @Singleton
    PostUpdateEventListener<Favorite> afterFavoriteUpdate() {
        return (favorite) -> {
            LOG.info("Indexing favorite: {}", favorite.getId() );
            ObjectMapper mapper = new ObjectMapper();
            IndexRequest indexRequest = new IndexRequest(indexName);
            try {
                indexRequest.id(favorite.getId().toString()).source(
                        mapper.writeValueAsString(favorite),
                        XContentType.JSON
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            searchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, indexListener);
        };
    }

    @Singleton
    PreRemoveEventListener<Favorite> beforeFavoriteRemove() {
        return (favorite) -> true;
    }

    @Singleton
    PostRemoveEventListener<Favorite> afterFavoriteRemove() {
        return (favorite) -> {
            LOG.info("Deleting indexed favorite: {}", favorite );
            DeleteRequest deleteIndexRequest = new DeleteRequest(indexName, favorite.getId().toString());
            searchClient.deleteAsync(deleteIndexRequest, RequestOptions.DEFAULT, new ActionListener<DeleteResponse>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {
                    LOG.info("Index deleted: {}", favorite.getId());
                }
                @Override
                public void onFailure(Exception e) {
                    e.printStackTrace();
                }
            });
        };
    }
}

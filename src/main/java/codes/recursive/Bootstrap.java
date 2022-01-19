package codes.recursive;

import codes.recursive.domain.Favorite;
import codes.recursive.repository.FavoriteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Singleton
public class Bootstrap implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private final FavoriteRepository favoriteRepository;
    private final RestHighLevelClient searchClient;
    private final String indexName;
    private final String indexType;

    public Bootstrap(
            FavoriteRepository favoriteRepository,
            RestHighLevelClient searchClient,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName,
            @Property(name = "codes.recursive.elasticsearch.index.type") String indexType
    ) {
        this.favoriteRepository = favoriteRepository;
        this.searchClient = searchClient;
        this.indexName = indexName;
        this.indexType = indexType;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        LOG.info("ServerStartupEvent handler begin...");

        // create index (if not exists)
        LOG.info("Checking for existing index named: {}", indexName);
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        boolean indexExists = false;
        try {
            indexExists = searchClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Index exists: {}", indexExists);
        if (!indexExists) {
            LOG.info("Creating index: {}", indexName);
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

            // schema for documents
            Map<String, Object> schema = new HashMap<>();
            schema.put("id", CollectionUtils.mapOf("type", "long"));
            schema.put("favoriteMovie", CollectionUtils.mapOf("type", "text"));
            schema.put("favoriteCity", CollectionUtils.mapOf("type", "text"));
            schema.put("favoriteAnimal", CollectionUtils.mapOf("type", "text"));
            schema.put("favoriteCarModel", CollectionUtils.mapOf("type", "text"));
            schema.put("favoriteColor", CollectionUtils.mapOf("type", "text"));
            schema.put("favoritePlant", CollectionUtils.mapOf("type", "text"));
            Map mapping = CollectionUtils.mapOf("properties", schema);
            createIndexRequest.mapping(mapping);

            CreateIndexResponse createIndexResponse = null;
            try {
                createIndexResponse = searchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (createIndexResponse != null) {
                LOG.info("Create index response ack: {}", createIndexResponse.isAcknowledged());
            }
        }

        // create records (if none exist)
        LOG.info("Checking database for records...");
        Iterable<Favorite> existingFavorites = favoriteRepository.findAll();
        if( StreamSupport.stream(existingFavorites.spliterator(), false).findAny().isEmpty() ) {
            LOG.info("Populating records in DB...");
            for(int i=0; i<500; i++) {
                Faker faker = new Faker();
                Favorite favorite = Favorite.builder()
                        .favoriteArtist(faker.artist().name())
                        .favoriteBeer(faker.beer().name())
                        .favoriteBook(faker.book().title())
                        .favoriteColor(faker.color().name())
                        .favoriteCat(faker.cat().breed())
                        .favoriteSuperhero(faker.superhero().name())
                        .build();
                favoriteRepository.save(favorite);
            }
            LOG.info("Records created in DB!");
        }
        else {
            LOG.info("DB records exist!");
        }

        // index all favorites
        LOG.info("Preparing to index all favorites...");
        BulkRequest indexRequest = new BulkRequest();
        ObjectMapper mapper = new ObjectMapper();
        Iterable<Favorite> favorites = favoriteRepository.findAll();
        for (Favorite favorite:favorites) {
            LOG.info("Adding favorite {} to bulk request...", favorite.getId().toString());
            try {
                indexRequest.add(
                        new IndexRequest(indexName).id(favorite.getId().toString()).source(
                                mapper.writeValueAsString(favorite),
                                XContentType.JSON
                        )
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        ActionListener<BulkResponse> indexListener = new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkResponse) {
                LOG.info("Bulk index complete!");
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    IndexResponse indexResponse = (IndexResponse) bulkItemResponse.getResponse();
                    LOG.info("Index response: {}", indexResponse.status().toString());
                }
            }
            @Override
            public void onFailure(Exception e) {
                LOG.warn("Index failure: {}", e.getMessage());
                e.printStackTrace();
            }
        };
        LOG.info("Submitting bulk index request...");
        searchClient.bulkAsync(indexRequest, RequestOptions.DEFAULT, indexListener);

        LOG.info("ServerStartupEvent handler end...");
    }
}
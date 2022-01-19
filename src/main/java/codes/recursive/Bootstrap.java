package codes.recursive;

import codes.recursive.domain.Favorite;
import codes.recursive.repository.FavoriteRepository;
import codes.recursive.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
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
    private final SearchService searchService;
    private final String indexName;
    private final String indexType;

    public Bootstrap(
            FavoriteRepository favoriteRepository,
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName,
            @Property(name = "codes.recursive.elasticsearch.index.type") String indexType
    ) {
        this.favoriteRepository = favoriteRepository;
        this.searchService = searchService;
        this.indexName = indexName;
        this.indexType = indexType;
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        LOG.info("ServerStartupEvent handler begin...");

        // create index (if it doesn't exist)
        LOG.info("Checking for existing index named: {}", indexName);
        boolean indexExists = searchService.indexExists(indexName);
        LOG.info("Index exists? {}", indexExists);
        if (!indexExists) {
            LOG.info("Creating index: {}", indexName);
            CreateIndexResponse createIndexResponse = searchService.createIndex(indexName);
            if (createIndexResponse != null) {
                LOG.info("Create index response ack: {}", createIndexResponse.isAcknowledged());
            }
        }

        // clean up favorites (delete all)
        LOG.info("Deleting records...");
        favoriteRepository.deleteAll();
        LOG.info("Records deleted!");

        // create favorites records (will be indexed automatically)
        LOG.info("Creating 500 records...");
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

        LOG.info("ServerStartupEvent handler end...");
    }
}
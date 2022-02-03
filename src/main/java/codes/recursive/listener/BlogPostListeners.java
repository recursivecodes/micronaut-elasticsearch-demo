package codes.recursive.listener;

import codes.recursive.domain.BlogPost;
import codes.recursive.service.SearchService;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.data.event.listeners.*;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Factory
@SuppressWarnings({"unused"})
public class BlogPostListeners {
    private static final Logger LOG = LoggerFactory.getLogger(BlogPostListeners.class);
    private final SearchService searchService;
    private final String indexName;

    public BlogPostListeners(
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.searchService = searchService;
        this.indexName = indexName;
    }

    @Singleton
    PrePersistEventListener<BlogPost> beforeBlogPostPersist() {
        return (blogPost) -> true;
    }

    @Singleton
    PostPersistEventListener<BlogPost> afterBlogPostPersist() {
        return this::saveIndex;
    }

    @Singleton
    PreUpdateEventListener<BlogPost> beforeBlogPostUpdate() {
        return (blogPost) -> true;
    }

    @Singleton
    PostUpdateEventListener<BlogPost> afterBlogPostUpdate() {
        return this::saveIndex;
    }

    @Singleton
    PreRemoveEventListener<BlogPost> beforeBlogPostRemove() {
        return (blogPost) -> true;
    }

    @Singleton
    PostRemoveEventListener<BlogPost> afterBlogPostRemove() {
        return this::deleteIndex;
    }
    private void saveIndex(BlogPost blogPost) {
        LOG.info("Indexing blogPost: {}", blogPost.getId() );
        try {
            searchService.indexBlogPost(blogPost, indexName)
                    .thenAccept(response -> LOG.info("Index operation ({}) complete!", response.result()))
                    .exceptionally(e -> {
                        LOG.error("Exception while indexing: {}", e.getMessage());
                        return null;
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteIndex(BlogPost blogPost) {
        LOG.info("Deleting indexed blogPost with ID: {}", blogPost.getId() );
        try {
            searchService.deleteIndexedBlogPost(blogPost, indexName)
                    .thenAccept(response -> LOG.info("Index deleted: {}", blogPost.getId()))
                    .exceptionally(e -> {
                        LOG.error("Exception while deleting index: {}", e.getMessage());
                        return null;
                    });
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

package codes.recursive.listener;

import codes.recursive.domain.BlogPost;
import codes.recursive.service.SearchService;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.data.event.listeners.*;
import jakarta.inject.Singleton;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Factory
@SuppressWarnings({"unused"})
public class BlogPostListeners {
    private static final Logger LOG = LoggerFactory.getLogger(BlogPostListeners.class);
    private final SearchService searchService;
    private final String indexName;
    private final ActionListener<IndexResponse> indexListener;

    public BlogPostListeners(
            SearchService searchService,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName
    ) {
        this.searchService = searchService;
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
    PrePersistEventListener<BlogPost> beforeBlogPostPersist() {
        return (blogPost) -> true;
    }

    @Singleton
    PostPersistEventListener<BlogPost> afterBlogPostPersist() {
        return (blogPost) -> {
            LOG.info("Indexing blogPost: {}", blogPost.getId() );
            searchService.indexBlogPost(blogPost, indexName, indexListener);
        };
    }

    @Singleton
    PreUpdateEventListener<BlogPost> beforeBlogPostUpdate() {
        return (blogPost) -> true;
    }

    @Singleton
    PostUpdateEventListener<BlogPost> afterBlogPostUpdate() {
        return (blogPost) -> {
            LOG.info("Indexing blogPost: {}", blogPost.getId() );
            searchService.indexBlogPost(blogPost, indexName, indexListener);
        };
    }

    @Singleton
    PreRemoveEventListener<BlogPost> beforeBlogPostRemove() {
        return (blogPost) -> true;
    }

    @Singleton
    PostRemoveEventListener<BlogPost> afterBlogPostRemove() {
        return (blogPost) -> {
            LOG.info("Deleting indexed blogPost: {}", blogPost );
            ActionListener<DeleteResponse> actionListener = new ActionListener<>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {
                    LOG.info("Index deleted: {}", blogPost.getId());
                }
                @Override
                public void onFailure(Exception e) {
                    e.printStackTrace();
                }
            };
            searchService.deleteBlogPost(blogPost, indexName, actionListener);
        };
    }
}

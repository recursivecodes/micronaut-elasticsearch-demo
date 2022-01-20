package codes.recursive;

import codes.recursive.domain.BlogPost;
import codes.recursive.repository.BlogPostRepository;
import codes.recursive.service.SearchService;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.apache.commons.text.StringEscapeUtils;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Singleton
@SuppressWarnings("unused")
public class Bootstrap implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private final SearchService searchService;
    private final BlogPostRepository blogPostRepository;
    private final String indexName;

    public Bootstrap(
            SearchService searchService,
            BlogPostRepository blogPostRepository,
            @Property(name = "codes.recursive.elasticsearch.index.name") String indexName) {
        this.searchService = searchService;
        this.blogPostRepository = blogPostRepository;
        this.indexName = indexName;
    }

    @SneakyThrows
    @Override
    @SuppressWarnings({"unchecked"})
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
        blogPostRepository.deleteAll();
        LOG.info("Records deleted!");

        LOG.info("Importing blog posts...");
        URL feedSource = new URL("https://recursive.codes/blog/feed");
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedSource));

        List<BlogPost> blogPosts = new ArrayList<>();
        feed.getEntries().forEach( (s) -> {
            SyndEntry item = (SyndEntry) s;
            String article = ((SyndContentImpl) item.getContents().get(0)).getValue();
            article = StringEscapeUtils.unescapeHtml4(article);
            BlogPost blogPost = BlogPost.builder()
                            .title(item.getTitle())
                            .description(item.getDescription().getValue())
                            .article(article)
                            .build();
            blogPosts.add(blogPost);
        });
        blogPostRepository.saveAll(blogPosts);
        // create favorites records (will be indexed automatically)
        LOG.info("Blog posts imported!");
        LOG.info("ServerStartupEvent handler end...");
    }
}
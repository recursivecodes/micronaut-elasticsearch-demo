package codes.recursive.repository;

import codes.recursive.domain.BlogPost;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface BlogPostRepository extends CrudRepository<BlogPost, Long> {}

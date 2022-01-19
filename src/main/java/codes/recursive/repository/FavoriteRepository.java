package codes.recursive.repository;

import codes.recursive.domain.Favorite;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface FavoriteRepository extends CrudRepository<Favorite, Long> {}

package codes.recursive.domain;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Data @Builder
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String favoriteArtist;
    private String favoriteBeer;
    private String favoriteBook;
    private String favoriteCat;
    private String favoriteColor;
    private String favoriteSuperhero;
}

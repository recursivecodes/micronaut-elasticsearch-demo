# Micronaut ElasticSearch Demo

## Test

### Create Favorite

```shell
$ curl -i \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"favoriteArtist": "Van Gogh", "favoriteBeer": "Busch Light", "favoriteBook": "None", "favoriteCat": "British Shorthair", "favoriteColor": "Magenta", "favoriteSuperhero": "Snowman"}' \
  http://localhost:8080/favorite 
```

### Search

```shell
$ curl -s \
  -X POST \
  -d 'searchString=blue' \
  -d 'max=5' \
  http://localhost:8080/search | html2text
```
# Micronaut ElasticSearch Demo

## Test

### Create Favorite

```shell
$ curl -i \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"favoriteArtist": "Van Gogh", "favoriteBeer": "Busch Light", "favoriteBook": "None", "favoriteCat": "British Shorthair", "favoriteColor": "Magenta", "favoriteSuperhero": "Snowman"}' \
  http://localhost:8080/api/favorite 
```

### Search

```shell
$ curl -s \
  -X POST \
  -d 'searchString=blue' \
  -d 'max=5' \
  http://localhost:8080/search | html2text
```

Using API:

```shell
$ curl -s \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"searchString": "blue", "offset": 0, "max": 5}' \
  http://localhost:8080/api/search | jq
```
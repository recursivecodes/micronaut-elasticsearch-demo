# Micronaut ElasticSearch Demo

## Test

### Create Blog Post

```shell
$ curl -i \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"title": "New Blog Post", "description": "A new blog post for your reading pleasure.", "article" : "<p>Hello, world!</p>"}' \
  http://localhost:8080/api/blogPost 
```

### Search

```shell
$ curl -s \
  -X POST \
  -d 'searchString=Java' \
  -d 'max=5' \
  http://localhost:8080/search | html2text
```

Using API:

```shell
$ curl -s \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"searchString": "java", "offset": 0, "max": 5}' \
  http://localhost:8080/api/search | jq
```
# Micronaut ElasticSearch Demo

- [x] Dependencies - refer to docs
- [ ] Config (`application-dev.yml`)
- [ ] Domain class (`BlogPost`)
- [ ] Repository (`BlogPostRepository`)
- [ ] Search Service (`SearchService`)
- [ ] Controller (`PageController`/`ApiController`)
- [ ] Listener (`BlogPostListeners`)
- [ ] `ServerStartupEvent` handler (`Bootstrap.java`)
- [ ] Demo UI
- [ ] Demo API

## API Demo

### Create Blog Post

```shell
POST_ID=$(curl \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"title": "New Blog Post", "description": "A new blog post for your reading pleasure.", "article" : "<p>Hello, world!</p>"}' \
  http://localhost:8080/api/blogPost | jq .id) 
```

Response:

```shell
HTTP/1.1 201 Created
date: Thu, 3 Feb 2022 16:13:22 GMT
Content-Type: application/json
content-length: 125
connection: keep-alive
{"id":26,"title":"New Blog Post","description":"A new blog post for your reading pleasure.","article":"<p>Hello, world!</p>"}%
```

Save ID:

```shell
POST_ID=$(curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"title": "New Blog Post", "description": "A new blog post for your reading pleasure.", "article" : "<p>Hello, world!</p>"}' \
  http://localhost:8080/api/blogPost | jq .id)
```

### Update Blog Post

```shell
curl -i \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{"id": '$POST_ID', "title": "New Blog Post", "description": "An updated blog post for your reading pleasure.", "article" : "<p>Hello, world!</p>"}' \
  http://localhost:8080/api/blogPost
```

### Delete Blog Post

```shell
curl -i localhost:8080/api/delete/$POST_ID
```

Response:

```shell
HTTP/1.1 204 No Content
date: Thu, 3 Feb 2022 16:13:57 GMT
connection: keep-alive
```

### Search

```shell
curl -s \
  -X POST \
  -d 'searchString=Java' \
  -d 'max=5' \
  http://localhost:8080/search | html2text
```

Using API:

GET:

```shell
curl http://localhost:8080/api/search\?searchString\=java\&offset\=0\&max\=5 | fx
```

POST:

```shell
curl -s \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"searchString": "java", "offset": 0, "max": 5}' \
  http://localhost:8080/api/search | fx
```
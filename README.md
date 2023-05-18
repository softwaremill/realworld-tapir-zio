# ![RealWorld Example App](logo.png)

> ### [Scala with tapir and ZIO] codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.


### [Demo](https://demo.realworld.io/)&nbsp;&nbsp;&nbsp;&nbsp;[RealWorld](https://github.com/gothinkster/realworld)


This codebase was created to demonstrate a backend application built with **[Scala with tapir and ZIO]** including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the **[Scala with tapir and ZIO]** community styleguides & best practices.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.


# How it works
### Aplication stack 

This application uses basically Scala with tapir and ZIO with some other technologies:
* Json Web Token to authorization
* SQLite as database
* Quill to express SQL queries in Scala
* HikariCP to initialize database
* Flyway to database migrations
* Swagger to create API documentation

Additionally, execution of Realworld Postman collection is part of CI.

### Architecture
The application has been divided by functionalities into the following packages:
* `common`
* `db`
* `auth`
* `articles`
* `users`

The `common` package contains auxiliary structures that are used in the project.
The `db` package has the necessary data to operate the database.
The `auth` package contains the tools needed to authorize the user.


The `articles` consist of 3 smaller submodules: `core`, `comments` and `users`.
Each of them has a similar structure. They contain specific endpoints, service them using services and communicate with the database using repositories.
These packages include domain classes and clearly separated api layer that contains "the contract", i.e. the endpoint descriptions, without logic.

Project ASCII tree:

```
articles
├── comments
│   ├── api
│   │   ├── CommentCreateData 
│   │   ├── CommentCreateRequest
│   │   ├── CommentResponse
│   │   ├── CommentsEndpoints
│   │   └── CommentsListResponse
│   ├── Comment
│   ├── CommentAuthor
│   ├── CommentsRepository
│   ├── CommentsServerEndpoints
│   └── CommentsService
├── core
│   ├── api
│   │   ├── ArticleCreateData
│   │   ├── ArticleCreateRequest
│   │   ├── ArticleResponse
│   │   ├── ArticlesEndpoints
│   │   ├── ArticlesListResponse
│   │   ├── ArticleUpdateData
│   │   └── ArticleUpdateRequest
│   ├── Article
│   ├── ArticleAuthor
│   ├── ArticlesFilters
│   ├── ArticlesRepository
│   ├── ArticlesServerEndpoints
│   └── ArticlesService
└── tags
    ├── api
    │   ├── TagsEndpoints
    │   └── TagsListResponse
    ├── TagsRepository
    ├── TagsService
    └── TagsServerEndpoints
auth
└── AuthService    
common
├── BaseEndpoints
├── Configuration
├── CustomDecodeFailureHandler 
├── DefectHandler
├── ErrorInfo
├── ErrorMapper
├── Exceptions
├── NoneAsNullOptionEncoder
└── Pagination
db
├── Db
├── DbConfig
└── DbMigrator
users
├── api
│   ├── ProfileResponse
│   ├── UserLoginData
│   ├── UserLoginRequest
│   ├── UserRegisterData
│   ├── UserRegisterRequest
│   ├── UserResponse
│   ├── UsersEndpoints
│   ├── UserUpdateData
│   └── UserUpdateRequest
├── Profile
├── User
├── UsersRepository
├── UsersServerEndpoints
├── UsersService
└── UserWithPassword
Endpoints
Main
```
# Getting started

If you don't have [sbt](https://www.scala-sbt.org) installed already, you can use the provided wrapper script:

```shell
./sbtx -h # shows an usage of a wrapper script
./sbtx compile # build the project
./sbtx test # run the tests
./sbtx run # run the application (Main)
```

For more details check the [sbtx usage](https://github.com/dwijnand/sbt-extras#sbt--h) page.

Otherwise, if sbt is already installed, you can use the standard commands:

```shell
sbt compile # build the project
sbt test # run the tests
sbt run # run the application (Main)
```

Swagger documentation by default is available at URL:
http://localhost:8080/docs

## Links:

* [tapir documentation](https://tapir.softwaremill.com/en/latest/)
* [tapir github](https://github.com/softwaremill/tapir)
* [ZIO documentation](https://zio.dev/)
* [ZIO github](https://github.com/zio/zio)
* [bootzooka: template microservice using tapir](https://softwaremill.github.io/bootzooka/)
* [sbtx wrapper](https://github.com/dwijnand/sbt-extras#installation)


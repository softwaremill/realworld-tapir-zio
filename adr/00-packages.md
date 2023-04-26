# Architecture Decision Record: Package structure

## Context

We need to specify structure for the application. 
It will involve choosing how we will divide code into packages and modules. 
We would like to make a clear decision, what application structure should be going forward.

## Decision

We will divide application by functionalities into packages.

We decided on the following packages:
* `common`
* `db`
* `auth`
* `articles`
* `comments`
* `users`

The `common` package will contain auxiliary structures that will be used in the project.
The `db` package will have the necessary data to operate the database.
The `auth` package will contain the tools needed to authorize the user. 


Packages of `articles`, `comments` and `users` will have a similar structure. 
Each of them will contain specific endpoints, will service them using services and will communicate with the database using repositories.
They will include domain classes and clearly separated api layer that will contain "the contract", i.e. the endpoint descriptions, without logic.
  
Example with one implementation per service:

```
common
├── BaseEndpoints
├── Configuration
├── Pagination
├── ErrorInfo
├── ErrorMapper
├── CustomDecodeFailureHandler
├── DefectHandler
└── Exceptions
db
├── Db
├── DbConfig
└── DbMigrator
auth
└── AuthService
articles
├── api
│   ├── ArticleResponse
│   ├── ArticlesListResponse
│   ├── ArticleCreateData
│   ├── ArticleCreateRequest
│   ├── ArticleUpdateData
│   ├── ArticleUpdateRequest
│   └── ArticlesEndpoints
├── Author
├── Article
├── ArticlesRepository
├── ArticlesService
├── Tag
├── TagsRepository
├── TagsService
└── ArticlesServerEndpoints
comments
├── api
│   ├── CommentResponse
│   ├── CommentsListResponse
│   ├── CommentCreateData
│   ├── CommentCreateRequest
│   ├── CommentUpdateData
│   ├── CommentUpdateRequest
│   └── CommentsEndpoints
├── Author
├── Comment
├── CommentsRepository
├── CommentsService
└── CommentsServerEndpoints
users
├── api
│   ├── ProfileResponse
│   ├── UserResponse
│   ├── UserRegisterData
│   ├── UserRegisterRequest
│   ├── UserLoginData
│   ├── UserLoginRequest
│   ├── UserUpdateData
│   ├── UserUpdateRequest
│   └── UsersEndpoints
├── Profile
├── User
├── UsersRepository
├── UsersService
└── UsersServerEndpoints
Endpoints
Main
```


## Rationale

* The application code is easier to read because of its predictable organization.
* Separate pure description of the api can better showcase tapirs strengths. For example, it can be used to generate client for the api.
* Pure api description, can be easily put into a separate module. This allows other projects to use it as a dependency.
* Pure api description allow browse pure endpoint declarations as a documentation of the API.

## Consequences

* Conforming to new structure will require refactoring the application.

## Conclusion

We decided that, from now on, application will have a new structure. 
New app structure will allow for easier changes in the future, and will better showcase tapirs potential.
# Architecture Decision Record: Package structure

## Context

We need to specify structure for the application. 
It will involve choosing how we will divide code into packages and modules. 
In its current state, application resembles an MVC based architecture. 
We would like to make a clear decision, what application structure should be going forward.

## Decision

We will divide application into layers represented by packages.

We decided on the following layers:
* app
* api
* infrastructure

The app layer will contain domain models and service interfaces. The api layer will contain endpoint descriptions, without logic.
Infrastructure layer will house concrete implementations of infrastructure-specific elements. 
These include database queries based on concrete DB and access library (Quill), and, in the future, possible other technical components like external services, cache, etc.
Each layer can have separate packages inside. Those packages can group models and services that are connected, by a common entity. 
For example, User model, UserRepository interface and UserService interface would be placed in the app layer, 
and could be grouped into a separate user package inside. 
Postgres based implementation of UserRepository would be placed in the infrastructure layer.


## Rationale

* Layered structure will allow to separate interfaces from implementations. This separation, in turn, should allow for easier evolution of the application.
* Separate pure description of the api can better showcase tapirs strengths. For example, it can be used to generate client for the api.
* Pure api description, can be easily put into a separate module. This allows other projects to use it as a dependency.
* New structure would simplify changing underlying infrastructure(database, cache, etc.) in the future.

## Consequences

* Maintaining layered structure requires discipline
* Conforming to new structure will require refactoring the application
* Separating interfaces and implementations puts more burden on the implementors side

## Conclusion

We decided that, from now on, application will have a layered structure. 
Such layered design requires to invest additional effort, but benefits outweigh the costs.
New app structure will allow for easier changes in the future, and will better showcase tapirs potential.
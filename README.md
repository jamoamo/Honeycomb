# Honeycomb

A set of Java libraries supporting services built on the **Adapted Use Cases** architecture.

## The architecture

A **use case** handles a single business capability — Login User, Create User, Find Users. It is the only place
business logic lives.

Everything outside the use case is reached through a **boundary**:

- **Incoming boundaries** are the adapters that call *into* a use case: REST controllers, GraphQL resolvers, scheduled
  tasks, event consumers, message processors.
- **Outgoing boundaries** are the ports a use case calls *out* through: third party APIs, databases, message queues,
  event systems, caches, metric gatherers. The **adapters** implementing those ports are the only code that knows about
  the actual technology.

A use case knows nothing about the adapter that called it and nothing about the adapters behind its ports.

## Modules

| Module                | Contents                                                                  |
|-----------------------|---------------------------------------------------------------------------|
| `honeycomb-bom`       | Bill of materials for consumers                                            |
| `honeycomb-exception` | `HoneycombException`, the base of every exception the libraries throw      |
| `honeycomb-types`     | `ErrorCode`, `ErrorCategory`                                               |
| `honeycomb-boundary`  | `UseCase`, `UseCaseResult`, `UseCaseException`, input validation           |

The libraries are framework free — the only runtime dependency is `slf4j-api`. Framework integrations (Spring, AOP,
Jakarta validation) will arrive as separate adapter modules.

## Writing a use case

```java
public final class FindUserUseCase extends UseCase<FindUserInput, User>
{
   private final UserRepositoryPort users;

   public FindUserUseCase(final UserRepositoryPort users)
   {
      this.users = users;
   }

   @Override
   protected ValidationResult validate(final FindUserInput input)
   {
      if(input.userId() == null)
      {
         return ValidationResult.invalid(new ValidationError("userId", "is required"));
      }
      return ValidationResult.valid();
   }

   @Override
   protected UseCaseResult<User> executeUseCase(final FindUserInput input)
   {
      return this.users.findById(input.userId())
         .map(UseCaseResult::success)
         .orElseGet(() -> UseCaseResult.failure(
            new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND, "No user with that id.")));
   }
}
```

Callers invoke `execute`, which is final and guarantees that:

- the input is non-null and has passed `validate` before `executeUseCase` is reached;
- nothing is thrown — every outcome, including an unanticipated one, comes back as a `UseCaseResult`;
- every failure carries an `ErrorCode` whose `ErrorCategory` lets the incoming adapter decide how to surface it
  without knowing anything about the use case.

## Calling a use case from an incoming boundary

```java
UseCaseResult<User> result = findUser.execute(new FindUserInput(userId));

return result.fold(
   user -> ResponseEntity.ok(toResponse(user)),
   failure -> ResponseEntity.status(statusFor(failure.errorCode().category()))
      .body(toErrorResponse(failure)));
```

## Building

Requires **JDK 25** and Maven 3.8+.

```bash
mvn clean verify
```

Quality gates: Checkstyle (fails the build on any violation) and JaCoCo coverage reporting.

## Licence

MIT — see [LICENSE](LICENSE).

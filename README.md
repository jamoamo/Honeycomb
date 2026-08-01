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

| Module                    | Contents                                                                             |
|---------------------------|---------------------------------------------------------------------------------------|
| `honeycomb-bom`           | Bill of materials for consumers                                                       |
| `honeycomb-exception`     | `HoneycombException`, the base of every exception the libraries throw                 |
| `honeycomb-types`         | `ErrorCode`, `ErrorCategory`                                                          |
| `honeycomb-usecase`       | `UseCase`, `UseCaseResult`, `UseCaseException`, input validation                      |
| `honeycomb-adapter`       | `AdapterArgumentResolver`, `ValueConverter` — the argument-binding contracts every incoming adapter shares |
| `honeycomb-rest`          | REST adapter annotations, routing, argument binding, `ApiResponse`, `ProblemDetail`   |
| `honeycomb-rest-spring`   | Spring WebMVC (Spring Framework 7 / Boot 4) integration for the REST adapter model    |
| `honeycomb-messaging`     | Broker-neutral message consumer adapter annotations, subject routing, argument binding, `MessageOutcome` |
| `honeycomb-nats`          | NATS JetStream binding for the message consumer adapter model                        |
| `honeycomb-telegram`      | Telegram bot adapter annotations, command routing, argument binding, `BotResponse`    |
| `honeycomb-telegram-bots` | `telegrambots` long-polling binding for the Telegram bot adapter model                |

The core libraries are framework free — `honeycomb-rest` reaches the HTTP runtime, `honeycomb-messaging` reaches
the broker, and `honeycomb-telegram` reaches the bot client only through their own seams (`RestRequest`,
`ConsumedMessage`, `BotUpdate`), with `slf4j-api` and Jackson 3 (where a payload needs deserializing) as their
only runtime dependencies. `honeycomb-rest-spring`, `honeycomb-nats` and `honeycomb-telegram-bots` are the
adapter modules that bind those seams to Spring WebMVC, NATS JetStream and `telegrambots` respectively. Further
framework integrations (AOP, Jakarta validation) will arrive as separate adapter modules.

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

## Declaring a REST adapter

An incoming REST boundary is a plain class annotated with `@RestControllerAdapter`; its `@Endpoint` methods bind
path variables, query parameters and the request body, call the use case, and return a value that is wrapped in a
consistent `ApiResponse` envelope. Binding failures surface as a `400` and handling failures as a `500`, both as
RFC 9457 `ProblemDetail` documents with the `application/problem+json` media type.

```java
@RestControllerAdapter(version = "v1", apiPath = "/users")
public final class UserRestAdapter
{
   private final FindUserUseCase findUser;

   public UserRestAdapter(final FindUserUseCase findUser)
   {
      this.findUser = findUser;
   }

   @Endpoint(path = "/{id}", method = HttpVerb.GET)
   public UserResponse find(@PathVariable("id") final UUID id, @QueryParam(value = "expand", required = false) final String expand)
   {
      return findUser.execute(new FindUserInput(id, expand)).fold(this::toResponse, this::surface);
   }
}
```

The adapter above is registered at `GET /api/v1/users/{id}`; `fullPath` replaces the `/api/{version}{apiPath}`
prefix entirely when set. To wire the model into Spring WebMVC, expose two beans from `honeycomb-rest-spring`:

```java
@Bean
public RestControllerAdapterHandlerMapping handlerMapping()
{
   return new RestControllerAdapterHandlerMapping(new PathPatternTemplateParser());
}

@Bean
public RestControllerAdapterHandlerAdapter handlerAdapter(final ObjectMapper objectMapper)
{
   return new RestControllerAdapterHandlerAdapter(new RestControllerAdapterMethodInvoker(objectMapper), objectMapper);
}
```

## Declaring a Telegram bot adapter

An incoming Telegram boundary is a plain class annotated with `@TelegramBotAdapter`; its `@Command` methods
handle a specific bot command and its `@OnUpdate` methods handle any other update of a given kind (plain
messages, edited messages, callback queries). Handler methods bind command arguments, the chat id and the user
id, call the use case, and return a `BotResponse` — or `void`, when no reply is needed.

```java
@TelegramBotAdapter
public final class BookingBotAdapter
{
   private final BookSeatUseCase bookSeat;

   public BookingBotAdapter(final BookSeatUseCase bookSeat)
   {
      this.bookSeat = bookSeat;
   }

   @Command("book")
   public BotResponse book(@CommandArgument(index = 0) final int seat, @ChatId final long chatId)
   {
      return bookSeat.execute(new BookSeatInput(seat, chatId))
         .fold(booking -> BotResponse.reply("Booked seat " + booking.seat() + "."),
            failure -> BotResponse.reply("Sorry, that seat could not be booked."));
   }
}
```

To start the bot over long polling with `honeycomb-telegram-bots`, scan the adapters into a registry and
register the bot's token:

```java
BotAdapterRegistry registry = new BotUpdateScanner().scanAll(adapterBeans);
TelegramBotRegistrar registrar = new TelegramBotRegistrar(new TelegramBotAdapterMethodInvoker());
registrar.register(botToken, registry);
```

`registrar.close()` stops long polling for every bot registered through it.

## Building

Requires **JDK 25** and Maven 3.8+.

```bash
mvn clean verify
```

Quality gates: Checkstyle (fails the build on any violation) and JaCoCo coverage reporting.

## Licence

MIT — see [LICENSE](LICENSE).

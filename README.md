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
| `honeycomb-starter-web`   | Spring Boot starter auto-configuring the Spring WebMVC REST integration               |
| `honeycomb-messaging`     | Broker-neutral message consumer adapter annotations, subject routing, argument binding, `MessageOutcome` |
| `honeycomb-nats`          | NATS JetStream binding for the message consumer adapter model                        |
| `honeycomb-telegram`      | Telegram bot adapter annotations, command routing, argument binding, `BotResponse`    |
| `honeycomb-telegram-bots` | `telegrambots` long-polling binding for the Telegram bot adapter model                |
| `honeycomb-scheduling`    | Scheduler-neutral scheduled task adapter annotations, task scanning, `ScheduledTaskRegistration` |
| `honeycomb-quartz`        | Quartz binding for the scheduled task adapter model                                  |

The core libraries are framework free — `honeycomb-rest` reaches the HTTP runtime, `honeycomb-messaging` reaches
the broker, and `honeycomb-telegram` reaches the bot client only through their own seams (`RestRequest`,
`ConsumedMessage`, `BotUpdate`), with `slf4j-api` and Jackson 3 (where a payload needs deserializing) as their
only runtime dependencies. `honeycomb-scheduling` needs no seam at all: a scheduled method takes no arguments, so
nothing about the scheduler reaches it. `honeycomb-rest-spring`, `honeycomb-nats`, `honeycomb-telegram-bots` and
`honeycomb-quartz` are the adapter modules that bind those seams to Spring WebMVC, NATS JetStream, `telegrambots`
and Quartz respectively. Further framework integrations (AOP, Jakarta validation) will arrive as separate adapter
modules.

## Getting started

Requires **JDK 25** on the consuming project as well — the modules are compiled with `--release 25`.

> **Honeycomb is not yet published to Maven Central.** Until it is, build and install it into your local
> repository first:
>
> ```bash
> git clone https://github.com/jamoamo/honeycomb.git && cd honeycomb && mvn clean install
> ```

Import the BOM so that module versions are managed in one place:

```xml
<dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>io.github.jamoamo.honeycomb</groupId>
         <artifactId>honeycomb-bom</artifactId>
         <version>1.0.0-SNAPSHOT</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
   </dependencies>
</dependencyManagement>
```

Then declare the modules you need, without versions:

```xml
<dependencies>
   <dependency>
      <groupId>io.github.jamoamo.honeycomb</groupId>
      <artifactId>honeycomb-usecase</artifactId>
   </dependency>
   <dependency>
      <groupId>io.github.jamoamo.honeycomb</groupId>
      <artifactId>honeycomb-rest-spring</artifactId>
   </dependency>
</dependencies>
```

Two things worth knowing when picking modules:

- **`honeycomb-usecase` is always declared explicitly.** The adapter modules deliberately do not depend on it — an
  incoming boundary module knows nothing about use cases — so nothing pulls it in transitively.
- **On Spring Boot, `honeycomb-starter-web` replaces `honeycomb-rest-spring`.** It brings the same module with
  it and auto-configures the beans that would otherwise be declared by hand.
- **Each `ext` module brings its own core module with it.** `honeycomb-rest-spring` brings `honeycomb-rest`,
  `honeycomb-nats` brings `honeycomb-messaging`, `honeycomb-telegram-bots` brings `honeycomb-telegram`, and
  `honeycomb-quartz` brings `honeycomb-scheduling`. Declare a core module on its own only when you are binding it
  to a runtime yourself.

`honeycomb-rest-spring` declares `jakarta.servlet-api` as `provided`; the servlet container (or Spring Boot's web
starter) supplies it. Every module that deserializes a payload uses **Jackson 3**, so the `ObjectMapper` in the
examples below is `tools.jackson.databind.ObjectMapper`, not the Jackson 2 class of the same name.

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

## Failure categories

An `ErrorCode` is a `code`, an `ErrorCategory` and a human-readable `description`. The code is the stable,
machine-readable identifier of the specific failure; the category is its protocol-neutral classification, and is
what an incoming adapter switches on to decide how to surface it.

| Category              | Meaning                                                                  |
|-----------------------|--------------------------------------------------------------------------|
| `VALIDATION`          | The input was missing, malformed or failed validation                    |
| `NOT_FOUND`           | The requested entity does not exist                                      |
| `CONFLICT`            | The request conflicts with current state, for example a duplicate create |
| `UNAUTHORISED`        | The caller is not authenticated, or its credentials were not accepted    |
| `FORBIDDEN`           | The caller is authenticated but not permitted to perform the operation   |
| `PRECONDITION_FAILED` | A precondition was not met, for example a version mismatch               |
| `UNAVAILABLE`         | A dependency is unavailable — typically retryable                        |
| `TIMEOUT`             | An operation exceeded its time budget — typically retryable              |
| `RATE_LIMITED`        | A rate or quota limit was exceeded — retryable after a delay              |
| `INTERNAL`            | An unanticipated failure — never retryable without intervention          |

Honeycomb deliberately ships no category-to-protocol mapping. What a `CONFLICT` means over HTTP, over a message
broker or to a bot user is a decision belonging to the adapter, so each adapter maps the categories it cares about
itself. `UseCase` produces three failures of its own, with the codes `UseCase.NULL_INPUT`,
`UseCase.VALIDATION_FAILED` and `UseCase.UNHANDLED_ERROR`.

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
prefix entirely when set.

On Spring Boot, depend on `honeycomb-starter-web` instead of `honeycomb-rest-spring` and there is nothing to
wire — the starter auto-configures the handler mapping and handler adapter (along with the route template
parser and the method invoker they need) in every servlet web application:

```xml
<dependency>
   <groupId>io.github.jamoamo.honeycomb</groupId>
   <artifactId>honeycomb-starter-web</artifactId>
</dependency>
```

Each of those beans is conditional on it being missing, so declaring your own bean of a given type replaces
just that one. Setting `honeycomb.rest.enabled` to `false` registers none of them:

```yaml
honeycomb:
   rest:
      enabled: false
```

The handler mapping is ordered at `1` — just behind `RequestMappingHandlerMapping`, so annotated `@RestController`
routes still win a tie, and well ahead of Spring's static resource mapping, which matches `/**` at
`Ordered.LOWEST_PRECEDENCE - 1`. A mapping ordered behind that one is never consulted and every endpoint surfaces
as a `No static resource ...` error instead. Set `honeycomb.rest.order` to place it elsewhere:

```yaml
honeycomb:
   rest:
      order: -50
```

Without Spring Boot, expose two beans from `honeycomb-rest-spring` yourself:

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

Adapters themselves need no registration beyond being beans: the handler mapping finds every bean in the
application context annotated with `@RestControllerAdapter` and registers its routes at startup.

## Declaring a message consumer adapter

An incoming messaging boundary is a plain class annotated with `@MessageConsumerAdapter`, naming the stream its
subscriptions consume from. Each `@Subscription` method names the subject it handles and the durable consumer
delivering it.

```java
@MessageConsumerAdapter(stream = "ORDERS")
public final class OrderConsumerAdapter
{
   private final RecordOrderUseCase recordOrder;

   public OrderConsumerAdapter(final RecordOrderUseCase recordOrder)
   {
      this.recordOrder = recordOrder;
   }

   @Subscription(subject = "orders.{orderId}.created", durable = "order-created")
   public MessageOutcome onCreated(
      @SubjectToken("orderId") final String orderId,
      @MessageHeader(value = "carrier", required = false) final String carrier,
      final OrderPlaced payload)
   {
      return recordOrder.execute(new RecordOrderInput(orderId, carrier, payload))
         .fold(order -> MessageOutcome.acknowledge(),
            failure -> MessageOutcome.redeliverAfter(Duration.ofSeconds(30)));
   }
}
```

Parameters bind as follows:

- `@SubjectToken` binds a token captured from the subject template — `{orderId}` above.
- `@MessageHeader` binds a message header. An `Optional` parameter is empty when the header is absent, a `List`
  parameter collects every value supplied for the name, and a plain parameter takes `defaultValue()` when absent.
  A required header with no default that is absent makes the message unbindable.
- A `ConsumedMessage` parameter gives the raw message — its subject, headers, payload stream and
  `deliveryAttempt()`.
- **Any other parameter is the payload**, deserialized into its type with Jackson. This is the fallback binding,
  so at most one such parameter may be declared.

Acknowledgement is decided in one place rather than scattered through handler code. A method returning `void` (or
anything that is not a `MessageOutcome`) acknowledges the message; a method returning a `MessageOutcome` decides
for itself between `acknowledge()`, `redeliverAfter(Duration)` and `terminate(String reason)`.

To consume over NATS JetStream with `honeycomb-nats`, scan the adapters and bind the registrations:

```java
List<SubscriptionRegistration> subscriptions = new SubscriptionScanner().scanAll(adapterBeans);

JetStreamSubscriptionBinder binder = new JetStreamSubscriptionBinder(
   connection, new MessageConsumerAdapterMethodInvoker(objectMapper));
binder.bind(subscriptions);
```

Scanning is where a mis-declared adapter is rejected — a subject token with no matching declaration in the
template, more than one payload parameter, or two subscriptions sharing a durable consumer all fail at scan time
rather than on the first message delivered. Durable consumers are **bound to, never created**: a subscription
naming a consumer the broker does not have fails at startup instead of silently consuming nothing, which keeps
the stream and consumer topology owned by whatever provisions the broker. `binder.close()` stops consuming from
every consumer it bound.

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

## Declaring a scheduled task adapter

An incoming scheduling boundary is a plain class annotated with `@ScheduledTaskAdapter`; its `@Scheduled` methods
are invoked on the cadence their cron expression describes. A scheduled method **takes no parameters** — there is
no scheduler context to bind, which is what keeps the model free of any scheduler's types.

```java
@ScheduledTaskAdapter
public final class BookingExpiryAdapter
{
   private final ExpireBookingsUseCase expireBookings;

   public BookingExpiryAdapter(final ExpireBookingsUseCase expireBookings)
   {
      this.expireBookings = expireBookings;
   }

   @Scheduled(cron = "0 0/15 * * * ?", name = "expire-bookings")
   public void expire()
   {
      expireBookings.execute(new ExpireBookingsInput(Instant.now()));
   }
}
```

`name` defaults to the method's name, and must be unique across the adapters scanned together. The cron syntax is
whichever scheduler binds the task — Honeycomb passes the expression through untouched.

To run the tasks on Quartz with `honeycomb-quartz`, scan the adapters and bind them to a scheduler you build and
own:

```java
List<ScheduledTaskRegistration> tasks = new ScheduledTaskScanner().scanAll(adapterBeans);

QuartzScheduleBinder binder = new QuartzScheduleBinder(scheduler, new ScheduledTaskMethodInvoker());
binder.bind(tasks);
scheduler.start();
```

Whether that scheduler runs a `RAMJobStore` for a single instance or a clustered `JDBCJobStore` shared by several
— and so how a cluster agrees on which instance fires a task — is configuration the binder has no opinion on. It
registers every job and trigger in the `honeycomb-scheduling` Quartz group; `binder.close()` unschedules them
again. As with messaging, a mis-declared adapter — a scheduled method with parameters, a blank cron expression,
two tasks sharing a name — fails at scan time rather than when the trigger first fires.

## Extending argument binding

Every incoming adapter binds its handler-method parameters through the two contracts in `honeycomb-adapter`, and
both are open for extension:

- **`AdapterArgumentResolver<C>`** resolves one parameter from whatever the adapter is handling, where `C` is the
  adapter's resolution context (`ArgumentResolutionContext` for REST, `MessageResolutionContext` for messaging).
  Resolvers are consulted in order and the first whose `supports(Parameter)` returns `true` wins, so a catch-all
  resolver — such as the payload resolver in `honeycomb-messaging` — must come last. Each method invoker accepts
  an ordered `List` of resolvers, so new sources of context are added without modifying the invoker:

  ```java
  ValueConverter valueConverter = new DefaultValueConverter();

  MessageConsumerAdapterMethodInvoker invoker = new MessageConsumerAdapterMethodInvoker(
     List.of(new TenantArgumentResolver(),
        new SubjectTokenArgumentResolver(valueConverter),
        new MessageHeaderArgumentResolver(valueConverter),
        new ConsumedMessageArgumentResolver(),
        new MessagePayloadArgumentResolver(objectMapper)));
  ```

  That list is what the single-argument `MessageConsumerAdapterMethodInvoker(ObjectMapper)` constructor builds by
  default, minus the added resolver.

- **`ValueConverter`** converts a raw string — a path variable, a query parameter, a header — to the parameter's
  declared type, and is what the resolvers above are handed. `DefaultValueConverter` covers `String`, the
  primitive and boxed numeric types, `boolean` and `UUID`; supply your own (backed by Spring's conversion
  service, for example) to go further.

## Building

Requires **JDK 25** and Maven 3.8+.

```bash
mvn clean verify
```

Quality gates: Checkstyle (fails the build on any violation) and JaCoCo coverage reporting.

## Licence

MIT — see [LICENSE](LICENSE).

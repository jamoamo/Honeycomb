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
| `honeycomb-auth-jwt`      | JWT `TokenAuthenticator` for REST endpoints, backed by Nimbus JOSE+JWT, with Spring Boot auto-configuration |
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
path variables, query parameters and the request body, call the use case, and return an `EndpointResult`: either
a successful response body, written in a consistent `ApiResponse` envelope, or an error, written as an RFC 9457
`ProblemDetail` document with the `application/problem+json` media type. Binding failures surface as a `400` and
unexpected handling failures as a `500`, also as problem details.

```java
@RestControllerAdapter(version = "v1", apiPath = "/users")
public final class UserRestAdapter
{
   private final FindUserUseCase findUser;
   private final RegisterUserUseCase registerUser;

   public UserRestAdapter(final FindUserUseCase findUser, final RegisterUserUseCase registerUser)
   {
      this.findUser = findUser;
      this.registerUser = registerUser;
   }

   @Endpoint(path = "/{id}", method = HttpVerb.GET)
   public EndpointResult<UserResponse> find(@PathVariable("id") final UUID id)
   {
      return EndpointResult.from(findUser.execute(new FindUserInput(id)), UserResponse::of);
   }

   @Endpoint(path = "/", method = HttpVerb.POST)
   public EndpointResult<UserResponse> register(final RegisterUserRequest request)
   {
      return EndpointResult.respond(
         registerUser.execute(request.toInput()),
         user -> EndpointResult.created(UserResponse.of(user), URI.create("/api/v1/users/" + user.id())));
   }
}
```

The adapter above is registered at `GET /api/v1/users/{id}` and `POST /api/v1/users/`; `fullPath` replaces the
`/api/{version}{apiPath}` prefix entirely when set.

`EndpointResult.from` turns a use case success into a `200 OK` carrying the mapped output, and a failure into an
error identified by its `ErrorCode`. `EndpointResult.respond` does the same but lets the success choose its own
result, such as `created(body, location)`, `accepted(body)` or `noContent()`. Any result can carry extra headers
with `withHeader(name, value)`, and an endpoint can return `EndpointResult.problem(...)` or
`EndpointResult.failed(errorCode)` directly when the error does not come from a use case.

Use case types stay out of the API layer: a `UseCaseResult` is only ever an argument to `from` or `respond`. A
handler method that declares a `UseCaseResult` return type is rejected when its routes are registered.

Errors identified by an `ErrorCode` are turned into problem details by a single `FailureProblemMapper`. The default,
`DefaultFailureProblemMapper`, derives the status from the error category (`VALIDATION` is `400`, `NOT_FOUND` is
`404`, `CONFLICT` is `409`, `INTERNAL` is `500` and so on), uses the error code description as the detail and
carries any validation errors as field errors. Server-side failures report a generic detail and an error id
instead of the description, which is logged against that id. Pass your own mapper to the
`RestControllerAdapterMethodInvoker`, or declare it as a bean with the Spring Boot starter, to change how error
codes surface over HTTP.

On Spring Boot, depend on `honeycomb-starter-web` instead of `honeycomb-rest-spring` and there is nothing to
wire — the starter auto-configures the handler mapping and handler adapter (along with the route template
parser, the method invoker and the `FailureProblemMapper` they need) in every servlet web application:

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

## Securing REST endpoints

REST endpoints are secured by default. The `AuthInterceptor` extracts a bearer token from the `Authorization`
header, validates it with a `TokenAuthenticator`, checks the endpoint's requirements with an `AuthorizationPolicy`
and makes the caller available as an `AuthenticatedPrincipal`. A missing or invalid token is answered with a `401`
and a caller lacking the required scopes or roles with a `403`, both as problem details carrying an RFC 6750
`WWW-Authenticate` header. Why a token was rejected is logged at debug level, never returned.

```java
@RestControllerAdapter(version = "v1", apiPath = "/orders")
@RequiresScopes("orders:read")
public final class OrderRestAdapter
{
   @Endpoint(path = "/{id}", method = HttpVerb.GET)
   public EndpointResult<OrderResponse> find(@PathVariable("id") final UUID id)
   {
      ...
   }

   @Endpoint(path = "/", method = HttpVerb.POST)
   @RequiresScopes({"orders:read", "orders:write"})
   public EndpointResult<OrderResponse> place(
      @CurrentPrincipal final AuthenticatedPrincipal caller,
      final PlaceOrderRequest request)
   {
      ...
   }

   @Endpoint(path = "/health", method = HttpVerb.GET)
   @PermitAll
   public EndpointResult<String> health()
   {
      return EndpointResult.ok("up");
   }
}
```

- `@PermitAll` makes an endpoint public; any credential presented to it is ignored. A `@CurrentPrincipal`
  parameter on a public endpoint must be declared as `Optional<AuthenticatedPrincipal>`.
- `@RequiresScopes` needs all of its scopes by default and `@RequiresRoles` any one of its roles; set `match` to
  `Match.ANY` or `Match.ALL` to change that.
- Each annotation can go on the adapter class, where it applies to every endpoint that doesn't declare its own.
  A method-level requirement overrides a class-level `@PermitAll`, and a method-level `@PermitAll` overrides
  class-level requirements.
- An endpoint with no annotations is open to any authenticated caller.

`TokenAuthenticator` is a port: `honeycomb-auth-jwt` provides a JWT implementation, or implement it yourself.
`CredentialExtractor` (default `BearerTokenExtractor`) and `AuthorizationPolicy` (default
`AnnotationAuthorizationPolicy`) can be replaced as well. Without Spring Boot, add the interceptor to the invoker:

```java
new RestControllerAdapterMethodInvoker(objectMapper, failureMapper, List.of(new AuthInterceptor(authenticator)));
```

With the Spring Boot starter, declare a `TokenAuthenticator` bean and the rest is auto-configured; declare your own
`CredentialExtractor` or `AuthorizationPolicy` bean to replace the default. **If there is no `TokenAuthenticator`
bean the application fails to start**, so that endpoints are never served unsecured by accident. To serve
endpoints without authentication, opt out explicitly:

```yaml
honeycomb:
   rest:
      auth:
         enabled: false
```

> **Upgrading from 1.0:** 1.0 served every endpoint without authentication. From 1.1, applications using the
> starter must either declare a `TokenAuthenticator` bean or set `honeycomb.rest.auth.enabled=false`.

Auth runs as an `EndpointInterceptor` at `AuthInterceptor.ORDER`. Your own interceptor beans run around every
endpoint in `@Order` order, and can read the principal from `invocation.attributes()` under
`AuthenticatedPrincipal.ATTRIBUTE` when they are ordered after it.

### JWT authentication

`honeycomb-auth-jwt` provides `JwtTokenAuthenticator`, backed by Nimbus JOSE+JWT. With Spring Boot, add the module
next to the starter and configure exactly one key source; the authenticator is then auto-configured:

```xml
<dependency>
   <groupId>io.github.jamoamo.honeycomb</groupId>
   <artifactId>honeycomb-auth-jwt</artifactId>
</dependency>
```

```yaml
honeycomb:
   rest:
      auth:
         jwt:
            jwks-uri: https://issuer.example/.well-known/jwks.json
            issuer: https://issuer.example/
            audiences: orders-api
```

| Property (`honeycomb.rest.auth.jwt.`) | Default                     | Meaning                                                                     |
|---------------------------------------|-----------------------------|-----------------------------------------------------------------------------|
| `jwks-uri`                            |                             | Key source: a JWK set, cached and refreshed when a token names an unknown `kid` |
| `public-key`                          |                             | Key source: an RSA or EC public key as a PEM `PUBLIC KEY` or `CERTIFICATE` |
| `public-key-location`                 |                             | Key source: a resource holding that PEM, e.g. `file:/etc/keys/jwt.pem`     |
| `secret`                              |                             | Key source: an HMAC secret (UTF-8, at least 256 bits for `HS256`)          |
| `issuer`                              | any                         | Required `iss`                                                              |
| `audiences`                           | any                         | Accepted `aud` values; the token must name at least one                    |
| `algorithms`                          | by key source               | Accepted algorithms; `RS256` for a JWKS URI or RSA key, the curve's algorithm for an EC key, `HS256` for a secret |
| `clock-skew`                          | `60s`                       | Tolerance when checking `exp` and `nbf`                                    |
| `subject-claim`                       | `sub`                       | Claim holding the subject                                                   |
| `scope-claims`                        | `scope`, `scp`              | Claims holding the scopes, first present wins; a space-separated string or an array |
| `roles-claim`                         | `roles`                     | Claim holding the roles; a space-separated string or an array              |
| `jwks-cache-ttl`                      | `5m`                        | How long a fetched JWK set is cached                                        |
| `jwks-min-refresh-interval`           | `30s`                       | Minimum interval between JWK set fetches; `0` disables the limit           |

Every token must be signed (unsigned and encrypted tokens are rejected) with an accepted algorithm, and must carry
an `exp` that has not passed. Algorithms must suit the key, so an RSA or EC key can never be used as an HMAC secret
and `none` is never accepted. RSA keys must be at least 2048 bits. An unreachable JWKS endpoint is a server-side
failure and is answered with a `500`, not a `401`. Setting more than one key source fails startup, and declaring
your own `TokenAuthenticator` bean replaces the auto-configured one.

Without Spring Boot, build the authenticator directly:

```java
TokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
   .jwksUri(URI.create("https://issuer.example/.well-known/jwks.json"))
   .issuer("https://issuer.example/")
   .audiences("orders-api")
   .build();
```

Pass a `ClaimsMapper` to the builder to derive the principal from the claims differently.

## Idempotent REST endpoints

Mark an endpoint `@Idempotent` and clients can retry it safely by sending the same `X-Idempotency-Key` header:

```java
@Endpoint(path = "/", method = HttpVerb.POST)
@Idempotent(required = true, ttl = "PT24H")
public EndpointResult<OrderResponse> place(final PlaceOrderRequest request)
{
   ...
}
```

The key is scoped to the caller and the endpoint, so two callers can't collide on a key, and the request is
fingerprinted from its method, path and body. Then:

- **First use of a key:** the endpoint runs. A `2xx` response is stored for replay. Any other outcome (a `4xx`, a
  `5xx` or an exception) releases the key so the request can be retried with it.
- **Repeat of a completed request:** the stored status, headers and body are returned with an
  `Idempotent-Replayed: true` header, and the endpoint is not invoked again.
- **Repeat while the first request is still running:** `409 Conflict`.
- **Same key, different request:** `422 Unprocessable Content`.
- **No key:** the endpoint runs normally, unless `required = true`, which answers with a `400`. Keys longer than
  255 characters are also rejected with a `400`.

Keys are remembered for `ttl`, an ISO-8601 duration that defaults to 24 hours. On `@PermitAll` endpoints all
anonymous callers share one scope.

Both the key source and the storage are pluggable. `IdempotencyKeyResolver` defaults to
`HeaderIdempotencyKeyResolver`, which reads `X-Idempotency-Key`; pass another header name or implement your own.
`IdempotencyStore` defaults to `InMemoryIdempotencyStore`, which only holds keys within one application instance;
implement the port over a shared database or cache to make idempotency hold across instances. Its `tryAcquire` must
be atomic. With the Spring Boot starter everything is auto-configured, and declaring your own
`IdempotencyKeyResolver` or `IdempotencyStore` bean replaces the default. Without Spring Boot, add the interceptor
after the auth interceptor:

```java
new RestControllerAdapterMethodInvoker(objectMapper, failureMapper, List.of(
   new AuthInterceptor(authenticator),
   new IdempotencyInterceptor(new InMemoryIdempotencyStore(), objectMapper)));
```

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

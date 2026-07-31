/*
 * The MIT License
 *
 * Copyright 2026 James Amoore.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.jamoamo.honeycomb.messaging;

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.adapter.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.messaging.argument.ConsumedMessageArgumentResolver;
import io.github.jamoamo.honeycomb.messaging.argument.MessageHeaderArgumentResolver;
import io.github.jamoamo.honeycomb.messaging.argument.MessagePayloadArgumentResolver;
import io.github.jamoamo.honeycomb.messaging.argument.MessageResolutionContext;
import io.github.jamoamo.honeycomb.messaging.argument.SubjectTokenArgumentResolver;
import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * Invoker for methods on a message consumer adapter.
 *
 * <p>
 * Arguments are bound from the message by consulting the configured {@link AdapterArgumentResolver}s in order
 * for each parameter, the handler method is invoked, and a {@link MessageOutcome} is produced so that every
 * subscription decides the fate of its message in the same way. A handler method returning a
 * {@code MessageOutcome} decides for itself; any other return value acknowledges the message.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class MessageConsumerAdapterMethodInvoker
{
   private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerAdapterMethodInvoker.class);

   private final List<AdapterArgumentResolver<MessageResolutionContext>> argumentResolvers;

   /**
    * Constructor using the default argument resolvers.
    *
    * @param objectMapper the object mapper used to deserialize message payloads
    */
   public MessageConsumerAdapterMethodInvoker(final ObjectMapper objectMapper)
   {
      this(defaultResolvers(objectMapper));
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters; the first
    *        resolver that supports a parameter resolves it, so a catch-all payload resolver should be last
    */
   public MessageConsumerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<MessageResolutionContext>> argumentResolvers)
   {
      this.argumentResolvers = List.copyOf(argumentResolvers);
   }

   /**
    * Invokes the handler method of the subscription the message was delivered for.
    *
    * @param registration the subscription that delivered the message
    * @param message      the message being handled
    * @return the outcome that should be applied to the message
    * @throws MalformedMessageException if the message cannot be bound to the handler method
    * @throws MessagingException        if the handler method throws or cannot be executed
    */
   public MessageOutcome invokeMessageConsumerAdapterMethod(
      final SubscriptionRegistration registration, final ConsumedMessage message)
   {
      Method handlerMethod = registration.handler().method();
      MessageResolutionContext context = new MessageResolutionContext(message, subjectTokens(registration, message));

      try
      {
         Object[] args = resolveArguments(handlerMethod, context);
         Object result = handlerMethod.invoke(registration.handler().bean(), args);

         return outcomeOf(result);
      }
      catch (final InvocationTargetException ex)
      {
         LOGGER.error("Exception invoking handling method.", ex);
         throw new MessagingException("Error invoking handling method", ex.getCause());
      }
      catch (final IOException | IllegalAccessException ex)
      {
         LOGGER.error("Message consumer adapter method error.", ex);
         throw new MessagingException("Method couldn't be executed", ex);
      }
   }

   private static Map<String, String> subjectTokens(
      final SubscriptionRegistration registration, final ConsumedMessage message)
   {
      return registration.template()
         .match(message.subject())
         .orElseThrow(() -> new MalformedMessageException(
            "Subject '" + message.subject() + "' does not match the subject of the subscription."));
   }

   private static MessageOutcome outcomeOf(final Object result)
   {
      if (result instanceof MessageOutcome outcome)
      {
         return outcome;
      }

      return MessageOutcome.acknowledge();
   }

   private Object[] resolveArguments(final Method handlerMethod, final MessageResolutionContext context)
      throws IOException
   {
      Parameter[] parameters = handlerMethod.getParameters();
      Object[] args = new Object[parameters.length];

      for (int i = 0; i < parameters.length; i++)
      {
         args[i] = resolveArgument(parameters[i], context);
      }

      return args;
   }

   private Object resolveArgument(final Parameter parameter, final MessageResolutionContext context)
      throws IOException
   {
      for (AdapterArgumentResolver<MessageResolutionContext> resolver : argumentResolvers)
      {
         if (resolver.supports(parameter))
         {
            return resolver.resolve(parameter, context);
         }
      }

      throw new IllegalStateException("No argument resolver supports parameter " + parameter + ".");
   }

   private static List<AdapterArgumentResolver<MessageResolutionContext>> defaultResolvers(
      final ObjectMapper objectMapper)
   {
      ValueConverter valueConverter = new DefaultValueConverter();
      return List.of(
         new SubjectTokenArgumentResolver(valueConverter),
         new MessageHeaderArgumentResolver(valueConverter),
         new ConsumedMessageArgumentResolver(),
         new MessagePayloadArgumentResolver(objectMapper));
   }
}

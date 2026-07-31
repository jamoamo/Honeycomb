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

import io.github.jamoamo.honeycomb.messaging.adapter.MessageConsumerAdapter;
import io.github.jamoamo.honeycomb.messaging.adapter.MessageHeader;
import io.github.jamoamo.honeycomb.messaging.adapter.SubjectToken;
import io.github.jamoamo.honeycomb.messaging.adapter.Subscription;
import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers the {@link Subscription} methods of {@link MessageConsumerAdapter} instances and resolves each
 * one into a {@link SubscriptionRegistration}.
 *
 * <p>
 * Scanning is where a mis-declared adapter is rejected: a subscription naming no stream, a subject token with
 * no matching declaration in the subject template, more than one payload parameter, or two subscriptions
 * sharing a durable consumer all fail here rather than on the first message delivered.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class SubscriptionScanner
{
   private final SubjectTemplateParser templateParser;

   /**
    * Constructor using the default subject template parser.
    */
   public SubscriptionScanner()
   {
      this(new DefaultSubjectTemplateParser());
   }

   /**
    * Constructor.
    *
    * @param templateParser the parser used to compile subscription subject templates
    */
   public SubscriptionScanner(final SubjectTemplateParser templateParser)
   {
      this.templateParser = templateParser;
   }

   /**
    * Scans the given adapters.
    *
    * @param adapterBeans the message consumer adapter instances to scan
    * @return the registrations declared across all of the adapters
    */
   public List<SubscriptionRegistration> scanAll(final Collection<?> adapterBeans)
   {
      List<SubscriptionRegistration> registrations = new ArrayList<>();
      Set<String> durables = new HashSet<>();
      for (Object adapterBean : adapterBeans)
      {
         for (SubscriptionRegistration registration : scan(adapterBean))
         {
            if (!durables.add(registration.durable()))
            {
               throw new IllegalStateException(
                  "Duplicate durable consumer registered: " + registration.durable());
            }
            registrations.add(registration);
         }
      }

      return List.copyOf(registrations);
   }

   /**
    * Scans a single adapter.
    *
    * @param adapterBean the message consumer adapter instance to scan
    * @return the registrations declared by the adapter
    */
   public List<SubscriptionRegistration> scan(final Object adapterBean)
   {
      MessageConsumerAdapter adapter = adapterBean.getClass().getAnnotation(MessageConsumerAdapter.class);
      if (adapter == null)
      {
         throw new IllegalStateException(
            "Not a message consumer adapter: " + adapterBean.getClass().getName());
      }

      List<SubscriptionRegistration> registrations = new ArrayList<>();
      for (Method method : adapterBean.getClass().getMethods())
      {
         Subscription subscription = method.getAnnotation(Subscription.class);
         if (subscription != null)
         {
            registrations.add(register(adapterBean, method, adapter, subscription));
         }
      }

      return List.copyOf(registrations);
   }

   private SubscriptionRegistration register(
      final Object adapterBean,
      final Method method,
      final MessageConsumerAdapter adapter,
      final Subscription subscription)
   {
      SubjectTemplate template = templateParser.parse(subscription.subject());
      validateParameters(method, template);

      return new SubscriptionRegistration(
         stream(method, adapter, subscription),
         subscription.durable(),
         template,
         new HandlerMethod(adapterBean, method));
   }

   private static String stream(
      final Method method, final MessageConsumerAdapter adapter, final Subscription subscription)
   {
      if (!subscription.stream().isEmpty())
      {
         return subscription.stream();
      }
      if (!adapter.stream().isEmpty())
      {
         return adapter.stream();
      }

      throw new IllegalStateException(
         "@Subscription methods must name a stream, on the subscription or the adapter: " + method);
   }

   private static void validateParameters(final Method method, final SubjectTemplate template)
   {
      long payloadParameters = 0;
      for (Parameter parameter : method.getParameters())
      {
         validateSubjectToken(method, template, parameter);
         if (isPayloadParameter(parameter))
         {
            payloadParameters++;
         }
      }

      if (payloadParameters > 1)
      {
         throw new IllegalStateException(
            "@Subscription methods may declare at most one payload parameter: " + method);
      }
   }

   private static void validateSubjectToken(
      final Method method, final SubjectTemplate template, final Parameter parameter)
   {
      SubjectToken token = parameter.getAnnotation(SubjectToken.class);
      if (token == null)
      {
         return;
      }

      String name = token.value().isEmpty() ? parameter.getName() : token.value();
      if (!template.tokenNames().contains(name))
      {
         throw new IllegalStateException(
            "Subject token '" + name + "' is not declared by the subject of: " + method);
      }
   }

   private static boolean isPayloadParameter(final Parameter parameter)
   {
      return !parameter.isAnnotationPresent(SubjectToken.class)
         && !parameter.isAnnotationPresent(MessageHeader.class)
         && parameter.getType() != ConsumedMessage.class;
   }
}

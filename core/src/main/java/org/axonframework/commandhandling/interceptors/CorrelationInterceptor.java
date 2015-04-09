/*
 * Copyright (c) 2010-2014. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.commandhandling.interceptors;

import org.axonframework.commandhandling.CommandHandlerInterceptor;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.InterceptorChain;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.EventMessage;
import org.axonframework.eventsourcing.EventSourcedAggregateRoot;
import org.axonframework.eventsourcing.EventStreamDecorator;
import org.axonframework.unitofwork.UnitOfWork;
import org.axonframework.unitofwork.UnitOfWorkListenerAdapter;

import java.util.Collections;

/**
 * Command handling interceptor and event stream decorator in one.
 * Doing so makes is possible to check if a command is already handled.
 * <p/>
 * http://issues.axonframework.org/youtrack/issue/AXON-94
 *
 * @author Steven van Beelen
 * @author Rommert de Bruijn
 * @author Patrick Kik
 * @since 2.5
 */
public class CorrelationInterceptor implements CommandHandlerInterceptor, EventStreamDecorator {

    // TODO Make the key of the correlation id configurable
    static final String CORRELATION_ID = "correlationId";
    private static final String COMMAND_NAME = CorrelationInterceptor.class.getName() + "_commandName";

    @Override
    public Object handle(final CommandMessage<?> command, UnitOfWork unitOfWork, InterceptorChain chain) throws Throwable {
        unitOfWork.registerListener(new CorrelationListener(command));

        unitOfWork.attachResource(CORRELATION_ID, command.getIdentifier());
        unitOfWork.attachResource(COMMAND_NAME, command.getCommandName());

        return chain.proceed();
    }

    @Override
    public DomainEventStream decorateForRead(String aggregateType, Object aggregateIdentifier, DomainEventStream eventStream) {
        // TODO Map bijhouden van events. concurrent map key = aggregateId, value = soft reference list of (event.timestamp, event.correlationId)
        // TODO Toegang tot case moet er ook bij. Zie CacheListener.
        return null;
    }

    @Override
    public DomainEventStream decorateForAppend(String aggregateType, EventSourcedAggregateRoot aggregate, DomainEventStream eventStream) {
        return eventStream;
    }

    /**
     * Unit of work listener that adds the command id to the event meta data.
     */
    private static class CorrelationListener extends UnitOfWorkListenerAdapter {
        private final CommandMessage<?> command;

        public CorrelationListener(CommandMessage<?> command) {
            this.command = command;
        }

        @Override
        public <T> EventMessage<T> onEventRegistered(UnitOfWork unitOfWork, EventMessage<T> event) {
            return event.andMetaData(Collections.singletonMap(CORRELATION_ID, command.getIdentifier()));
        }

    }
}

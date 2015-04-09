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

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.annotation.AggregateAnnotationCommandHandler;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.MetaData;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.AggregateFactory;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventstore.EventStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Steven van Beelen
 * @author Rommert de Bruijn
 * @author Patrick Kik
 * @since 2.5
 */
public class CorrelationInterceptorTest {

    private CorrelationInterceptor subject;

    private SimpleCommandBus commandBus;
    private EventSourcingRepository eventSourcingRepository;
    private EventStore mockEventStore;

    @Before
    public void setUp() throws Exception {
        subject = new CorrelationInterceptor();

        AggregateFactory mockAggregateFactory = mock(AggregateFactory.class);
        when(mockAggregateFactory.getAggregateType()).thenReturn(StubAggregate.class);

        mockEventStore = mock(EventStore.class);

        eventSourcingRepository = new EventSourcingRepository(mockAggregateFactory, mockEventStore);
        eventSourcingRepository.setEventBus(mock(EventBus.class));

        commandBus = new SimpleCommandBus();
        commandBus.setHandlerInterceptors(Arrays.asList(subject));
    }

    @Test
    public void testEventHasCorrelationId() throws Throwable {
        AggregateAnnotationCommandHandler.subscribe(StubAggregate.class, eventSourcingRepository, commandBus);
        CommandMessage<?> commandMessage = new GenericCommandMessage<Object>("<payload/>");
        commandBus.dispatch(commandMessage);

        ArgumentCaptor<DomainEventStream> argumentCaptor = ArgumentCaptor.forClass(DomainEventStream.class);
        verify(mockEventStore).appendEvents(anyString(), argumentCaptor.capture());
        MetaData metaData = argumentCaptor.getValue().next().getMetaData();
        assertEquals(commandMessage.getIdentifier(), metaData.get(subject.CORRELATION_ID));
    }

    public static class StubAggregate extends AbstractAnnotatedAggregateRoot<String> {

        @AggregateIdentifier
        private String id;

        @CommandHandler
        public StubAggregate(String stubCommand) {
            apply(stubCommand);
        }

        public StubAggregate() {
        }

        @EventSourcingHandler
        public void handle(String id) {
            this.id = id;
        }

    }

}

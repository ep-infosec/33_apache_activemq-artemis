/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.transport.fanout;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS;
import org.apache.activemq.broker.artemiswrapper.OpenwireArtemisBaseTest;
import org.apache.activemq.util.MessageIdList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FanoutTest extends OpenwireArtemisBaseTest {

   EmbeddedJMS[] servers = new EmbeddedJMS[2];

   ActiveMQConnectionFactory producerFactory = new ActiveMQConnectionFactory("fanout:(static:(tcp://localhost:61616,tcp://localhost:61617))?fanOutQueues=true");
   Connection producerConnection;
   Session producerSession;
   int messageCount = 100;

   @Before
   public void setUp() throws Exception {
      setUpNonClusterServers(servers);

      producerConnection = producerFactory.createConnection();
      producerConnection.start();
      producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
   }

   @After
   public void tearDown() throws Exception {
      producerSession.close();
      producerConnection.close();

      shutDownNonClusterServers(servers);
   }

   @Test
   public void testSendReceive() throws Exception {

      MessageProducer prod = createProducer();
      for (int i = 0; i < messageCount; i++) {
         Message msg = producerSession.createTextMessage("Message " + i);
         prod.send(msg);
      }
      prod.close();

      assertMessagesReceived("tcp://localhost:61616");
      assertMessagesReceived("tcp://localhost:61617");
   }

   protected MessageProducer createProducer() throws Exception {
      return producerSession.createProducer(producerSession.createQueue("TEST"));
   }

   protected void assertMessagesReceived(String brokerURL) throws Exception {
      ActiveMQConnectionFactory consumerFactory = new ActiveMQConnectionFactory(brokerURL);
      Connection consumerConnection = consumerFactory.createConnection();
      consumerConnection.start();
      Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer consumer = consumerSession.createConsumer(consumerSession.createQueue("TEST"));
      MessageIdList listener = new MessageIdList();
      consumer.setMessageListener(listener);
      listener.waitForMessagesToArrive(messageCount);
      listener.assertMessagesReceived(messageCount);

      consumer.close();
      consumerSession.close();
      consumerConnection.close();
   }
}

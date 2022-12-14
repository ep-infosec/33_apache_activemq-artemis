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
package org.apache.activemq.transport.failover;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.net.URI;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.jms.server.config.impl.JMSConfigurationImpl;
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS;
import org.apache.activemq.broker.artemiswrapper.OpenwireArtemisBaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FailoverTimeoutTest extends OpenwireArtemisBaseTest {

   private static final Logger LOG = LoggerFactory.getLogger(FailoverTimeoutTest.class);

   private static final String QUEUE_NAME = "test.failovertimeout";
   EmbeddedJMS server;
   URI tcpUri;

   @Before
   public void setUp() throws Exception {
      Configuration config = createConfig(0);
      server = new EmbeddedJMS().setConfiguration(config).setJmsConfiguration(new JMSConfigurationImpl());
      server.start();
      tcpUri = new URI(newURI(0));
   }

   @After
   public void tearDown() throws Exception {
      if (server != null) {
         server.stop();
      }
   }

   @Test
   public void testTimoutDoesNotFailConnectionAttempts() throws Exception {
      server.stop();
      long timeout = 1000;

      long startTime = System.currentTimeMillis();

      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("failover:(" + tcpUri + ")" +
                                                                      "?timeout=" + timeout + "&useExponentialBackOff=false" +
                                                                      "&maxReconnectAttempts=5" + "&initialReconnectDelay=1000");
      Connection connection = cf.createConnection();
      try {
         connection.start();
         fail("Should have failed to connect");
      } catch (JMSException ex) {
         LOG.info("Caught exception on call to start: {}", ex.getMessage());
      }

      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      LOG.info("Time spent waiting to connect: {} ms", duration);

      assertTrue(duration > 3000);
   }

   @Test
   public void testTimeout() throws Exception {

      long timeout = 1000;
      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("failover:(" + tcpUri + ")?timeout=" + timeout + "&useExponentialBackOff=false");
      Connection connection = cf.createConnection();
      try {
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         MessageProducer producer = session.createProducer(session.createQueue(QUEUE_NAME));
         TextMessage message = session.createTextMessage("Test message");
         producer.send(message);

         server.stop();

         try {
            producer.send(message);
         } catch (JMSException jmse) {
            assertEquals("Failover timeout of " + timeout + " ms reached.", jmse.getMessage());
         }

         Configuration config = createConfig(0);
         server = new EmbeddedJMS().setConfiguration(config).setJmsConfiguration(new JMSConfigurationImpl());
         server.start();

         producer.send(message);

         server.stop();
         server = null;
      } finally {
         if (connection != null) {
            connection.close();
         }
      }
   }

   @Test
   public void testUpdateUris() throws Exception {

      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("failover:(" + tcpUri + ")?useExponentialBackOff=false");
      ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();
      try {
         connection.start();
         FailoverTransport failoverTransport = connection.getTransport().narrow(FailoverTransport.class);

         URI[] bunchOfUnknownAndOneKnown = new URI[]{new URI("tcp://unknownHost:" + tcpUri.getPort()), new URI("tcp://unknownHost2:" + tcpUri.getPort()), new URI("tcp://localhost:2222")};
         failoverTransport.add(false, bunchOfUnknownAndOneKnown);
      } finally {
         if (connection != null) {
            connection.close();
         }
      }
   }
}

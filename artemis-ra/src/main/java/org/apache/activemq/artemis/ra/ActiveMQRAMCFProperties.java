/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.ra;

import javax.jms.Queue;
import javax.jms.Topic;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * The MCF default properties - these are set in the tx-connection-factory at the jms-ds.xml
 */
public class ActiveMQRAMCFProperties extends ConnectionFactoryProperties implements Serializable {

   private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

   /**
    * Serial version UID
    */
   static final long serialVersionUID = -5951352236582886862L;

   /**
    * The queue type
    */
   private static final String QUEUE_TYPE = Queue.class.getName();

   /**
    * The topic type
    */
   private static final String TOPIC_TYPE = Topic.class.getName();
   /**
   * If true then for outbound connections a local tx will be used if no JTA is configured
   */
   private boolean allowLocalTransactions;

   /**
    * If true then for outbound connections will always assume that they are part of a transaction.
    * This is helpful when running in containers where access to the Transaction manager can't be configured
    */
   private boolean inJtaTransaction;

   private String strConnectorClassName;

   public String strConnectionParameters;

   /**
    * The connection type
    */
   private int type = ActiveMQRAConnectionFactory.CONNECTION;

   /**
    * Use tryLock
    */
   private Integer useTryLock;

   /**
    * Constructor
    */
   public ActiveMQRAMCFProperties() {
      logger.trace("constructor()");

      useTryLock = null;
   }

   /**
    * Get the connection type
    *
    * @return The type
    */
   public int getType() {
      logger.trace("getType()");

      return type;
   }

   public String getConnectorClassName() {
      return strConnectorClassName;
   }

   public void setConnectorClassName(final String connectorClassName) {
      logger.trace("setConnectorClassName({})", connectorClassName);

      strConnectorClassName = connectorClassName;

      setParsedConnectorClassNames(ActiveMQRaUtils.parseConnectorConnectorConfig(connectorClassName));
   }

   /**
    * @return the connectionParameters
    */
   public String getStrConnectionParameters() {
      return strConnectionParameters;
   }

   public void setConnectionParameters(final String configuration) {
      strConnectionParameters = configuration;
      setParsedConnectionParameters(ActiveMQRaUtils.parseConfig(configuration));
   }

   /**
    * Set the default session type.
    *
    * @param defaultType either javax.jms.Topic or javax.jms.Queue
    */
   public void setSessionDefaultType(final String defaultType) {
      logger.trace("setSessionDefaultType({})", type);

      if (defaultType.equals(ActiveMQRAMCFProperties.QUEUE_TYPE)) {
         type = ActiveMQRAConnectionFactory.QUEUE_CONNECTION;
      } else if (defaultType.equals(ActiveMQRAMCFProperties.TOPIC_TYPE)) {
         type = ActiveMQRAConnectionFactory.TOPIC_CONNECTION;
      } else {
         type = ActiveMQRAConnectionFactory.CONNECTION;
      }
   }

   /**
    * Get the default session type.
    *
    * @return The default session type
    */
   public String getSessionDefaultType() {
      logger.trace("getSessionDefaultType()");

      if (type == ActiveMQRAConnectionFactory.CONNECTION) {
         return "BOTH";
      } else if (type == ActiveMQRAConnectionFactory.QUEUE_CONNECTION) {
         return ActiveMQRAMCFProperties.TOPIC_TYPE;
      } else {
         return ActiveMQRAMCFProperties.QUEUE_TYPE;
      }
   }

   /**
    * Get the useTryLock.
    *
    * @return the useTryLock.
    */
   public Integer getUseTryLock() {
      logger.trace("getUseTryLock()");

      return useTryLock;
   }

   /**
    * Set the useTryLock.
    *
    * @param useTryLock the useTryLock.
    */
   public void setUseTryLock(final Integer useTryLock) {
      logger.trace("setUseTryLock({})", useTryLock);

      this.useTryLock = useTryLock;
   }

   public boolean isAllowLocalTransactions() {
      return allowLocalTransactions;
   }

   public void setAllowLocalTransactions(boolean allowLocalTransactions) {
      this.allowLocalTransactions = allowLocalTransactions;
   }

   public boolean isInJtaTransaction() {
      return inJtaTransaction;
   }

   public void setInJtaTransaction(boolean inJtaTransaction) {
      this.inJtaTransaction = inJtaTransaction;
   }
}

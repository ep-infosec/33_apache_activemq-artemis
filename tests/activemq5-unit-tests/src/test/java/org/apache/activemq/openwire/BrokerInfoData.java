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
package org.apache.activemq.openwire;

import org.apache.activemq.command.BrokerId;
import org.apache.activemq.command.BrokerInfo;

public class BrokerInfoData extends DataFileGenerator {

   @Override
   protected Object createObject() {
      BrokerInfo rc = new BrokerInfo();
      rc.setResponseRequired(false);
      rc.setBrokerName("localhost");
      rc.setBrokerURL("tcp://localhost:61616");
      rc.setBrokerId(new BrokerId("ID:1289012830123"));
      rc.setCommandId((short) 12);
      rc.setResponseRequired(false);
      return rc;
   }

}

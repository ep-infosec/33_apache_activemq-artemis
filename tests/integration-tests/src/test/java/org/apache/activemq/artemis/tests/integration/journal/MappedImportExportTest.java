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
package org.apache.activemq.artemis.tests.integration.journal;

import org.apache.activemq.artemis.core.io.SequentialFileFactory;
import org.apache.activemq.artemis.core.io.mapped.MappedSequentialFileFactory;

public class MappedImportExportTest extends NIOImportExportTest {

   @Override
   protected void setup(int minFreeFiles, int fileSize, boolean sync) {
      super.setup(minFreeFiles, fileSize, sync);
      ((MappedSequentialFileFactory) this.fileFactory).capacity(fileSize);
   }

   @Override
   protected void setup(int minFreeFiles, int fileSize, boolean sync, int maxAIO) {
      super.setup(minFreeFiles, fileSize, sync, maxAIO);
      ((MappedSequentialFileFactory) this.fileFactory).capacity(fileSize);
   }

   @Override
   protected void setup(int minFreeFiles, int poolSize, int fileSize, boolean sync, int maxAIO) {
      super.setup(minFreeFiles, poolSize, fileSize, sync, maxAIO);
      ((MappedSequentialFileFactory) this.fileFactory).capacity(fileSize);
   }

   @Override
   protected SequentialFileFactory getFileFactory() throws Exception {
      return new MappedSequentialFileFactory(getTestDirfile(), 10 * 4096, false, 0, 0, null);
   }
}

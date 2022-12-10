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
package org.apache.activemq.artemis.message;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionReceiveMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.reader.TextMessageUtil;
import org.apache.activemq.artemis.utils.Base64;
import org.apache.activemq.artemis.utils.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class CoreMessageTest {

   public static final SimpleString ADDRESS = new SimpleString("this.local.address");
   public static final byte MESSAGE_TYPE = Message.TEXT_TYPE;
   public static final boolean DURABLE = true;
   public static final long EXPIRATION = 123L;
   public static final long TIMESTAMP = 321L;
   public static final byte PRIORITY = (byte) 3;
   public static final String TEXT = "hi";
   public static final String BIGGER_TEXT = "AAAAAAAAAAAAAAAAAAAAAAAAA ASDF ASDF ASF ASD ASF ASDF ASDF ASDF ASF ADSF ASDF";
   public static final String SMALLER_TEXT = "H";
   public static final UUID uuid = new UUID(UUID.TYPE_TIME_BASED, new byte[]{0, 0, 0, 0,
      0, 0, 0, 0,
      0, 0, 0, 0,
      0, 0, 0, 1});
   public static final SimpleString PROP1_NAME = new SimpleString("t1");
   public static final SimpleString PROP1_VALUE = new SimpleString("value-t1");

   /**
    * This encode was generated by {@link #generate()}.
    * Run it manually with a right-click on the IDE to eventually update it
    * */
   // body = "hi";
   private final String STRING_ENCODE = "AAAAFgEAAAAEaABpAAAAAAAAAAAAAQAAACR0AGgAaQBzAC4AbABvAGMAYQBsAC4AYQBkAGQAcgBlAHMAcwAAAwEAAAAAAAAAewAAAAAAAAFBAwEAAAABAAAABHQAMQAKAAAAEHYAYQBsAHUAZQAtAHQAMQA=";

   private ByteBuf BYTE_ENCODE;


   @Before
   public void before() {
      BYTE_ENCODE = Unpooled.wrappedBuffer(Base64.decode(STRING_ENCODE, Base64.DONT_BREAK_LINES | Base64.URL_SAFE));
      // some extra caution here, nothing else, to make sure we would get the same encoding back
      Assert.assertEquals(STRING_ENCODE, encodeString(BYTE_ENCODE.array()));
      BYTE_ENCODE.readerIndex(0).writerIndex(BYTE_ENCODE.capacity());
   }

   /** The message is received, then sent to the other side untouched */
   @Test
   public void testPassThrough() {
      CoreMessage decodedMessage = decodeMessage();

      Assert.assertEquals(TEXT, TextMessageUtil.readBodyText(decodedMessage.getReadOnlyBodyBuffer()).toString());
   }

   @Test
   public void testBodyBufferSize() {
      final CoreMessage decodedMessage = decodeMessage();
      final int bodyBufferSize = decodedMessage.getBodyBufferSize();
      final int readonlyBodyBufferReadableBytes = decodedMessage.getReadOnlyBodyBuffer().readableBytes();
      Assert.assertEquals(bodyBufferSize, readonlyBodyBufferReadableBytes);
   }

   /** The message is received, then sent to the other side untouched */
   @Test
   public void sendThroughPackets() {
      CoreMessage decodedMessage = decodeMessage();

      int encodeSize = decodedMessage.getEncodeSize();
      Assert.assertEquals(BYTE_ENCODE.capacity(), encodeSize);

      SessionSendMessage sendMessage = new SessionSendMessage(decodedMessage, true, null);
      sendMessage.setChannelID(777);

      ActiveMQBuffer buffer = sendMessage.encode(null);

      byte[] byteArray = buffer.byteBuf().array();
      //System.out.println("Sending " + ByteUtil.bytesToHex(buffer.toByteBuffer().array(), 1) + ", bytes = " + byteArray.length);

      buffer.readerIndex(5);

      SessionSendMessage sendMessageReceivedSent = new SessionSendMessage(new CoreMessage());

      sendMessageReceivedSent.decode(buffer);

      Assert.assertEquals(encodeSize, sendMessageReceivedSent.getMessage().getEncodeSize());

      Assert.assertTrue(sendMessageReceivedSent.isRequiresResponse());

      Assert.assertEquals(TEXT, TextMessageUtil.readBodyText(sendMessageReceivedSent.getMessage().getReadOnlyBodyBuffer()).toString());
   }

   /** The message is received, then sent to the other side untouched */
   @Test
   public void sendThroughPacketsClient() {
      CoreMessage decodedMessage = decodeMessage();

      int encodeSize = decodedMessage.getEncodeSize();
      Assert.assertEquals(BYTE_ENCODE.capacity(), encodeSize);

      SessionReceiveMessage sendMessage = new SessionReceiveMessage(33, decodedMessage, 7);
      sendMessage.setChannelID(777);

      ActiveMQBuffer buffer = sendMessage.encode(null);

      buffer.readerIndex(5);

      SessionReceiveMessage sendMessageReceivedSent = new SessionReceiveMessage(new CoreMessage());

      sendMessageReceivedSent.decode(buffer);

      Assert.assertEquals(33, sendMessageReceivedSent.getConsumerID());

      Assert.assertEquals(7, sendMessageReceivedSent.getDeliveryCount());

      Assert.assertEquals(encodeSize, sendMessageReceivedSent.getMessage().getEncodeSize());

      Assert.assertEquals(TEXT, TextMessageUtil.readBodyText(sendMessageReceivedSent.getMessage().getReadOnlyBodyBuffer()).toString());
   }

   private CoreMessage decodeMessage() {

      ByteBuf newBuffer = Unpooled.buffer(BYTE_ENCODE.capacity());
      newBuffer.writeBytes(BYTE_ENCODE, 0, BYTE_ENCODE.writerIndex());

      CoreMessage coreMessage = internalDecode(newBuffer);

      int encodeSize = coreMessage.getEncodeSize();

      Assert.assertEquals(newBuffer.capacity(), encodeSize);

      Assert.assertEquals(ADDRESS, coreMessage.getAddressSimpleString());

      Assert.assertEquals(PROP1_VALUE.toString(), coreMessage.getStringProperty(PROP1_NAME));

      ByteBuf destinedBuffer = Unpooled.buffer(BYTE_ENCODE.array().length);
      coreMessage.sendBuffer(destinedBuffer, 0);

      byte[] destinedArray = destinedBuffer.array();
      byte[] sourceArray = BYTE_ENCODE.array();

      CoreMessage newDecoded = internalDecode(Unpooled.wrappedBuffer(destinedArray));

      Assert.assertEquals(encodeSize, newDecoded.getEncodeSize());

      Assert.assertArrayEquals(sourceArray, destinedArray);

      return coreMessage;
   }

   private CoreMessage internalDecode(ByteBuf bufferOrigin) {
      CoreMessage coreMessage = new CoreMessage();
//      System.out.println("Bytes from test " + ByteUtil.bytesToHex(bufferOrigin.array(), 1));
      coreMessage.receiveBuffer(bufferOrigin);
      return coreMessage;
   }

   /** The message is received, then sent to the other side untouched */
   @Test
   public void testChangeBodyStringSameSize() {
      testChangeBodyString(TEXT.toUpperCase());
   }

   @Test
   public void testChangeBodyBiggerString() {
      testChangeBodyString(BIGGER_TEXT);
   }

   @Test
   public void testGenerateEmpty() {
      CoreMessage empty = new CoreMessage().initBuffer(100);
      ByteBuf buffer = Unpooled.buffer(200);
      empty.sendBuffer(buffer, 0);

      CoreMessage empty2 = new CoreMessage();
      empty2.receiveBuffer(buffer);

      try {
         empty2.getBodyBuffer().readLong();
         Assert.fail("should throw exception");
      } catch (Exception expected) {

      }
   }

   @Test
   public void testToMapLimit() throws Exception {

      CoreMessage coreMessage = new CoreMessage().initBuffer(BIGGER_TEXT.length() * 2);
      coreMessage.putStringProperty("prop", BIGGER_TEXT);
      coreMessage.putBytesProperty("bytesProp", BIGGER_TEXT.getBytes(StandardCharsets.UTF_8));

      Assert.assertEquals(BIGGER_TEXT.length(), ((String)coreMessage.toMap().get("prop")).length());
      Assert.assertEquals(BIGGER_TEXT.getBytes(StandardCharsets.UTF_8).length, ((byte[])coreMessage.toMap().get("bytesProp")).length);

      // limit the values
      Assert.assertNotEquals(BIGGER_TEXT.getBytes(StandardCharsets.UTF_8).length, ((byte[])coreMessage.toMap(40).get("bytesProp")).length);
      String mapVal = ((String)coreMessage.toMap(40).get("prop"));
      Assert.assertNotEquals(BIGGER_TEXT.length(), mapVal.length());
      Assert.assertTrue(mapVal.contains("more"));

      mapVal = ((String)coreMessage.toMap(0).get("prop"));
      Assert.assertNotEquals(BIGGER_TEXT.length(), mapVal.length());
      Assert.assertTrue(mapVal.contains("more"));

      Assert.assertEquals(BIGGER_TEXT.length(), Integer.parseInt(mapVal.substring(mapVal.lastIndexOf('+') + 1, mapVal.lastIndexOf('m')).trim()));

      Assert.assertEquals(BIGGER_TEXT.getBytes(StandardCharsets.UTF_8).length, ((byte[])coreMessage.toPropertyMap().get("bytesProp")).length);
      Assert.assertNotEquals(BIGGER_TEXT.getBytes(StandardCharsets.UTF_8).length, ((byte[])coreMessage.toPropertyMap(40).get("bytesProp")).length);
   }

   @Test
   public void testSaveReceiveLimitedBytes() {
      CoreMessage empty = new CoreMessage().initBuffer(100);
      empty.getBodyBuffer().writeByte((byte)7);

      ByteBuf buffer = Unpooled.buffer(200);
      empty.sendBuffer(buffer, 0);

      CoreMessage empty2 = new CoreMessage();
      empty2.receiveBuffer(buffer);

      Assert.assertEquals((byte)7, empty2.getBodyBuffer().readByte());

      try {
         empty2.getBodyBuffer().readByte();
         Assert.fail("should throw exception");
      } catch (Exception expected) {

      }
   }

   @Test
   public void testChangeBodySmallerString() {
      testChangeBodyString(SMALLER_TEXT);
   }

   public void testChangeBodyString(String newString) {
      CoreMessage coreMessage = decodeMessage();

      coreMessage.putStringProperty("newProperty", "newValue");
      ActiveMQBuffer legacyBuffer = coreMessage.getBodyBuffer();
      legacyBuffer.resetWriterIndex();
      legacyBuffer.clear();

      TextMessageUtil.writeBodyText(legacyBuffer, SimpleString.toSimpleString(newString));

      ByteBuf newbuffer = Unpooled.buffer(150000);

      coreMessage.sendBuffer(newbuffer, 0);
      newbuffer.readerIndex(0);

      CoreMessage newCoreMessage = new CoreMessage();
      newCoreMessage.receiveBuffer(newbuffer);


      SimpleString newText = TextMessageUtil.readBodyText(newCoreMessage.getReadOnlyBodyBuffer());

      Assert.assertEquals(newString, newText.toString());

//      coreMessage.putStringProperty()
   }

   @Test
   public void testPassThroughMultipleThreads() throws Throwable {
      CoreMessage coreMessage = new CoreMessage();
      coreMessage.receiveBuffer(BYTE_ENCODE);

      LinkedList<Throwable> errors = new LinkedList<>();

      Thread[] threads = new Thread[50];
      for (int i = 0; i < threads.length; i++) {
         threads[i] = new Thread(() -> {
            try {
               for (int j = 0; j < 50; j++) {
                  Assert.assertEquals(ADDRESS, coreMessage.getAddressSimpleString());
                  Assert.assertEquals(PROP1_VALUE.toString(), coreMessage.getStringProperty(PROP1_NAME));

                  ByteBuf destinedBuffer = Unpooled.buffer(BYTE_ENCODE.array().length);
                  coreMessage.sendBuffer(destinedBuffer, 0);

                  byte[] destinedArray = destinedBuffer.array();
                  byte[] sourceArray = BYTE_ENCODE.array();

                  Assert.assertArrayEquals(sourceArray, destinedArray);

                  Assert.assertEquals(TEXT, TextMessageUtil.readBodyText(coreMessage.getReadOnlyBodyBuffer()).toString());
               }
            } catch (Throwable e) {
               e.printStackTrace();
               errors.add(e);
            }
         });
      }

      for (Thread t : threads) {
         t.start();
      }

      for (Thread t : threads) {
         t.join();
      }

      for (Throwable e: errors) {
         throw e;
      }

   }

   // This is to compare the original encoding with the current version
   @Test
   public void compareOriginal() throws Exception {
      String generated = generate(TEXT);

      Assert.assertEquals(STRING_ENCODE, generated);

      for (int i = 0; i < generated.length(); i++) {
         Assert.assertEquals("Chart at " + i + " was " + generated.charAt(i) + " instead of " + STRING_ENCODE.charAt(i), generated.charAt(i), STRING_ENCODE.charAt(i));
      }
   }

   /** Use this method to update the encode for the known message */
   @Ignore
   @Test
   public void generate() throws Exception {

      printVariable(TEXT, generate(TEXT));
      printVariable(SMALLER_TEXT, generate(SMALLER_TEXT));
      printVariable(BIGGER_TEXT, generate(BIGGER_TEXT));

   }

   @Test
   public void testMemoryEstimateChangedAfterModifiedAlreadyEncodedCopy() {
      final CoreMessage msg = new CoreMessage(1, 44);
      msg.getEncodeSize();
      final int memoryEstimate = msg.getMemoryEstimate();
      final CoreMessage copy = (CoreMessage) msg.copy(2);
      copy.getEncodeSize();
      copy.putBytesProperty(Message.HDR_ROUTE_TO_IDS, new byte[Long.BYTES]);
      final int increasedMemoryFootprint = copy.getMemoryEstimate() - memoryEstimate;
      final int increasedPropertyFootprint = copy.getProperties().getMemoryOffset() - msg.getProperties().getMemoryOffset();
      assertThat("memory estimation isn't accounting for the additional encoded property",
                 increasedMemoryFootprint, greaterThan(increasedPropertyFootprint));
   }

   @Test
   public void testMessageBufferCapacityMatchEncodedSizeAfterModifiedCopy() {
      final CoreMessage msg = new CoreMessage(1, 4155);
      msg.setAddress("a");
      msg.putBytesProperty("_", new byte[4096]);
      final CoreMessage copy = (CoreMessage) msg.copy(2);
      Assert.assertEquals(msg.getEncodeSize(), copy.getBuffer().capacity());
      copy.setAddress("b");
      copy.setBrokerProperty(Message.HDR_ORIGINAL_QUEUE, "a");
      copy.setBrokerProperty(Message.HDR_ORIGINAL_ADDRESS, msg.getAddressSimpleString());
      copy.setBrokerProperty(Message.HDR_ORIG_MESSAGE_ID, msg.getMessageID());
      Assert.assertEquals(copy.getEncodeSize(), copy.getBuffer().capacity());
   }

   private void printVariable(String body, String encode) {
      System.out.println("// body = \"" + body + "\";");
      System.out.println("private final String STRING_ENCODE = \"" + encode + "\";");

   }

   public String generate(String body) throws Exception {

      ClientMessageImpl message = new ClientMessageImpl(MESSAGE_TYPE, DURABLE, EXPIRATION, TIMESTAMP, PRIORITY, 10 * 1024, null);
      TextMessageUtil.writeBodyText(message.getBodyBuffer(), SimpleString.toSimpleString(body));

      message.setAddress(ADDRESS);
      message.setUserID(uuid);
      message.getProperties().putSimpleStringProperty(PROP1_NAME, PROP1_VALUE);


      ActiveMQBuffer buffer = ActiveMQBuffers.dynamicBuffer(10 * 1024);
      message.sendBuffer(buffer.byteBuf(), 0);

      byte[] bytes = new byte[buffer.byteBuf().writerIndex()];
      buffer.byteBuf().readBytes(bytes);

      return encodeString(bytes);

      // replace the code


   }

   private String encodeString(byte[] bytes) {
      return Base64.encodeBytes(bytes, 0, bytes.length, Base64.DONT_BREAK_LINES | Base64.URL_SAFE);
   }

}
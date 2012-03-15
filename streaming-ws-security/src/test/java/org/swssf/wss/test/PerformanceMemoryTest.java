/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.swssf.wss.test;

import org.apache.ws.security.handler.WSHandlerConstants;
import org.swssf.wss.WSSec;
import org.swssf.wss.ext.InboundWSSec;
import org.swssf.wss.ext.OutboundWSSec;
import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSSecurityProperties;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.xmlsec.test.utils.XmlReaderToWriter;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class PerformanceMemoryTest extends AbstractTestBase {

    private File prepareBigEncryptedFile(int factor) throws Exception {
        File target = genBigFile(factor);
        File output = new File("target/enc.xml");
        doStreamingSecurityOutbound(target, output);
        return output;
    }

    private void doStreamingSecurityOutbound(File source, File output) throws Exception {
        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);
        securityProperties.setTimestampTTL(60 * 60 * 24 * 7); //a week for testing:)

        InputStream sourceDocument = new BufferedInputStream(new FileInputStream(source));
        OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);

        XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(new FileOutputStream(output), "UTF-8", new ArrayList<SecurityEvent>());
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(sourceDocument);
        XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
        xmlStreamWriter.close();
        xmlStreamReader.close();
    }

    private File genBigFile(int factor) throws IOException {
        File source = new File("ReferenzInstanzdokument20060922.xml");
        File target = new File("target/tmp.xml");
        FileWriter fileWriter = new FileWriter(target);
        fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "<env:Header></env:Header>\n" +
                "<env:Body><test xmlns=\"http://www.example.com\">");
        fileWriter.close();
        FileOutputStream fileOutputStream = new FileOutputStream(target, true);
        for (int i = 0; i <= factor; i++) {
            int read = 0;
            byte[] buffer = new byte[4096];
            FileInputStream fileInputStream = new FileInputStream(source);
            while ((read = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }
            fileInputStream.close();
        }
        fileWriter = new FileWriter(target, true);
        fileWriter.write("</test></env:Body>\n" +
                "</env:Envelope>");
        fileWriter.close();
        return target;
    }

    /*
    private int countTags(File file) throws Exception {
        int tagCount = 0;
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new FileInputStream(file));
        while (xmlStreamReader.hasNext()) {
            int eventType = xmlStreamReader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                tagCount++;
            }
        }
        return tagCount;
    }
    */

    private int[] tagCounts = new int[]{33391, 63731, 94071, 124411, 154751, 185091, 215431, 245771, 276111, 306451, 336791, 367131, 397471, 427811, 458151};

    @Test(groups = {"timing-out"})
    public void setUpOut() throws Exception {
        File input = genBigFile(1);
        Document doc = doOutboundSecurityWithWSS4J(new FileInputStream(input), WSHandlerConstants.ENCRYPT, new Properties());
        javax.xml.transform.Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(new File("target/bigfile-dom.xml")));
        doStreamingSecurityOutbound(input, new File("target/bigfile-stream.xml"));
    }

    @Test(groups = "streaming-memory-out")
    public void testStreamingOutMemoryPerformance() throws Exception {

        FileWriter samples = new FileWriter("target/memory-samples-stream-out.txt");
        long memoryDiff = 0;

        for (int i = 1; i <= 15; i++) {
            System.out.println("Run " + i);

            long startMem = getUsedMemory();
            System.out.println("Start Mem: " + startMem / 1024.0 / 1024.0);

            File input = genBigFile(i * 10);

            ThreadStopper threadStopper = new ThreadStopper();
            Thread thread = new Thread(new MemorySamplerThread(threadStopper, samples, memoryDiff));
            thread.setPriority(8);
            thread.start();

            long start = System.currentTimeMillis();
            doStreamingSecurityOutbound(input, new File("target/bigfile-stream.xml"));

            samples.write("" + tagCounts[i - 1]);
            samples.write(" ");
            samples.flush();

            threadStopper.setStop(true);

            System.out.println("Stream Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            System.out.println("Tag Count: " + tagCounts[i - 1]);
            System.out.println("");

            thread.join();

            samples.write("\n");
            samples.flush();
            long endMem = getUsedMemory();
            memoryDiff = endMem - startMem;
            System.out.println("Memory leak: " + ((memoryDiff)) / 1024.0 / 1024.0);
            System.out.println("Used memory: " + (endMem / 1024.0 / 1024.0));
            System.out.println("");
        }

        samples.close();
    }

    @Test(groups = "dom-memory-out")
    public void testDOMOutMemoryPerformance() throws Exception {

        FileWriter samples = new FileWriter("target/memory-samples-dom-out.txt");
        long memoryDiff = 0;
        long leakedMemory = 0;

        for (int i = 1; i <= 15; i++) {
            System.out.println("Run " + i);

            long startMem = getUsedMemory();
            System.out.println("Start Mem: " + startMem / 1024.0 / 1024.0);

            File input = genBigFile(i * 10);

            ThreadStopper threadStopper = new ThreadStopper();
            Thread thread = new Thread(new MemorySamplerThread(threadStopper, samples, leakedMemory));
            thread.setPriority(8);
            thread.start();

            long start = System.currentTimeMillis();

            Document doc = doOutboundSecurityWithWSS4J(new FileInputStream(input), WSHandlerConstants.ENCRYPT, new Properties());
            javax.xml.transform.Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(new File("target/bigfile-dom.xml")));

            samples.write("" + tagCounts[i - 1]);
            samples.write(" ");
            samples.flush();

            threadStopper.setStop(true);

            System.out.println("DOM Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            System.out.println("Tag Count: " + tagCounts[i - 1]);
            System.out.println("");

            thread.join();

            samples.write("\n");
            samples.flush();
            long endMem = getUsedMemory();
            memoryDiff = endMem - startMem;
            leakedMemory += memoryDiff;
            System.out.println("Memory leak: " + ((memoryDiff)) / 1024.0 / 1024.0);
            System.out.println("Used memory: " + (endMem / 1024.0 / 1024.0));
            System.out.println("");
        }

        samples.close();
    }

    @Test(groups = "stream")
    public void testStreamingMemoryPerformance() throws Exception {

        FileWriter samples = new FileWriter("target/memory-samples-stream.txt");
        long memoryDiff = 0;

        for (int i = 1; i <= 15; i++) {
            System.out.println("Run " + i);

            long startMem = getUsedMemory();
            System.out.println("Start Mem: " + startMem / 1024.0 / 1024.0);

            File input = prepareBigEncryptedFile(i * 10);

            ThreadStopper threadStopper = new ThreadStopper();
            Thread thread = new Thread(new MemorySamplerThread(threadStopper, samples, memoryDiff));
            thread.setPriority(8);
            thread.start();

            long start = System.currentTimeMillis();
            int tagCount = doStreamingInSecurity(input);

            samples.write("" + tagCount);
            samples.write(" ");
            samples.flush();

            threadStopper.setStop(true);

            System.out.println("Stream Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            System.out.println("Tag Count: " + tagCount);
            System.out.println("");

            thread.join();

            samples.write("\n");
            samples.flush();
            long endMem = getUsedMemory();
            memoryDiff = endMem - startMem;
            System.out.println("Memory leak: " + ((memoryDiff)) / 1024.0 / 1024.0);
            System.out.println("Used memory: " + (endMem / 1024.0 / 1024.0));
            System.out.println("");
        }

        samples.close();
    }

    private static void gc() {
        /*
        try {
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        System.gc();
        System.runFinalization();
        System.gc();
    }


    private static long getUsedMemory() {
        gc();
        long totalMemory = Runtime.getRuntime().totalMemory();
        gc();
        long freeMemory = Runtime.getRuntime().freeMemory();
        return totalMemory - freeMemory;
    }

    @Test(groups = "dom")
    public void testDOMMemoryPerformance() throws Exception {

        FileWriter samples = new FileWriter("target/memory-samples-dom.txt");
        long memoryDiff = 0;
        long leakedMemory = 0;

        for (int i = 1; i <= 15; i++) {
            System.out.println("Run " + i);

            long startMem = getUsedMemory();
            System.out.println("Start Mem: " + startMem / 1024.0 / 1024.0);

            File input = prepareBigEncryptedFile(i * 10);

            ThreadStopper threadStopper = new ThreadStopper();
            Thread thread = new Thread(new MemorySamplerThread(threadStopper, samples, leakedMemory));
            thread.setPriority(8);
            thread.start();

            long start = System.currentTimeMillis();
            int tagCount = doDOMInSecurity(input);

            samples.write("" + tagCount);
            samples.write(" ");
            samples.flush();

            threadStopper.setStop(true);

            System.out.println("DOM Time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            System.out.println("Tag Count: " + tagCount);
            System.out.println("");

            thread.join();

            samples.write("\n");
            samples.flush();
            long endMem = getUsedMemory();
            memoryDiff = endMem - startMem;
            leakedMemory += memoryDiff;
            System.out.println("Memory leak: " + ((memoryDiff)) / 1024.0 / 1024.0);
            System.out.println("Used memory: " + (endMem / 1024.0 / 1024.0));
            System.out.println("");
        }

        samples.close();
    }

    private int doDOMInSecurity(File input) throws Exception {
        String action = WSHandlerConstants.ENCRYPT;
        Document document = doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(input), action);
        NodeList nodeList = document.getElementsByTagName("*");
        int tagCount = nodeList.getLength();
        return tagCount;
    }

    private int doStreamingInSecurity(File input) throws Exception {
        WSSSecurityProperties inSecurityProperties = new WSSSecurityProperties();
        inSecurityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        inSecurityProperties.setCallbackHandler(new CallbackHandlerImpl());

        InboundWSSec wsSecIn = WSSec.getInboundWSSec(inSecurityProperties);
        FileInputStream fileInputStream = new FileInputStream(input);
        XMLStreamReader outXmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(fileInputStream));

        int tagCount = 0;
        while (outXmlStreamReader.hasNext()) {
            int eventType = outXmlStreamReader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                tagCount++;
            }
        }
        fileInputStream.close();
        outXmlStreamReader.close();
        return tagCount;
    }

    class ThreadStopper {
        private volatile boolean stop = false;

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }

    class MemorySamplerThread implements Runnable {

        private ThreadStopper threadStopper;
        private FileWriter fileWriter;
        private long memoryDiff = 0;
        private Thread parentThread;

        private List<Integer> memory = new LinkedList<Integer>();

        MemorySamplerThread(ThreadStopper threadStopper, FileWriter fileWriter, long memoryDiff) {
            this.threadStopper = threadStopper;
            this.fileWriter = fileWriter;
            this.memoryDiff = memoryDiff;
            this.parentThread = Thread.currentThread();
        }

        public void run() {

            int sleepTime = 100;

            while (!threadStopper.isStop()) {
                try {
                    Thread.sleep(sleepTime);
                    if (threadStopper.isStop()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //parentThread.suspend();
                memory.add((int) (((getUsedMemory()) - memoryDiff) / 1024.0 / 1024.0));
                //System.out.println("Sample: " + memory.get(memory.size() - 1));
                //parentThread.resume();
            }

            System.out.println("Collected " + memory.size() + " samples");

            int maxMem = Integer.MIN_VALUE;
            for (int i = 0; i < memory.size(); i++) {
                //System.out.println("Sample: " + memory.get(i));
                int mem = memory.get(i);
                maxMem = mem > maxMem ? mem : maxMem;
            }

            try {
                fileWriter.write("" + maxMem);
                fileWriter.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            System.out.println("Max memory usage: " + maxMem + "MB");
        }
    }
}
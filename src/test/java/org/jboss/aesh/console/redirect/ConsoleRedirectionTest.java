/*
  * Copyright 2012 Red Hat, Inc. and/or its affiliates.
  *
  * Licensed under the Eclipse Public License version 1.0, available at
  * http://www.eclipse.org/legal/epl-v10.html
  */
package org.jboss.aesh.console.redirect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.aesh.console.BaseConsoleTest;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.TestConsoleCallback;
import org.jboss.aesh.console.operator.ControlOperator;
import org.junit.Test;

/**
  * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
  */
 public class ConsoleRedirectionTest extends BaseConsoleTest {

     @Test
     public void pipeCommands() throws Throwable {
         PipedOutputStream outputStream = new PipedOutputStream();
         PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

         CountDownLatch latch = new CountDownLatch(1);
         List<Throwable> exceptions = new ArrayList<Throwable>();

         Console console = getTestConsole(pipedInputStream);
         console.setConsoleCallback(new RedirectionConsoleCallback(latch, exceptions, console));

         console.start();

         outputStream.write(("ls | find *. -print"+Config.getLineSeparator()).getBytes());

         if(!latch.await(200, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
         }
         console.stop();
         if(exceptions.size() > 0) {
            throw exceptions.get(0);
         }
     }

     @Test
     public void redirectionCommands() throws Throwable {
         PipedOutputStream outputStream = new PipedOutputStream();
         PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

         CountDownLatch latch = new CountDownLatch(1);
         List<Throwable> exceptions = new ArrayList<Throwable>();

         Console console = getTestConsole(pipedInputStream);
         console.setConsoleCallback(new RedirectionConsoleCallback(latch, exceptions, console));

         console.start();

         if(Config.isOSPOSIXCompatible()) {
             outputStream.write(("ls >"+Config.getTmpDir()+"/foo\\ bar.txt"+Config.getLineSeparator()).getBytes());
         }
         else {
             outputStream.write(("ls >"+Config.getTmpDir()+"\\foo\\ bar.txt"+Config.getLineSeparator()).getBytes());
         }

         if(!latch.await(200, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
         }
         console.stop();
         if(exceptions.size() > 0) {
            throw exceptions.get(0);
         }
     }

     @Test
     public void redirectIn() throws Throwable {
         PipedOutputStream outputStream = new PipedOutputStream();
         PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

         CountDownLatch latch = new CountDownLatch(1);
         List<Throwable> exceptions = new ArrayList<Throwable>();

         final Console console = getTestConsole(pipedInputStream);
         console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {

             @Override
             public int verify(ConsoleOperation output) {
                 assertEquals("ls ", output.getBuffer());
                 try {
                     assertTrue(console.getShell().in().getStdIn().available() > 0);
                 }
                 catch (IOException e) {
                     fail();
                 }
                 assertEquals(ControlOperator.NONE, output.getControlOperator());
                 java.util.Scanner s = new java.util.Scanner(console.getShell().in().getStdIn()).useDelimiter("\\A");
                 String fileContent = s.hasNext() ? s.next() : "";
                 assertEquals("CONTENT OF FILE", fileContent);
                 return 0;
             }
         });
         console.start();

         if(Config.isOSPOSIXCompatible())
             outputStream.write(("ls < "+Config.getTmpDir()+"/foo\\ bar.txt"+Config.getLineSeparator()).getBytes());
         else
             outputStream.write(("ls < "+Config.getTmpDir()+"\\foo\\ bar.txt"+Config.getLineSeparator()).getBytes());
         outputStream.flush();

         if(!latch.await(200, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
         }
         console.stop();
         if(exceptions.size() > 0) {
            throw exceptions.get(0);
         }
     }

     @Test
     public void redirectIn2() throws Throwable {
         PipedOutputStream outputStream = new PipedOutputStream();
         PipedInputStream pipedInputStream = new PipedInputStream(outputStream);

         CountDownLatch latch = new CountDownLatch(1);
         List<Throwable> exceptions = new ArrayList<Throwable>();

         final Console console = getTestConsole(pipedInputStream);
         console.setConsoleCallback(new TestConsoleCallback(latch, exceptions) {

            private int count = 0;

            @Override
            public int verify(ConsoleOperation output) {
                if(count == 0) {
                    assertEquals("ls ", output.getBuffer());
                    try {
                        assertTrue(console.getShell().in().getStdIn().available() > 0);
                    }
                    catch (IOException e) {
                        fail();
                    }
                    //assertTrue(output.getStdOut().contains("CONTENT OF FILE"));
                    assertEquals(ControlOperator.PIPE, output.getControlOperator());
                    java.util.Scanner s = new java.util.Scanner(console.getShell().in().getStdIn()).useDelimiter("\\A");
                    String fileContent = s.hasNext() ? s.next() : "";
                    assertEquals("CONTENT OF FILE", fileContent);
                }
                else if(count == 1) {
                    assertEquals(" man", output.getBuffer());
                    assertEquals(ControlOperator.NONE, output.getControlOperator());
                }

                count++;
                return 0;
            }
         });
         console.start();

         if(Config.isOSPOSIXCompatible())
             outputStream.write(("ls < "+Config.getTmpDir()+"/foo\\ bar.txt | man"+Config.getLineSeparator()).getBytes());
         else
             outputStream.write(("ls < "+Config.getTmpDir()+"\\foo\\ bar.txt | man"+Config.getLineSeparator()).getBytes());
         outputStream.flush();

         if(!latch.await(200, TimeUnit.MILLISECONDS)) {
            fail("Failed waiting for Console to finish");
         }
         console.stop();
         if(exceptions.size() > 0) {
            throw exceptions.get(0);
         }
     }

     class RedirectionConsoleCallback extends TestConsoleCallback {
         private int count = 0;
         Console console;

         RedirectionConsoleCallback(CountDownLatch latch, List<Throwable> exceptions, Console console) {
            super(latch, exceptions);
            this.console = console;
         }
         @Override
         public int verify(ConsoleOperation output) {
             if(count == 0) {
                 assertEquals("ls ", output.getBuffer());
                 count++;
             }
             else if(count == 1) {
                 assertEquals(" find *. -print", output.getBuffer());
             }
             return 0;
         }
     }
}



/*
 * Copyright (C) 2026 Herve Girod
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.girod.httpserver.gui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import org.girod.httpserver.HttpServer;

/**
 * The HTTP server GUI which can be used to serve the content of the html wiki.
 *
 * @since 1.0
 */
public class HttpServerGUI extends JFrame {
   private boolean quiet = true;
   private String cors = null;
   private File rootDir = null;
   private String host = null;
   private int port = 8080;
   private HttpServer server = null;
   private JTextField tfPort = null;
   private AbstractAction startButton = null;
   private AbstractAction stopButton = null;

   public HttpServerGUI(String host, int port, File rootDir, boolean quiet, String cors) {
      super("HTTP Server");
      this.host = host;
      this.port = port;
      this.rootDir = rootDir;
      this.quiet = quiet;
      this.cors = cors;
      setup();
   }

   /**
    * Start the server.
    */
   public final void start() {
      if (server == null) {
         this.server = new HttpServer(host, port, rootDir, quiet, cors);
      }
      server.startInBackground();
   }

   /**
    * Stop the server.
    */
   public void stop() {
      if (server != null) {
         server.stop();
      }
   }

   private void setup() {
      Container pane = this.getContentPane();
      pane.setLayout(new FlowLayout());

      startButton = new AbstractAction("Start") {
         @Override
         public void actionPerformed(ActionEvent e) {
            start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            tfPort.setEnabled(false);
         }
      };
      tfPort = new JTextField(5);
      tfPort.setText(Integer.toString(port));
      tfPort.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               int thePort = Integer.parseInt(tfPort.getText());
               port = thePort;
            } catch (NumberFormatException ex) {
               tfPort.setText(Integer.toString(port));
            }
         }
      });

      stopButton = new AbstractAction("Stop") {
         @Override
         public void actionPerformed(ActionEvent e) {
            stop();
            startButton.setEnabled(true);
            tfPort.setEnabled(true);
            stopButton.setEnabled(false);
         }
      };
      stopButton.setEnabled(false);
      this.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed(WindowEvent e) {
            stop();
            System.exit(-1);
         }
      });
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      pane.add(new JButton(startButton));
      pane.add(tfPort);
      pane.add(new JButton(stopButton));
      this.pack();
   }

   /**
    * Starts a server GUI. The arguments are:
    * <ul>
    * <li>"-h" or "--host": the host (will use the localhost by default)</li>
    * <li>"-p" or "--port": the port (will be 8080 by default)</li>
    * <li>"-l" or "--log": indicates if the server execution should be logged (false by default)</li>
    * <li>"-d" or "--dir": the directory where the index.html file should be found (user.dir by default)</li>
    * </ul>
    *
    * @param args the arguments
    */
   public static void main(String[] args) {
      // Defaults
      int port = 8080;
      String host = null;
      File rootDir = null;
      boolean quiet = true;
      String cors = null;

      // Parse command-line, with short and long versions of the options.
      for (int i = 0; i < args.length; ++i) {
         if ("-h".equalsIgnoreCase(args[i]) || "--host".equalsIgnoreCase(args[i])) {
            host = args[i + 1];
         } else if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
            port = Integer.parseInt(args[i + 1]);
         } else if ("-l".equalsIgnoreCase(args[i]) || "--log".equalsIgnoreCase(args[i])) {
            quiet = false;
         } else if ("-d".equalsIgnoreCase(args[i]) || "--dir".equalsIgnoreCase(args[i])) {
            rootDir = new File(args[i + 1]).getAbsoluteFile();
         } else if (args[i].startsWith("--cors")) {
            cors = "*";
            int equalIdx = args[i].indexOf('=');
            if (equalIdx > 0) {
               cors = args[i].substring(equalIdx + 1);
            }
         }
      }

      if (rootDir == null) {
         rootDir = new File(System.getProperty("user.dir"));
      }
      HttpServerGUI gui = new HttpServerGUI(host, port, rootDir, quiet, cors);
      gui.setVisible(true);
   }
}

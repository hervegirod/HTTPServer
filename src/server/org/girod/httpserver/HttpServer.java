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
package org.girod.httpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import static org.nanohttpd.protocols.http.NanoHTTPD.MIME_PLAINTEXT;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.ServerRunner;

/**
 * The HTTP server which can be used to serve the content of the html wiki. The options are:
 * <ul>
 * <li>"-h" or "--host": the host (will use the localhost by default)</li>
 * <li>"-p" or "--port": the port (will be 8080 by default)</li>
 * <li>"-l" or "--log": indicates if the server should be logged (false by default)</li>
 * <li>"-d" or "--dir": the directory where the index.html file should be found (user.dir by default)</li>
 * </ul>
 *
 * The class can also get these properties from a <code>server.properties</code> file in the same directory as the jar file. The supported
 * properties are:
 * <ul>
 * <li>"host": the host</li>
 * <li>"port": the port</li>
 * <li>"log": indicates if the server should be logged</li>
 * <li>"dir": the directory where the index.html file should be found</li>
 * </ul>
 *
 * @since 1.0
 */
public class HttpServer extends NanoHTTPD {
   private final boolean quiet;
   private final String cors;
   private File rootDir;
   private long lastModified = -1;
   private boolean isZipURI = false;
   private URL parentZipURL = null;

   private String calculateAllowHeaders(Map<String, String> queryHeaders) {
      // here we should use the given asked headers
      // but NanoHttpd uses a Map whereas it is possible for requester to send
      // several time the same header
      // let's just use default values for this version
      return System.getProperty(ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, DEFAULT_ALLOWED_HEADERS);
   }
   private final static String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
   public static final List<String> INDEX_FILE_NAMES = new ArrayList<String>() {
      {
         add("index.html");
         add("index.htm");
      }
   };
   private final static int MAX_AGE = 42 * 60 * 60;

   // explicitly relax visibility to package for tests purposes
   public final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";

   public final static String ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";

   /**
    * Constructor. Will use the 8080 port on the user directory.
    */
   public HttpServer() {
      super(null, 8080);
      this.quiet = true;
      this.cors = null;
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    */
   public HttpServer(String host, int port) {
      super(host, port);
      this.quiet = true;
      this.cors = null;
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    * @param quiet true if the server is quiet
    */
   public HttpServer(String host, int port, boolean quiet) {
      super(host, port);
      this.quiet = quiet;
      this.cors = null;
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    * @param quiet true if the server is quiet
    */
   public HttpServer(String host, int port, boolean quiet, String cors) {
      super(host, port);
      this.quiet = quiet;
      this.cors = cors;
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    * @param rootDir the root directory
    */
   public HttpServer(String host, int port, File rootDir) {
      this(host, port, rootDir, true, null);
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    * @param rootDir the root directory
    * @param quiet true if the server is quiet
    */
   public HttpServer(String host, int port, File rootDir, boolean quiet) {
      this(host, port, rootDir, quiet, null);
   }

   /**
    * Constructor.
    *
    * @param host the host
    * @param port the port
    * @param rootDir the root directory
    * @param quiet true if the server is quiet
    */
   public HttpServer(String host, int port, File rootDir, boolean quiet, String cors) {
      super(host, port);
      this.quiet = quiet;
      this.cors = cors;
      if (rootDir == null) {
         this.rootDir = new File(System.getProperty("user.dir"));
      } else {
         this.rootDir = rootDir;
      }
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @return the server
    */
   public static HttpServer create() {
      return create(false);
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @param quiet true if the server is quiet
    * @return the server
    */
   public static HttpServer create(boolean quiet) {
      int port = 8080;
      String host = "localhost";
      File rootDir = new File(System.getProperty("user.dir"));
      HttpServer server = new HttpServer(host, port, rootDir, true);
      return server;
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @param port the port
    * @param quiet true if the server is quiet
    * @return the server
    */
   public static HttpServer create(int port, boolean quiet) {
      return create(null, port, null, true);
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @param host the host
    * @param port the port
    * @return the server
    */
   public static HttpServer create(String host, int port) {
      return create(host, port, null, true);
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @param host the host
    * @param port the port
    * @param rootDir the root directory
    * @return the server
    */
   public static HttpServer create(String host, int port, File rootDir) {
      return create(host, port, rootDir, true);
   }

   /**
    * Creates a HttpServer. Will use the 8080 port on the user directory.
    *
    * @param port the port
    * @param rootDir the root directory
    * @param quiet true if the server is quiet
    * @return the server
    */
   public static HttpServer create(int port, File rootDir, boolean quiet) {
      return create(null, port, rootDir, true);
   }

   /**
    * Creates a HttpServer.Will use the 8080 port on the user directory.
    *
    * @param host the host
    * @param port the port
    * @param rootDir the root directory
    * @param quiet true if the server is quiet
    * @return the server
    */
   public static HttpServer create(String host, int port, File rootDir, boolean quiet) {
      if (host == null) {
         host = "localhost";
      }
      if (rootDir == null) {
         rootDir = new File(System.getProperty("user.dir"));
      }
      HttpServer server = new HttpServer(host, port, rootDir, quiet);
      return server;
   }

   /**
    * Set the root directory or file. This is the directory (or thz zip file) which will be used by the server to fetch the files. It will
    * be used if {@link #isZipURI()} is false.
    *
    * @param rootDir the root directory or file
    */
   public void setRootDirectory(File rootDir) {
      this.rootDir = rootDir;
   }

   /**
    * Return the root directory. This is the directory which will be used by the server to fetch the files. It will be used if
    * {@link #isZipURI()} is false.
    *
    * @return the root directory
    */
   public File getRootDirectory() {
      return rootDir;
   }

   /**
    * Set the URL of the zip file which will be used by the server to fetch the files.
    *
    * @param parentZipURL the URL of the zip file
    */
   public void setZipURI(URL parentZipURL) {
      this.isZipURI = true;
      this.parentZipURL = parentZipURL;
      File file = new File(parentZipURL.getFile());
      this.lastModified = file.lastModified();
   }

   /**
    * Return true if the files are fetched from a zip.
    *
    * @return true if the files are fetched from a zip
    * @see #setZipURI(java.net.URL)
    */
   public boolean isZipURI() {
      return isZipURI;
   }

   private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
      // First let's handle CORS OPTION query
      Response r;
      if (cors != null && Method.OPTIONS.equals(session.getMethod())) {
         r = Response.newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, null, 0);
      } else {
         r = defaultRespond(headers, session, uri);
      }

      if (cors != null) {
         r = addCORSHeaders(headers, r, cors);
      }
      return r;
   }

   private boolean exist(URL url) {
      if (url == null) {
         return false;
      } else {
         String path = url.getFile();
         try {
            if (path.startsWith("file:")) {
               path = path.substring(5);
            }
            int entryPos = path.indexOf('!');
            if (entryPos != -1) {
               path = path.substring(0, entryPos);
               url = new URL("file", "", -1, path);
            }
            InputStream stream = url.openStream();
            stream.close();
            return true;
         } catch (IOException e) {
            return false;
         }
      }
   }

   private Response defaultRespond(Map<String, String> headers, IHTTPSession session, String uri) {
      // Remove URL arguments
      uri = uri.trim().replace(File.separatorChar, '/');
      if (uri.indexOf('?') >= 0) {
         uri = uri.substring(0, uri.indexOf('?'));
      }

      // Prohibit getting out of current directory
      if (uri.contains("../")) {
         return getForbiddenResponse("Won't serve ../ for security reasons.");
      }

      boolean canServeUri = false;
      File homeDir = null;
      if (isZipURI) {
         canServeUri = canServeUri(uri, null);
         if (canServeUri) {
            URL theURL;
            try {
               theURL = FileUtils.getEntryURL(parentZipURL, uri);
               uri = theURL.toURI().toString();
            } catch (MalformedURLException | URISyntaxException ex) {
               theURL = null;
            }
         }
      } else {
         if (rootDir != null) {
            homeDir = rootDir;
            canServeUri = canServeUri(uri, homeDir);
         } else {
            homeDir = new File(System.getProperty("user.dir"));
            rootDir = homeDir;
         }
      }
      if (!canServeUri) {
         return getNotFoundResponse();
      }

      File f;
      if (!isZipURI) {
         // Browsers get confused without '/' after the directory, send a redirect.
         f = new File(homeDir, uri);
         if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = newFixedLengthResponse(Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
         }

         if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if
            // none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile != null) {
               return respond(headers, session, uri + indexFile);
            }
         }
      } else {
         f = null;
      }
      String mimeTypeForFile = getMimeTypeForFile(uri);
      Response response = serveFile(uri, headers, f, mimeTypeForFile);
      return response != null ? response : getNotFoundResponse();
   }

   private boolean canServeUri(String uri, File homeDir) {
      boolean canServeUri;
      if (isZipURI) {
         URL theURL;
         try {
            theURL = FileUtils.getEntryURL(parentZipURL, uri);
            return exist(theURL);
         } catch (MalformedURLException ex) {
            return false;
         }

      } else {
         File f = new File(homeDir, uri);
         canServeUri = f.exists();
         return canServeUri;
      }
   }

   private String findIndexFileInDirectory(File directory) {
      for (String fileName : HttpServer.INDEX_FILE_NAMES) {
         File indexFile = new File(directory, fileName);
         if (indexFile.isFile()) {
            return fileName;
         }
      }
      return null;
   }

   /**
    * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
    */
   Response serveFile(String uri, Map<String, String> header, File file, String mime) {
      Response res;
      try {
         long fileLen;
         String etag;
         URL url = null;
         if (isZipURI) {
            url = FileUtils.getURL(uri);
            fileLen = FileUtils.getLength(url);
            etag = Integer.toHexString((url.getPath() + lastModified + "" + fileLen).hashCode());
         } else {
            fileLen = file.length();
            etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + fileLen).hashCode());
         }

         // Calculate etag
         // Support (simple) skipping:
         long startFrom = 0;
         long endAt = -1;
         String range = header.get("range");
         if (range != null) {
            if (range.startsWith("bytes=")) {
               range = range.substring("bytes=".length());
               int minus = range.indexOf('-');
               try {
                  if (minus > 0) {
                     startFrom = Long.parseLong(range.substring(0, minus));
                     endAt = Long.parseLong(range.substring(minus + 1));
                  }
               } catch (NumberFormatException ignored) {
               }
            }
         }

         // get if-range header. If present, it must match etag or else we
         // should ignore the range request
         String ifRange = header.get("if-range");
         boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

         String ifNoneMatch = header.get("if-none-match");
         boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

         if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
            // range request that matches current etag
            // and the startFrom of the range is satisfiable
            if (headerIfNoneMatchPresentAndMatching) {
               // range request that matches current etag
               // and the startFrom of the range is satisfiable
               // would return range from file
               // respond with not-modified
               res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
               res.addHeader("ETag", etag);
            } else {
               if (endAt < 0) {
                  endAt = fileLen - 1;
               }
               long newLen = endAt - startFrom + 1;
               if (newLen < 0) {
                  newLen = 0;
               }

               InputStream is;
               if (isZipURI) {
                  is = url.openStream();
               } else {
                  is = new FileInputStream(file);
               }

               is.skip(startFrom);

               res = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, mime, is, newLen);
               res.addHeader("Accept-Ranges", "bytes");
               res.addHeader("Content-Length", "" + newLen);
               res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
               res.addHeader("ETag", etag);
            }
         } else {
            if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
               // return the size of the file
               // 4xx responses are not trumped by if-none-match
               res = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
               res.addHeader("Content-Range", "bytes */" + fileLen);
               res.addHeader("ETag", etag);
            } else if (range == null && headerIfNoneMatchPresentAndMatching) {
               // full-file-fetch request
               // would return entire file
               // respond with not-modified
               res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
               res.addHeader("ETag", etag);
            } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
               // range request that doesn't match current etag
               // would return entire (different) file
               // respond with not-modified
               res = newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
               res.addHeader("ETag", etag);
            } else {
               // supply the file
               if (isZipURI) {
                  res = newFixedFileResponse(url, fileLen, mime);
               } else {
                  res = newFixedFileResponse(file, mime);
               }
               res.addHeader("Content-Length", "" + fileLen);
               res.addHeader("ETag", etag);
            }
         }
      } catch (IOException ioe) {
         res = getForbiddenResponse("Reading file failed.");
      }
      return res;
   }

   public static Response newFixedLengthResponse(IStatus status, String mimeType, String message) {
      Response response = Response.newFixedLengthResponse(status, mimeType, message);
      response.addHeader("Accept-Ranges", "bytes");
      return response;
   }

   private Response newFixedFileResponse(URL url, long len, String mime) throws IOException {
      Response res = Response.newFixedLengthResponse(Status.OK, mime, url.openStream(), (int) len);
      res.addHeader("Accept-Ranges", "bytes");
      return res;
   }

   private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
      Response res = Response.newFixedLengthResponse(Status.OK, mime, new FileInputStream(file), (int) file.length());
      res.addHeader("Accept-Ranges", "bytes");
      return res;
   }

   private Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
      resp.addHeader("Access-Control-Allow-Origin", cors);
      resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
      resp.addHeader("Access-Control-Allow-Credentials", "true");
      resp.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
      resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

      return resp;
   }

   @Override
   public Response serve(IHTTPSession session) {
      Map<String, String> header = session.getHeaders();
      Map<String, String> parms = session.getParms();
      String uri = session.getUri();

      if (!this.quiet) {
         System.out.println(session.getMethod() + " '" + uri + "' ");

         Iterator<String> e = header.keySet().iterator();
         while (e.hasNext()) {
            String value = e.next();
            System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
         }
         e = parms.keySet().iterator();
         while (e.hasNext()) {
            String value = e.next();
            System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
         }
      }

      // Make sure we won't die of an exception later
      if (!isZipURI && !rootDir.isDirectory()) {
         return getInternalErrorResponse("given path is not a directory (" + rootDir + ").");
      }
      return respond(Collections.unmodifiableMap(header), session, uri);
   }

   private Response getForbiddenResponse(String s) {
      return Response.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
   }

   private Response getInternalErrorResponse(String s) {
      return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
   }

   private Response getNotFoundResponse() {
      return Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
   }

   /**
    * Starts as a standalone file server. The arguments are:
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

      String host = null; // bind to all interfaces by default
      File rootDir = null;
      boolean quiet = true;
      String cors = null;
      File userdir = new File(System.getProperty("user.dir"));
      File propertiesFile = new File(userdir, "server.properties");
      if (propertiesFile.exists() && propertiesFile.isFile()) {
         try ( FileInputStream fis = new FileInputStream(propertiesFile)) {
            PropertyResourceBundle pb = new PropertyResourceBundle(fis);
            if (pb.containsKey("port")) {
               port = Integer.parseInt(pb.getString("port"));
            }
            if (pb.containsKey("port")) {
               port = Integer.parseInt(pb.getString("port"));
            }
            if (pb.containsKey("host")) {
               host = pb.getString("host");
            }
            if (pb.containsKey("dir")) {
               rootDir = new File(pb.getString("dir")).getAbsoluteFile();
            }
         } catch (IOException ex) {
         }
      }

      // Parse command-line, with short and long versions of the options.
      for (int i = 0; i < args.length; ++i) {
         if ("-h".equalsIgnoreCase(args[i]) || "--host".equalsIgnoreCase(args[i])) {
            host = args[i + 1];
         } else if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
            port = Integer.parseInt(args[i + 1]);
         } else if ("-l".equalsIgnoreCase(args[i]) || "--log".equalsIgnoreCase(args[i])) {
            quiet = true;
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
      String hostName;
      if (host == null) {
         hostName = "localhost";
      } else {
         hostName = host;
      }
      System.out.println("Browse the Wiki at the http://" + hostName + ":" + port + "/ address");
      ServerRunner.executeInstance(new HttpServer(host, port, rootDir, quiet, cors));
   }

   /**
    * Start the server.
    */
   @Override
   public void start() {
      ServerRunner.executeInstance(this);
   }

   /**
    * Start the server in a background thread.
    */
   public void startInBackground() {
      Runnable r = new Runnable() {
         @Override
         public void run() {
            start();
         }
      };
      Thread th = new Thread(r, "HttpServer");
      th.start();
   }
}

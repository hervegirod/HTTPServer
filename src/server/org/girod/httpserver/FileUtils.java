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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * File utilities for the http server.
 *
 * @since 1.0
 */
class FileUtils {
   private static final String FIREFOX10_USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0";  

   private FileUtils() {
   }

   static URL getURL(String uri) {
      try {
         URI theURI = new URI(uri);
         URL url = theURI.toURL();
         return url;
      } catch (URISyntaxException | MalformedURLException ex) {
         return null;
      }
   }
   
   static URL getEntryURL(URL parentURL, String path) throws MalformedURLException {
      if (path.startsWith("jar:file:")) {
         URL url = new URL(path);
         return url;
      } else if (path.startsWith("/jar:file:")) { 
         URL url = new URL(path.substring(1));
         return url;
      }
      String zipfileURL = parentURL.toString();
      if( path.startsWith("/")) {
         path = path.substring(1);
      }
      String urlSpec = "jar:" + zipfileURL + "!/" + path;
      URL url = new URL(urlSpec);
      return url;
   }    

   static boolean isHTTPorHTTPSProtocol(URL url) {
      String protocol = url.getProtocol();
      switch (protocol) {
         case "http":
         case "https":
            return true;
         case "jar":
         case "zip":
            String path = url.getPath();
            try {
               URL nestedURL = new URL(path);
               protocol = nestedURL.getProtocol();
               return protocol.equals("http") || protocol.equals("https");
            } catch (MalformedURLException ex) {
               return false;
            }
         default:
            return false;
      }
   }

   static long getLength(URL url) throws IOException {
      if (isHTTPorHTTPSProtocol(url)) {
         URLConnection connection = url.openConnection();
         if (connection instanceof HttpURLConnection) {
            HttpURLConnection huc = (HttpURLConnection) connection;
            huc.setRequestProperty("User-Agent", FIREFOX10_USERAGENT);
            huc.setRequestMethod("GET");
            huc.setRequestProperty("Accept-Charset", "UTF-8");
            huc.setInstanceFollowRedirects(true);
            huc.connect();
            return huc.getContentLengthLong();
         } else {
            return -1;
         }
      } else {
         String protocol = url.getProtocol();
         if (protocol.equals("file")) {
            File file = new File(url.getFile());
            return file.length();
         } else {
            // see https://www.rgagnon.com/javadetails/java-0298.html
            URLConnection conn;
            try {
               conn = url.openConnection();
               return conn.getContentLengthLong();
            } catch (IOException ex) {
               return -1;
            }
         }
      }
   }
}

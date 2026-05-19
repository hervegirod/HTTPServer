# HTTP Server
This library is a simple an compact Java HTTP Server using the nanohttpd library. Note that this library has no dependencies, the nanohttpd library is embedded.

# History
See [HISTORY.md](HISTORY.md)

# Command-line usage
This tools allows to start a simple Java HTTP Server, for example:
~~~~
java -jar HTTPServer.jar 
~~~~
The server considers by default that the directory where the html pages are stored is the same directory as the HTTP Server jar file. The server will
initially. Then you can go in any brower and type http://localhost:8080 to access the index page (`index.html` or `index.htm`). 

## Command-line arguments
The HTTP Server has the following optional command-line arguments:
- `-h` or `--host`: the host (will use the localhost by default)
- `-p` or `--port`: the port (will be 8080 by default)
- `-l` or `--log`: indicates if the server should be logged (false by default)
- `-d` or `--dir`: the directory where the `index.html` or `index.htm` file should be found (will be the user directory by default)

The class can also get these properties from a `server.properties` file in the same directory as the jar file. The supported properties are:
- `host`: the host
- `port`: the port
- `log`: indicates if the server should be logged
- `dir`: the directory where the `index.html` or `index.htm` file should be found

# Embedding usage
The HTTP Server can also be embedded and started with the following code:
~~~~
HTTPServer server = HTTPServer.create();
server.startInBackground();
~~~~

Several constructors allow to specify how the server must be started, for example:
~~~~
HTTPServer server = HTTPServer.create(String host, String port);
HTTPServer server = HTTPServer.create(String host, String port, File root);
HTTPServer server = HTTPServer.create(String port, boolean quiet);
HTTPServer server = HTTPServer.create(String port, File root, boolean quiet);
~~~~
Using the various `create(...)` static methods rather than the constructors is simpler because the HTTPServer will use the default values for the host and
the root if these parameters are not defined (ie if their respective value is `null`).
 
If `root` is a directory, the server will navigate in the directory for the content of the pages. It can also be a zip file, in that case it will
navigate in the content of the zip file.

## Starting and stopping the HTTP Server
To start the HTTP Server, you must use the `startInBackground()` method rather than the `start()` method because the `start()` method blocks the caller thread
until a request has been received.

To stop the HTTP Server, you can simply use the `stop() method.

# HTTP Server GUI
Dubkle clicking on the HttpServerGUI.jar opens a GUI allowing to start or stop a HTTP Server.

# Embedding of the the nanohttpd library
The nanohttpd library 2.3.1 (https://github.com/NanoHttpd/nanohttpd) is embedded in the HTTP Server:
- Only the `org.nanohttpd` package and sub-packages have been included (because the other packages are not useful in our use case)
- Several methods have been improved

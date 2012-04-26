/* Copyright 2012 Aaron Porter aaron@mongus.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.picohttp.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PicoHttp {
    private final Constructor<? extends Request> requestConstructor;
    private final RequestHandler requestHandler;
    private boolean run = true;

    public PicoHttp(RequestHandler requestHandler, int port) {
        this(SimpleRequest.class, requestHandler, null, port, 20);
    }

    public <R extends Request> PicoHttp(Class<R> requestType,
            RequestHandler requestHandler, InetAddress address, int port,
            final int maxThreads) throws IllegalArgumentException {
        this.requestHandler = requestHandler;
        
        try {
            final ServerSocket server = address == null ? new ServerSocket(port) : new ServerSocket(port, maxThreads * 4, address);
            server.setSoTimeout(1000);

            requestConstructor = requestType.getConstructor(Socket.class);

            final Thread main = new Thread(new Runnable() {
                @Override
                public void run() {
                    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, maxThreads, 30,
                            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

                    System.out.println("PicoHttp listening on " + server.getLocalSocketAddress());

                    while (run) {
                        try {
                            Socket socket = server.accept();
                            RequestEngine requestEngine = new RequestEngine(socket);
                            executor.execute(requestEngine);
                        }
                        catch (SocketTimeoutException e) {
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    executor.shutdown();
                    try {
                        executor.awaitTermination(1, TimeUnit.MINUTES);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            main.setDaemon(false);
            main.start();
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Couldn't start the server.", e);
        }
        catch (SecurityException e) {
            throw new IllegalArgumentException(
                    "Please check constructor access for " + requestType, e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Please check constructor for " + requestType, e);
        }
    }

    private class RequestEngine implements Runnable {
        private final Socket socket;

        public RequestEngine(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Request request = null;
            
            try {
                request = requestConstructor.newInstance(socket);
            }
            catch (Exception e) {
                System.err.println("Bad request from "
                        + socket.getRemoteSocketAddress());
            }
            
            if (request != null) {
                final Response response = requestHandler.execute(request);
                final Socket socket = request.getSocket();

                try {
                    final OutputStream out = socket.getOutputStream();
                    response.write(out);
                    out.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (!socket.isClosed())
                            socket.close();
                    }
                    catch (IOException e) {
                    }
                }
            }
        }
    }

    public void stop() {
        run = false;
    }

    public static final DateFormat[] HTTP_HEADER_DATE_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"), // RFC 822, updated by RFC 1123
            new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z"), // RFC 850, obsoleted by RFC 1036
            new SimpleDateFormat("EEE MMM HH:mm:ss yyyy"), // ANSI C's asctime() format
    };
}

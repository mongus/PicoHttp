package com.picohttp.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public interface Request {
    public Socket getSocket();
    public String getMethod();
    public String getUri();
    public String getProtocol();
    public String getPath();
    public String getQueryString();
    public String getHostname();
    public String getHeader(String name);
    public <T> T getHeader(String name, Class<T> target);
    public Map<String, String> getHeaders();
    public String getBody() throws IOException;
    public String getParameter(String name);
    public <T> T getParameter(String name, Class<T> target);
    public Map<String, String> getParameters();
}

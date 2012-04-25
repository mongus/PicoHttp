package com.picohttp.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SimpleRequest implements Request {
    private final Socket socket;
    private final BufferedInputStream inputStream;
    private final String method, uri, protocol, path, queryString, hostname;
    private final Map<String, String> headers = new HashMap<String, String>();
    private final Map<String, String> parameters = new HashMap<String, String>();

    private String body = null;

    public SimpleRequest(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = new BufferedInputStream(socket.getInputStream());

        // Read the first line
        String line = readHeaderLine();
        String[] parts = splitStatusLine(line);
        this.method = parts[0];
        this.uri = parts[1];
        this.protocol = parts[2];

        // Read the headers
        String name = null;
        while ((line = readHeaderLine()).length() > 0) {
            if (line.charAt(0) != ' ') {
                parts = splitHeaderLine(line);
                name = parts[0];
                String value = parts[1];
                headers.put(name, value);
            }
            else {
                headers.put(name, headers.get(name) + line.trim());
            }
        }

        // Read the query string parameters
        int q = uri.indexOf('?');
        if (q != -1) {
            path = uri.substring(0, q);
            queryString = uri.substring(q + 1);
            readUrlEncodedParameters(uri.substring(q + 1), "UTF-8");
        }
        else {
            path = uri;
            queryString = null;
        }

        String contentType = getHeader("Content-Type");
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            String charset = getCharset(contentType);
            // Read the parameters encoded in the message body
            readUrlEncodedParameters(getBody(), charset);
        }
        
        String hostname = getHeader("Host");
        if (hostname != null)
            hostname = hostname.replaceAll(":.*", "");
        else
            hostname = socket.getInetAddress().getHostAddress();
        this.hostname = hostname;
    }
    
    private String readHeaderLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        
        int b;
        
        while ((b = inputStream.read()) != -1) {
            if (b == '\r' || b == '\n') {
                if (b == '\r') {
                    // Header lines should end with \r\n but just to be safe...
                    inputStream.mark(1);
                    b = inputStream.read();
                    if (b != '\n') {
                        // Request is invalid according to the HTTP spec but we'll forgive
                        inputStream.reset();
                    }
                }
                
                break;
            }
            else
                sb.append((char) b);
        }
        
        return sb.toString();
    }
    
    protected String getCharset(String string) {
        if (string != null && string.contains("charset="))
            return string.replaceAll(".*charset=(\\S+)", "$1");
        else
            return "UTF-8";
    }

    protected String[] splitStatusLine(String line) {
        String[] parts = new String[3];
        if (line != null) {
            int s1 = line.indexOf(' ');
            if (s1 > 0) {
                int s2 = line.indexOf(' ', s1 + 1);
                if (s2 > 0) {
                    parts[0] = line.substring(0, s1);
                    parts[1] = line.substring(s1 + 1, s2);
                    parts[2] = line.substring(s2 + 1);
                }
            }
        }
        return parts;
    }

    protected String[] splitHeaderLine(String line) {
        String[] parts = new String[2];
        if (line != null) {
            int p = line.indexOf(":");
            if (p > 0) {
                parts[0] = line.substring(0, p);
                p++;
                if (line.charAt(p) == ' ')
                    p++;
                parts[1] = line.substring(p);
            }
        }
        return parts;
    }

    protected void readUrlEncodedParameters(String encoded, String charset) {
        try {
            String[] pairs = encoded.split("&");
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                String name = URLDecoder.decode(parts[0], charset);
                String value = "";
                if (parts.length == 2)
                    value = URLDecoder.decode(parts[1], charset);
                parameters.put(name, value);
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getHeader(String name) {
        String value = headers.get(name);

        if (value == null) {
            for (String header : headers.keySet()) {
                if (name.equalsIgnoreCase(header)) {
                    value = headers.get(header);
                    break;
                }
            }
        }

        return value;
    }

    @Override
    public String getBody() throws IOException {
        if (body == null)
            readBody();

        return body;
    }

    synchronized private void readBody() throws IOException {
        if (body != null)
            return;

        Integer contentLength = getHeader("Content-Length", Integer.class);
        
        int maxBytes = contentLength != null ? contentLength : 16*1024*1024;
        
        StringBuilder sb = new StringBuilder();
        
        String charset = getCharset(getHeader("Content-Type"));

        byte[] data = new byte[4096];
        int count;
        while (maxBytes > 0 && (count = inputStream.read(data, 0, Math.min(data.length, maxBytes))) != -1) {
            sb.append(new String(data, 0, count, charset));
            maxBytes -= count;
        }

        body = sb.toString();
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public <T> T getParameter(String name, Class<T> target) {
        return cooerce(getParameter(name), target);
    }
    
    private static final DateFormat[] DATE_FORMATS = {
        new SimpleDateFormat("d MMM yy"),
        new SimpleDateFormat("yyyy M d"),
        new SimpleDateFormat("yyyy MMM d"),
        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
    };
    public static final Pattern PRE_PROCESS_PATTERN = Pattern.compile("(?<!GMT)[\\s,/\\.-]+");
    
    private <T> T cooerce(String value, Class<T> target) {
        if (value == null)
            return null;

        if (target == Date.class) {
            for (DateFormat format : PicoHttp.HTTP_HEADER_DATE_FORMATS) {
                try {
                    @SuppressWarnings("unchecked")
                    final T date = (T) format.parse(value);
                    return date;
                }
                catch (ParseException e) {
                }
            }
            
            // Courtesy of Stripe's DateTypeConverter
            String d = PRE_PROCESS_PATTERN.matcher(value).replaceAll(" ");
            d = checkAndAppendYear(d);
            for (DateFormat format : DATE_FORMATS) {
                try {
                    @SuppressWarnings("unchecked")
                    final T date = (T) format.parse(d);
                    return date;
                }
                catch (ParseException e) {
                }
            }
            
            return null;
        }

        try {
            Constructor<T> constructor = target.getConstructor(String.class);

            return constructor.newInstance(value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    private String checkAndAppendYear(String input) {
        // Count the spaces, date components = spaces + 1
        int count = 0;
        for (char ch : input.toCharArray()) {
            if (ch == ' ') ++count;
        }

        // Looks like we probably only have a day and month component, that won't work!
        if (count == 1) {
            input += " " + Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR);
        }
        return input;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public <T> T getHeader(String name, Class<T> target) {
        return cooerce(getHeader(name), target);
    }
}

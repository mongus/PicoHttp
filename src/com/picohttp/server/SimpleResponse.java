package com.picohttp.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SimpleResponse implements Response {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, Object> headers = new HashMap<String, Object>();
    private byte[] body;
    
    private static final String CRLF = "\r\n";

    @Override
    public void write(OutputStream os) throws IOException {
        PrintStream out = new PrintStream(os);
        
        // First line
        out.print("HTTP/1.0 "+statusCode+' '+statusMessage+CRLF);

        // Headers
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            String key = header.getKey();
            Object value = header.getValue();
            if (value instanceof Date)
                value = PicoHttp.HTTP_HEADER_DATE_FORMATS[0].format((Date) value);
            out.print(key+": "+value.toString()+CRLF);
        }

        out.print(CRLF);

        if (body != null)
            out.write(body);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public void setHeader(String name, Object value) {
        headers.put(name, value);
    }
    
    public Object getHeader(String name) {
        return headers.get(name);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        if (body == null)
            body = new byte[0];
        
        this.body = body;
        
        setHeader("Content-Length", body.length);
    }
    
    public void setBody(String body) {
        String contentType = (String) getHeader("Content-Type");
        String charset = "UTF-8";
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded") && contentType.contains("charset="))
            charset = contentType.replaceAll(".*charset=(.*)", "$1");
        try {
            setBody(body.getBytes(charset));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}

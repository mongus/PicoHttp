package com.picohttp.server;

public interface RequestHandler {
    public Response execute(Request request);
}

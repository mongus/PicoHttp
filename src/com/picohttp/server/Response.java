package com.picohttp.server;

import java.io.IOException;
import java.io.OutputStream;

public interface Response {
    public void write(OutputStream out) throws IOException;
}

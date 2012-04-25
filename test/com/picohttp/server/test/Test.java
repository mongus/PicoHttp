package com.picohttp.server.test;

import com.picohttp.server.PicoHttp;

public class Test {
    public static void main(String[] args) {
        new PicoHttp(new SnoopRequestHandler(), 6580);
    }
}

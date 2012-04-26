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

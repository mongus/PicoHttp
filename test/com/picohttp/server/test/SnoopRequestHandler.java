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

package com.picohttp.server.test;

import java.util.Map;

import com.picohttp.server.Request;
import com.picohttp.server.RequestHandler;
import com.picohttp.server.Response;
import com.picohttp.server.SimpleResponse;

public class SnoopRequestHandler implements RequestHandler {
    @Override
    public Response execute(Request request) {
        SimpleResponse response = new SimpleResponse();

        response.setHeader("Content-Type", "text/html");

        StringBuilder sb = new StringBuilder(
                "<!DOCTYPE html><html><head><title>Snoop Request Handler</title></head><body><h1>Snoop Request Handler</h1>");
        
        sb.append("<h2>Info</h2>");
        sb.append("<div>Request Protocol: ").append(request.getProtocol()).append("</div>");
        sb.append("<div>Request Method: ").append(request.getMethod()).append("</div>");
        sb.append("<div>Request URI: ").append(escapeHtml(request.getUri())).append("</div>");
        sb.append("<div>Host: ").append(request.getHostname()).append("</div>");
        sb.append("<div>Path: ").append(request.getPath()).append("</div>");
        sb.append("<div>Query String: ").append(escapeHtml(request.getQueryString())).append("</div>");

        sb.append("<h2>Headers</h2>");

        for (Map.Entry<String, String> header : request.getHeaders().entrySet())
            sb.append("<div>").append(header.getKey()).append(": ").append(header.getValue())
                    .append("</div>");

        Map<String, String> parameters = request.getParameters();

        if (parameters.size() > 0) {
            sb.append("<h2>Parameters</h2>");

            for (Map.Entry<String, String> parameter : parameters.entrySet())
                sb.append("<div>").append(parameter.getKey()).append(" = ")
                        .append(parameter.getValue()).append("</div>");
        }

        sb.append("</body></html>");

        response.setBody(sb.toString());

        return response;
    }
    
    public String escapeHtml(String string) {
        if (string == null)
            return null;
        
        return string.replaceAll("&", "&amp;");
    }
}

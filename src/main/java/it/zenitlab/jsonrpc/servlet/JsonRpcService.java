/*
Copyright 2016 Zenit Srl

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package it.zenitlab.jsonrpc.servlet;

import it.zenitlab.jsonrpc.commons.JsonRpcError;
import it.zenitlab.jsonrpc.commons.JsonRpcResponse;

public abstract class JsonRpcService {

    protected Integer requestId;
    protected String sessionKey;
    protected String remoteAddress;
    protected String hostname;

    public JsonRpcService() {
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public JsonRpcResponse buildResponse(Object result) {
        JsonRpcResponse res = new JsonRpcResponse();
        res.setError(null);
        res.setResult(result);
        return res;
    }

    public JsonRpcResponse buildError(int code, String detailedMessage, String userMessage) {
        JsonRpcResponse res = new JsonRpcResponse();
        JsonRpcError err = new JsonRpcError();
        err.setCode(code);
        err.setDetailedMessage(detailedMessage);
        err.setUserMessage(userMessage);
        res.setError(err);
        res.setResult(null);
        return res;
    }
}

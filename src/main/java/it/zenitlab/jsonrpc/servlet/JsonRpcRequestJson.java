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

import com.google.gson.JsonElement;

public class JsonRpcRequestJson {

    private String jsonrpc, method;
    Object id;
    private String sessionkey; //non-standard json-rpc
    private JsonElement params;

    public JsonRpcRequestJson() {
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSessionkey() {
        return sessionkey;
    }

    public void setSessionkey(String sessionkey) {
        this.sessionkey = sessionkey;
    }

    public JsonElement getParams() {
        return params;
    }

    public void setParams(JsonElement params) {
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append("id: ").append(id).append(", jsonrpc: ").append(jsonrpc).append(", method: ").append(method);
        sb.append(", sessionkey: ").append(sessionkey);
        return sb.toString();
    }    
}

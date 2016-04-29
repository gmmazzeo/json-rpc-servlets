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

import it.zenitlab.jsonrpc.commons.JsonRpcException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.zenitlab.jsonrpc.commons.JsonRpcError;
import it.zenitlab.jsonrpc.commons.JsonRpcResponse;
import it.zenitlab.sessionmanager.SessionInfo;
import it.zenitlab.sessionmanager.SessionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

public class JsonRpcServlet extends HttpServlet {

    Class serviceClass;
    private ArrayList<JsonRpcWsDescriptor> wsDescriptor;
    private HashMap<String, JsonRpcWsDescriptor> wsMethods = new HashMap<String, JsonRpcWsDescriptor>();
    private String serviceListKeyword;
    private Logger logger;
    private DBConnectionMonitor dbConnectionMonitor;
    private LogAppendersProvider logAppenderProvider;
    private int parametersMaxLogLength;
    private String callback;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String qs = request.getQueryString();

        if (qs != null && qs.toLowerCase().equals(serviceListKeyword.toLowerCase())) {
            writeWsDescriptors(response);
            return;
        }

        if (qs != null && qs.toLowerCase().equals("rpcdelegate")) {
            writeRpcDelegate(response);
            return;
        }

        callback = request.getHeader("callback");

        JsonRpcRequestJson jsonRequest = parseRequest(request, response);
        if (jsonRequest == null) {
            return;
        }

        SessionInfo is = null;
        String username = "";
        if (jsonRequest.getSessionkey() != null) {
            is = SessionManager.get(jsonRequest.getSessionkey());
            if (is != null) {
                username = " username: " + is.getUsername();
                SessionManager.updateExpiration(jsonRequest.getSessionkey());
            }
        }
        if (logAppenderProvider != null) {
            for (Appender a : logAppenderProvider.getAppenders(jsonRequest.getSessionkey())) {
                logger.addAppender(a);
            }
        }
        logger.info(jsonRequest + username);

        //get a service class instance
        JsonRpcService serviceInstance = null;
        try {
            serviceInstance = (JsonRpcService) serviceClass.newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(JsonRpcServlet.class.getName()).fatal(serviceClass.getName() + " instantiation error", ex);
            throw new ServletException(ex.getCause());
        } catch (IllegalAccessException ex) {
            Logger.getLogger(JsonRpcServlet.class.getName()).fatal(serviceClass.getName() + " access error", ex);
            throw new ServletException(ex.getCause());
        }

        JsonRpcWsDescriptor ws = wsMethods.get(jsonRequest.getMethod());
        if (ws == null) {
            writeError(response, jsonRequest.getId(), JsonRpcError.METHOD_NOT_FOUND, "Method \"" + jsonRequest.getMethod() + "\" not found", null);
            return;
        }

        if (ws.getRolesAllowed()!= null && !ws.getRolesAllowed().isEmpty()) { //devo controllare se l'utente è abilitato ad invocare il metodo
            if (is == null || !ws.isRoleAllowed(is.getRoles())) {
                writeAuthorizationError(response);
                return;
            }
        }

        Object[] oParams = new Object[ws.getParameterTypes().size()];

        if (jsonRequest.getParams() == null && oParams.length > 0) {
            writeError(response, jsonRequest.getId(), JsonRpcError.INVALID_PARAMS, oParams.length + " params required, no param found.", null);
            return;
        }
        int initialConnections = 0;
        if (dbConnectionMonitor != null) {
            initialConnections = dbConnectionMonitor.getConnectionCount();
        }
        Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm").create();
        if (jsonRequest.getParams().isJsonArray()) {
            JsonArray ja = jsonRequest.getParams().getAsJsonArray();
            if (ja.size() != oParams.length) {
                writeError(response, jsonRequest.getId(), JsonRpcError.INVALID_PARAMS, oParams.length + " params required, " + ja.size() + " param found.", null);
                return;
            }
            String params = "[";
            ArrayList<Type> tipiParametri = ws.getParameterTypes();
            for (int i = 0; i < oParams.length; i++) {
                if (i > 0) {
                    params += ",";
                }
                JsonElement je = ja.get(i);
                String p = je.toString();
                if (p.length() > parametersMaxLogLength) {
                    p = p.substring(0, parametersMaxLogLength) + "...";
                }
                params += p;
                if (je != null) {
                    oParams[i] = gson.fromJson(je, tipiParametri.get(i));
                }
            }
            params += "]";
            logger.debug(params);
        } else {
            JsonObject jo = jsonRequest.getParams().getAsJsonObject();
            int nParams = jo.entrySet().size();
            if (nParams != oParams.length) {
                writeError(response, jsonRequest.getId(), JsonRpcError.INVALID_PARAMS, oParams.length + " params required, " + nParams + " param found.", null);
                return;
            }
            ArrayList<String> nomiParametri = ws.getParameterNames();
            ArrayList<Type> tipiParametri = ws.getParameterTypes();
            String params = "{";
            for (int i = 0; i < oParams.length; i++) {
                String nome = nomiParametri.get(i);
                JsonElement je = jo.get(nome);
                String p = je.toString();
                if (p.length() > parametersMaxLogLength) {
                    p = p.substring(0, parametersMaxLogLength) + "...";
                }
                params += nome + ":" + p;
                if (je != null) {
                    oParams[i] = gson.fromJson(je, tipiParametri.get(i));
                }
            }
            params += "}";
            logger.debug(params);
        }

        serviceInstance.setSessionKey(jsonRequest.getSessionkey());
        serviceInstance.setRemoteAddress(request.getRemoteAddr());
        serviceInstance.setHostname(request.getRemoteHost());

        JsonRpcResponse jsonResponse = null;
        if (ws.getReturnType().equals(JsonRpcResponse.class)) {
            try {
                jsonResponse = (JsonRpcResponse) (ws.getReflectedMethod().invoke(serviceInstance, oParams));
            } catch (Exception e) {
                logger.error("Errore interno", e);
                writeError(response, jsonRequest.getId(), JsonRpcError.INTERNAL_ERROR, e.getCause().getMessage(), null);
                return;
            } finally {
                if (dbConnectionMonitor != null) {
                    int finalConnections = dbConnectionMonitor.getConnectionCount();
                    if (finalConnections > initialConnections) {
                        logger.debug("================> Number of connections increased by " + (finalConnections - initialConnections) + " <======================");
                    }
                }
            }
        } else {
            try {
                Object result = ws.getReflectedMethod().invoke(serviceInstance, oParams);
                jsonResponse = new JsonRpcResponse();
                jsonResponse.setError(null);
                jsonResponse.setResult(result);
            } catch (Exception e) {
                if (e.getCause() instanceof JsonRpcException) {
                    JsonRpcException je = (JsonRpcException) e.getCause();
                    writeError(response, jsonRequest.getId(), je.getCode(), je.getDetailedMessage(), je.getUserMessage());
                } else {
                    logger.error("Internal error", e);
                    writeError(response, jsonRequest.getId(), JsonRpcError.INTERNAL_ERROR, e.getCause().getMessage(), null);
                }
                return;
            } finally {
                if (dbConnectionMonitor != null) {
                    int finalConnections = dbConnectionMonitor.getConnectionCount();
                    if (finalConnections > initialConnections) {
                        logger.debug("================> Number of connections increased by " + (finalConnections - initialConnections) + " <======================");
                    }
                }
            }
        }
        jsonResponse.setId(jsonRequest.getId());
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            if (callback != null) {
                out.print(callback + "(");
            }
            String res = gson.toJson(jsonResponse);
            out.print(res);
            if (callback != null) {
                out.print(")");
            }
            out.println();
            //logger.debug(res);
        } finally {
            out.close();
        }
    }

    private void initWsDescriptor() {

        wsDescriptor = new ArrayList<JsonRpcWsDescriptor>();
        wsMethods = new HashMap<String, JsonRpcWsDescriptor>();

        for (Method m : serviceClass.getMethods()) {
            if (m.isAnnotationPresent(JsonRpcMethod.class)) {
                JsonRpcWsDescriptor wsd = new JsonRpcWsDescriptor(m);
                wsDescriptor.add(wsd);
                wsMethods.put(wsd.getMethodName(), wsd);
            }
        }
        Collections.sort(wsDescriptor);
    }

    private void writeWsDescriptors(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            for (JsonRpcWsDescriptor wsd : wsDescriptor) {
                out.print(wsd.getReturnType() == null ? "void" : wsd.getReturnType().toString().replace("class ", ""));
                out.print(" " + wsd.getMethodName() + "(");
                if (wsd.getParameterNames().size() > 0) {
                    out.print(wsd.getParameterNames().get(0));
                }
                for (int i = 1; i < wsd.getParameterNames().size(); i++) {
                    out.print(", " + wsd.getParameterNames().get(i));
                }
                out.println("); - " + wsd.getDescription());
            }
        } finally {
            out.close();
        }
    }

    private void writeRpcDelegate(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String tab = "   ";
        out.println("public class " + serviceClass.getSimpleName() + "RpcDelegate extends it.zenitlab.jsonrpc.client.JsonRpcClient {\n");
        out.println(tab + "final static String urlString = \"PUT YOUR WS URL HERE\";\n");
        out.println(tab + "public " + serviceClass.getSimpleName() + "RpcDelegate(String sessionKey) throws java.net.MalformedURLException {");
        out.println(tab + tab + "super(urlString, sessionKey);");
        out.println(tab + "}\n");
        out.println(tab + "public " + serviceClass.getSimpleName() + "RpcDelegate() throws java.net.MalformedURLException {");
        out.println(tab + tab + "super(urlString);");
        out.println(tab + "}\n");
        out.println();

        try {
            for (JsonRpcWsDescriptor wsd : wsDescriptor) {
                out.println(tab + "/**\n" + tab + " * " + wsd.getDescription() + "\n" + tab + " **/");
                out.print(tab + "public " + (wsd.getReturnType() == null ? "void" : wsd.getReturnType().toString().replace("class ", "").replace("java.lang.", "")));
                out.print(" " + wsd.getMethodName() + "(");
                if (wsd.getParameterNames().size() > 0) {
                    out.print(wsd.getParameterTypes().get(0).toString().replace("class ", "").replace("java.lang.", "")+" "+wsd.getParameterNames().get(0));
                }
                for (int i = 1; i < wsd.getParameterNames().size(); i++) {
                    out.print(", " + wsd.getParameterTypes().get(i).toString().replace("class ", "").replace("java.lang.", "")+" "+wsd.getParameterNames().get(i));
                }
                out.println(") throws it.zenitlab.jsonrpc.commons.JsonRpcException {");
                if (wsd.getReturnType().toString().contains("<")) {
                    out.print(tab + tab + "com.google.gson.reflect.TypeToken t=new com.google.gson.reflect.TypeToken<" + wsd.getReturnType().toString().replace("class ", "") + ">() {};\n");
                    out.print(tab + tab + "return (" + wsd.getReturnType().toString().replace("class ", "") + ")call(\"" + wsd.getMethodName() + "\", t.getType()");
                } else {
                    out.print(tab + tab + "return (" + wsd.getReturnType().toString().replace("class ", "") + ")call(\"" + wsd.getMethodName() + "\", " + wsd.getReturnType().toString().replace("class ", "") + ".class");
                }
                for (int i = 0; i < wsd.getParameterNames().size(); i++) {
                    out.print(", " + wsd.getParameterNames().get(i));
                }
                out.println(");");
                out.println(tab + "}");
            }
            out.print("}");
        } finally {
            out.close();
        }
    }

    public void writeAuthorizationError(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    public void writeError(HttpServletResponse response, Object id, int code, String detailedMessage, String userMessage) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        JsonRpcError error = new JsonRpcError();
        error.setCode(code);
        error.setDetailedMessage(detailedMessage);
        error.setUserMessage(userMessage == null ? "Internal error" : userMessage);
        JsonRpcResponse jsonResponse = new JsonRpcResponse();
        jsonResponse.setId(id);
        jsonResponse.setError(error);
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        try {
            String s = gson.toJson(jsonResponse);
            out.println(s);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            out.close();
        }
    }

    public JsonRpcRequestJson parseRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //get parameters from json request
        JsonRpcRequestJson jsonRequest = new JsonRpcRequestJson();
        String qs = request.getQueryString();
        if (qs == null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            qs = in.readLine();
            if (qs == null) {
                PrintWriter out = response.getWriter();
                try {
                    out.println("Ok");
                } catch (Exception e) {
                    System.out.println(e);
                } finally {
                    out.close();
                }
                return null;
            }
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(qs);
        JsonObject jo = je.getAsJsonObject();

        je = jo.get("jsonrpc");
        if (je == null) {
            writeError(response, null, JsonRpcError.INVALID_REQUEST, "jsonrpc parameter missing", null);
            return null;
        } else {
            jsonRequest.setJsonrpc(je.getAsString());
            if (!jsonRequest.getJsonrpc().equals("2.0")) {
                writeError(response, null, JsonRpcError.INVALID_REQUEST, "jsonrpc parameter must be \"2.0\"", null);
                return null;
            }
        }

        je = jo.get("method");
        if (je == null) {
            writeError(response, null, JsonRpcError.INVALID_REQUEST, "method parameter missing", null);
            return null;
        } else {
            jsonRequest.setMethod(je.getAsString());
        }

        je = jo.get("id");
        if (je != null) {
            try {
                jsonRequest.setId(je.getAsString());
            } catch (Exception e1) {
                try {
                    jsonRequest.setId(je.getAsNumber());
                } catch (Exception e2) {
                    writeError(response, null, JsonRpcError.INVALID_REQUEST, "Invalid id parameter format", null);
                    return null;
                }
            }
        }

        je = jo.get("sessionkey");
        if (je != null) {
            jsonRequest.setSessionkey(je.getAsString());
        }

        jsonRequest.setParams(jo.get("params"));

        return jsonRequest;
    }

    public void writeServiceList(ArrayList<JsonRpcWsDescriptor> wsDescriptor, HttpServletResponse response) throws IOException {
        JsonRpcResponse jsonResponse = new JsonRpcResponse();
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        try {
            String s = gson.toJson(jsonResponse);
            out.println(s);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    @Override
    public void init() throws ServletException {
        super.init();
        String serviceClassName = getInitParameter("serviceClass");
        try {
            serviceClass = Class.forName(serviceClassName);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JsonRpcServlet.class.getName()).fatal(serviceClassName + " class not found!", ex);
            throw new ServletException(serviceClassName + " class not found");
        }
        initWsDescriptor();
        serviceListKeyword = getInitParameter("serviceListKeyword");
        logger = Logger.getLogger(serviceClass);

        String lapClassName = getInitParameter("logAppenderProvider");
        if (lapClassName != null) {
            try {
                Class lapClass = Class.forName(lapClassName);
                logAppenderProvider = (LogAppendersProvider) lapClass.newInstance();
            } catch (ClassNotFoundException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        String dbcmClassName = getInitParameter("dbConnectionMonitor");
        if (dbcmClassName != null) {
            try {
                Class dbcmClass = Class.forName(dbcmClassName);
                dbConnectionMonitor = (DBConnectionMonitor) dbcmClass.newInstance();
            } catch (ClassNotFoundException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        String parametersMaxLogLengthS = getInitParameter("parametersMaxLogLength");
        if (parametersMaxLogLengthS != null) {
            try {
                parametersMaxLogLength = Integer.parseInt(parametersMaxLogLengthS);
            } catch (NumberFormatException ex) {
                java.util.logging.Logger.getLogger(JsonRpcServlet.class.getName()).log(Level.WARNING, "Impostato 1000 come valore di lunghezza massima dei parametri", ex);
                parametersMaxLogLength = 1000;
            }
        } else {
            parametersMaxLogLength = 1000;
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>
}

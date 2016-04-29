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

import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.StringTokenizer;

public class JsonRpcWsDescriptor implements Comparable {

    String methodName, description;
    Type returnType;
    ArrayList<String> parameterDescription = new ArrayList<String>();
    ArrayList<Type> parameterTypes = new ArrayList<Type>();
    ArrayList<String> parameterNames = new ArrayList<String>();
    HashSet<String> rolesAllowed = null;
    Method reflectedMethod;

    public JsonRpcWsDescriptor() {
    }

    public JsonRpcWsDescriptor(Method m) {
        methodName = m.getName();
        description = m.getAnnotation(JsonRpcMethod.class).description();
        returnType = m.getGenericReturnType();
        String[] paramNames = m.getAnnotation(JsonRpcMethod.class).paramNames();
        int i = 0;
        for (Type c : m.getGenericParameterTypes()) {
            String nomeParametro = i < paramNames.length ? paramNames[i] : "?";
            String descParametro = c.toString().replace("class ", "") + " " + nomeParametro;
            parameterDescription.add(descParametro);
            parameterTypes.add(c);
            parameterNames.add(nomeParametro);
            i++;
        }
        if (m.getAnnotation(JsonRpcMethod.class).rolesAllowed() != null) {
            rolesAllowed = new HashSet<String>();
            rolesAllowed.addAll(Arrays.asList(m.getAnnotation(JsonRpcMethod.class).rolesAllowed()));
        }
        reflectedMethod = m;
    }

    public JsonRpcWsDescriptor(String metodo, String descrizione, ArrayList<String> parametri) {
        this.methodName = metodo;
        this.description = descrizione;
        this.parameterDescription = parametri;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getParameterDescription() {
        return parameterDescription;
    }

    public void setParameterDescription(ArrayList<String> parameterDescription) {
        this.parameterDescription = parameterDescription;
    }

    public ArrayList<Type> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(ArrayList<Type> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public ArrayList<String> getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(ArrayList<String> parametersName) {
        this.parameterNames = parametersName;
    }

    public HashSet<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public void setRolesAllowed(HashSet<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    public void addParameter(String parameter) {
        parameterDescription.add(parameter);
        StringTokenizer st = new StringTokenizer(parameter, " ");
        String type = st.nextToken();
        String name = st.nextToken();
        if (type.equals("int") || type.equals("Integer")) {
            parameterTypes.add(Integer.class);
        } else if (type.equals("String")) {
            parameterTypes.add(String.class);
        } else if (type.equals("Date")) {
            parameterTypes.add(Date.class);
        } else if (type.equals("Double") || type.equals("double") || type.equals("Float") || type.equals("float")) {
            parameterTypes.add(Double.class);
        }
        parameterNames.add(name);
    }

    public Method getReflectedMethod() {
        return reflectedMethod;
    }

    public void setReflectedMethod(Method reflectedMethod) {
        this.reflectedMethod = reflectedMethod;
    }

    public boolean isRoleAllowed(String role) {
        return rolesAllowed == null || role.equals("superadmin") || rolesAllowed.contains(role);
    }

    public boolean isRoleAllowed(Collection<String> roles) {
        if (rolesAllowed == null || roles.contains("superadmin")) {
            return true;
        }
        for (String r : roles) {
            if (rolesAllowed.contains(r)) {
                return true;
            }
        }
        return false;
    }

    public int compareTo(Object o) {
        return methodName.compareTo(((JsonRpcWsDescriptor) o).methodName);
    }
}

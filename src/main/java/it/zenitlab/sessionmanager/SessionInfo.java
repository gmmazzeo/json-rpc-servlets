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

package it.zenitlab.sessionmanager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class SessionInfo {

    protected HashMap<String, Object> info = new HashMap<String, Object>();
    protected String username, key;
    protected HashSet<String> roles = new HashSet<String>();
    protected Date expiration, creation, lastAccess;
    protected long duration = 1000l * 60 * 60 * 3;
    protected boolean stayConnected;

    public SessionInfo() {
    }

    public SessionInfo(String key, String username, long duration, boolean stayConnected, HashSet<String> roles) {
        this.key = key;
        if (roles != null) {
            this.roles = roles;
        }
        Date now = new Date();
        creation = now;
        expiration = new Date(now.getTime() + duration);
        lastAccess = now;
    }

    public Object getInfo(String chiave) {
        return info.get(chiave);
    }

    public Object putInfo(String chiave, Object valore) {
        return info.put(chiave, valore);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getCreation() {
        return creation;
    }

    public void setCreation(Date creation) {
        this.creation = creation;
    }

    public HashMap<String, Object> getInfo() {
        return info;
    }

    public void setInfo(HashMap<String, Object> info) {
        this.info = info;
    }

    public HashSet<String> getRoles() {
        return roles;
    }

    public void setRoles(HashSet<String> roles) {
        this.roles = roles;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
    }

    public boolean getStayConnected() {
        return stayConnected;
    }

    public void setStayConnected(boolean stayConnected) {
        this.stayConnected = stayConnected;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

}

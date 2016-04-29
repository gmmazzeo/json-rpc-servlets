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

public abstract class PersistentSessionManager {

    public abstract HashMap<String, SessionInfo> loadSessions();

    public abstract void deleteExpiredSessions();

    public abstract void deleteSession(String key);

    public abstract void createSession(String key, SessionInfo info);

    public abstract void updateExpiration(String key, Date expiration);
    
}

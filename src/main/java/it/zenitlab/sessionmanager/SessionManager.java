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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class SessionManager {

    private static HashMap<String, SessionInfo> sessions;
    public static final long duration = 1000l * 60 * 60 * 3; //3 hours
    public static final long unlimitedDuration = 1000l * 60 * 60 * 24 * 30; //30 days
    public static final long expiredSessionDeletionFrequency = 1000l * 60 * 60; //1 hour
    private static PersistentSessionManager spm;

    public static void init(PersistentSessionManager spm1) {
        spm = spm1;
        sessions = new HashMap<String, SessionInfo>();
        sessions = spm.loadSessions();
        spm.deleteExpiredSessions();

        //start the timer for deleting expired sessions
        Timer timer = new Timer();
        TimerTask task = new CancellazioneSessioniScaduteTask(spm);
        timer.schedule(task, expiredSessionDeletionFrequency, expiredSessionDeletionFrequency);
    }

    public static synchronized void add(String chiave, SessionInfo info) {
        spm.createSession(chiave, info);
        sessions.put(chiave, info);
    }

    public static synchronized SessionInfo get(String chiave) {
        SessionInfo is=sessions.get(chiave);
        if (is!=null && is.getExpiration().after(new Date())) {
            return is;
        }
        return null;
    }

    public static synchronized SessionInfo remove(String chiave) {
        spm.deleteSession(chiave);
        return sessions.remove(chiave);
    }

    public static synchronized void updateExpiration(String key) {
        SessionInfo info = sessions.get(key);
        if (info != null) {
            Date scadenza = new Date(new Date().getTime() + info.getDuration());
            Date scadenzaAttuale = info.getExpiration();
            if (scadenza.getTime()-scadenzaAttuale.getTime()>info.getDuration()/2) {
                info.setExpiration(scadenza);
                spm.updateExpiration(key, scadenza);
            }
        }
    }

    public static ArrayList<SessionInfo> listAll() {        
        return new ArrayList<SessionInfo>(sessions.values());
    }
}
class CancellazioneSessioniScaduteTask extends TimerTask {

    PersistentSessionManager spm;

    public CancellazioneSessioniScaduteTask(PersistentSessionManager spm) {
        this.spm = spm;
    }

    public void run() {
        spm.deleteExpiredSessions();
    }
}

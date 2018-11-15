package org.jenkinsci.plugins.qpidjmsbuildtrigger;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jenkinsci.plugins.qpidjmsbuildtrigger.extensions.MessageQueueListener;

import hudson.Extension;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

@Extension
public class RemoteBuildListener extends MessageQueueListener implements MessageListener {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PARAMETER = "parameter";
    private static final String PLUGIN_NAME = "Remote Builder";
    
    private static final Logger LOGGER = Logger.getLogger(RemoteBuildTrigger.class.getName());
    private final Set<RemoteBuildTrigger> triggers = new CopyOnWriteArraySet<RemoteBuildTrigger>();
    
    @Override
    public String getName() {
    	return PLUGIN_NAME;
    }
    
    @Override
    public String getAppId() {
    	return RemoteBuildTrigger.PLUGIN_APPID;
    }
    
    @Override
    public void onReceive(String queueName, String contentType, Map<String, Object> headers, byte[] body) {
        if (CONTENT_TYPE_JSON.equals(contentType)) {
            try {
                String msg = new String(body, "UTF-8");
                try {
                    JSONObject json = (JSONObject) JSONSerializer.toJSON(msg);
                    for (RemoteBuildTrigger t : triggers) {

                        if (t.getRemoteBuildToken() == null) {
                            LOGGER.log(Level.WARNING, "ignoring AMQP trigger for project {0}: no token set", t.getProjectName());
                            continue;
                        }

                        if (t.getProjectName().equals(json.getString(KEY_PROJECT))
                                && t.getRemoteBuildToken().equals(json.getString(KEY_TOKEN))) {
                            if (json.containsKey(KEY_PARAMETER)) {
                                t.scheduleBuild(queueName, json.getJSONArray(KEY_PARAMETER));
                            } else {
                                t.scheduleBuild(queueName, null);
                            }
                        }
                    }
                } catch (JSONException e) {
                    LOGGER.warning("JSON format string: " + msg);
                    LOGGER.warning(e.getMessage());
                }
            } catch (UnsupportedEncodingException e) {
                LOGGER.warning("Unsupported encoding. Is message body is not string?");
            }
        }    	
    }

    @Override
    public void onMessage(Message message) {
    	LOGGER.info("onMessage() m=" + message.toString());
    }
    
    public void addTrigger(RemoteBuildTrigger trigger) {
        triggers.add(trigger);
    }

    public void removeTrigger(RemoteBuildTrigger trigger) {
        triggers.remove(trigger);
    }
    
    public  Set<RemoteBuildTrigger> getTriggers(){
        return triggers;
    }   
}

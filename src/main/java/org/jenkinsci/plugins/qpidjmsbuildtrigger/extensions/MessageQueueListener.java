package org.jenkinsci.plugins.qpidjmsbuildtrigger.extensions;

import java.util.Map;
import java.util.logging.Logger;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

public abstract class MessageQueueListener implements ExtensionPoint {

	private static final Logger LOGGER = Logger.getLogger(MessageQueueListener.class.getName());

    public abstract String getName();
    
    public abstract String getAppId();
    
    public abstract void onReceive(String queueName, String contentType, Map<String, Object> headers, byte[] body);
    
    public static void fireOnReceive(String appId, String queueName, String contentType, Map<String, Object> headers, byte[] body) {
        LOGGER.entering("MessageQueueListener", "fireOnReceive");
        for (MessageQueueListener l : all()) {
            if (appId.equals(l.getAppId())) {
                l.onReceive(queueName, contentType, headers, body);
            }
        }
    }
    
    public static ExtensionList<MessageQueueListener> all() {
        return Jenkins.getInstance().getExtensionList(MessageQueueListener.class);
    }
}

package org.jenkinsci.plugins.qpidjmsbuildtrigger.extensions;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

public abstract class MessageQueueListener implements ExtensionPoint {

	private static final Logger LOGGER = Logger.getLogger(MessageQueueListener.class.getName());

    public abstract String getName();
    
    public abstract String getAppId();
    
    public abstract void onBind(String queueName);
    
    public abstract void onUnbind(String queueName);
    
    public abstract void onReceive(String queueName, String contentType, Map<String, Object> headers, byte[] body);
    
    public static void fireOnReceive(String appId,
            String queueName,
            String contentType,
            Map<String, Object> headers,
            byte[] body) {
        LOGGER.entering("MessageQueueListener", "fireOnReceive");
        for (MessageQueueListener l : all()) {
            if (appId.equals(l.getAppId())) {
                l.onReceive(queueName, contentType, headers, body);
            }
        }
    }
    public static void fireOnBind(HashSet<String> appIds, String queueName) {
        LOGGER.entering("MessageQueueListener", "fireOnBind");
        for (MessageQueueListener l : all()) {
            if (appIds.contains(l.getAppId())) {
                l.onBind(queueName);
            }
        }
    }
    public static void fireOnUnbind(HashSet<String> appIds, String queueName) {
        LOGGER.entering("MessageQueueListener", "fireOnUnbind");
        for (MessageQueueListener l : all()) {
            if (appIds.contains(l.getAppId())) {
                l.onUnbind(queueName);
            }
        }
    }
    public static ExtensionList<MessageQueueListener> all() {
        return Jenkins.getInstance().getExtensionList(MessageQueueListener.class);
    }
}

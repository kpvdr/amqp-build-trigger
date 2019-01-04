package org.jenkinsci.plugins.amqpbuildtrigger.extensions;

import java.util.logging.Logger;
import javax.jms.Message;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

public abstract class MessageQueueListener implements ExtensionPoint {

	private static final Logger LOGGER = Logger.getLogger(MessageQueueListener.class.getName());

	public abstract void onReceive(Message message);
	
	public static void fireOnReceive(Message message) {
		LOGGER.entering("MessageQueueListener", "fireOnReceive");
		System.out.println("MessageQueueListeners: " + all().size());
		for (MessageQueueListener l : all()) {
			l.onReceive(message);
		}
	}
	
    public static ExtensionList<MessageQueueListener> all() {
        return Jenkins.getInstance().getExtensionList(MessageQueueListener.class);
    }
}

package org.jenkinsci.plugins.amqpbuildtrigger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jenkinsci.plugins.amqpbuildtrigger.extensions.MessageQueueListener;

import hudson.Extension;

@Extension
public class RemoteBuildListener extends MessageQueueListener implements MessageListener {
	private static final String MSG_TYPE_KEY = "msg_type";
	private static final String MSG_TYPE_VALUE = "AMQP_build_trigger";
	private static final String PROJ_TOKEN_KEY = "jenkins_project_token";
	private static final String PROJ_ACTION_KEY = "jenkins_project_action";
	private static final String PROJ_ACTION_BUILD_VALUE = "build";
    
    private static final Logger LOGGER = Logger.getLogger(RemoteBuildTrigger.class.getName());
    private final Set<RemoteBuildTrigger> triggers = new CopyOnWriteArraySet<RemoteBuildTrigger>();
    private String queueName;

    public RemoteBuildListener() {}
    
    public RemoteBuildListener(String queueName) {
    	this.queueName = queueName;
    }
    
    public void setQueueName(String queueName) {
    	this.queueName = queueName;
    }
    
    @Override
    public void onReceive(Message msg) {
    	System.out.println("onReceive()");
    	try {
    		String msgType = getMessageProperty(msg, MSG_TYPE_KEY);
    		String projToken = getMessageProperty(msg, PROJ_TOKEN_KEY);
    		String projAction = getMessageProperty(msg, PROJ_ACTION_KEY);
    		if (msgType != null && projToken != null && projAction != null) {
    			if (msgType.equals(MSG_TYPE_VALUE)) {
                    for (RemoteBuildTrigger t : triggers) {
                    	if (t.getRemoteBuildToken() == null) {
                            LOGGER.log(Level.WARNING, "Ignoring AMQP trigger for project {0}: no token set", t.getProjectName());
                            continue;
                        }
                    	if (projToken.equals(t.getRemoteBuildToken()) && projAction.equals(PROJ_ACTION_BUILD_VALUE)) {
                    		LOGGER.info("Remote build triggered: message received: " + 
                                         MSG_TYPE_KEY + "=\"" + MSG_TYPE_VALUE + "\", " +
                    				     PROJ_TOKEN_KEY + "=\"" + t.getRemoteBuildToken() + "\", " +
                                         PROJ_ACTION_KEY + "=\"" + PROJ_ACTION_BUILD_VALUE + "\"");
                    		t.scheduleBuild(queueName, null);
                    	}
                    }    				
    			} else {
    				LOGGER.warning("Incoming message discarded: propoerty \"" + MSG_TYPE_KEY + "\" value \"" + msgType + "\" not recognized");
    			}
    		} else {
    			LOGGER.warning("Incoming message discarded: missing required propoerties");
    		}
    	} catch (JMSException e) {
    		LOGGER.warning("Error handling incoming message: " + e.getMessage());
    	}
    }
    
    private String getMessageProperty(Message message, String propKey) throws JMSException {
    	String p = message.getStringProperty(propKey);
    	if (p == null) {
    		LOGGER.info("Incoming message: no property \"" + propKey + "\"");
    	}
    	return p;
    }
    
    @Override
    public void onMessage(Message message) {
    	try {
    		LOGGER.info("onMessage() m=" + message.toString());
    		fireOnReceive(message);
    	} catch (Exception e) {
    		LOGGER.warning("Exception thrown in RemoteBuildListener.onMessage(): " + e.getMessage());
    	}
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

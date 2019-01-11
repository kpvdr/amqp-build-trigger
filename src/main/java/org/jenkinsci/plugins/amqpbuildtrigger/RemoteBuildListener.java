package org.jenkinsci.plugins.amqpbuildtrigger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;

public class RemoteBuildListener implements MessageListener {
    
    private static final Logger LOGGER = Logger.getLogger(RemoteBuildTrigger.class.getName());
    private final Set<RemoteBuildTrigger> triggers = new CopyOnWriteArraySet<RemoteBuildTrigger>();
    private String queueName;

    //public RemoteBuildListener() {}
    
    public RemoteBuildListener(String queueName) {
    	this.queueName = queueName;
    	//System.out.println("*** DEBUG *** RemoteBuildListener() queue = " + queueName);
    }
    
    public String getQueueName() {
    	return queueName;
    }
    
    public void setQueueName(String queueName) {
    	this.queueName = queueName;
    }
    
    @Override
    public void onMessage(Message message) {
    	try {
    		LOGGER.info("onMessage() m=" + message.toString());
            for (RemoteBuildTrigger t : triggers) {
        		LOGGER.info("Remote build triggered: message received on queue " + queueName);
        		t.scheduleBuild(queueName, null);
            }    				
    	} catch (Exception e) {
    		LOGGER.warning("Exception thrown in RemoteBuildListener.onMessage(): " + e.getMessage());
    	}
    }
    
    public void addTrigger(RemoteBuildTrigger trigger) {
    	//System.out.println("*** DEBUG *** RemoteBuildListener.addTrigger() on queue " + queueName + ", trigger = " + trigger.getProjectName());
        triggers.add(trigger);
    }

    public void removeTrigger(RemoteBuildTrigger trigger) {
    	//System.out.println("*** DEBUG *** RemoteBuildListener.removeTrigger() on queue " + queueName + ", trigger = " + trigger.getProjectName());
        triggers.remove(trigger);
    }
    
    public  Set<RemoteBuildTrigger> getTriggers(){
        return triggers;
    }   
}

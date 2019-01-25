package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.util.logging.Logger;

@Extension
public class JenkinsEventListener extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(JenkinsEventListener.class.getName());

    @Override
    public final void onLoaded() {
        LOGGER.info("Starting AMQP Build Trigger");
        ConnectionManager.getInstance().initialize();
        // TODO: Start ConnectionUpdateTimer
        super.onLoaded();
    }
    
    @Override
    public final void onUpdated(Item item) {
    	LOGGER.info("Job updated: " + item.getFullName());
    	ConnectionManager.getInstance().initialize();
    	super.onUpdated(item);
    }

    @Override
    public final void onBeforeShutdown() {
        LOGGER.info("Shutting down AMQP Build Trigger");
        ConnectionManager.getInstance().shutdown();
        // TODO: Stop ConnectionUpdateTimer
        super.onBeforeShutdown();
    }

    public static JenkinsEventListener get() {
        return ItemListener.all().get(JenkinsEventListener.class);
    }
}

package org.jenkinsci.plugins.amqpbuildtrigger;

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.listeners.ItemListener;

@Extension
public class ItemListenerImpl extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(ItemListenerImpl.class.getName());
    private final ConnectionManager manager;

    public ItemListenerImpl() {
        super();
        this.manager = ConnectionManager.getInstance();
    }

    @Override
    public final void onLoaded() {
        LOGGER.info("Starting AMQP Build Trigger");
        manager.update();
        super.onLoaded();
    }

    @Override
    public final void onBeforeShutdown() {
        LOGGER.info("Shutting down AMQP Build Trigger");
        manager.shutdown();
        super.onBeforeShutdown();
    }

    public static ItemListenerImpl get() {
        return ItemListener.all().get(ItemListenerImpl.class);
    }
}

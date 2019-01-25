package com.redhat.jenkins.plugins.amqpbuildtrigger;

import java.util.Set;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;

public class AmqpMessageListener implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(AmqpBuildTrigger.class.getName());
    private final AmqpBrokerParams brokerParams;
    private final Set<AmqpBuildTrigger> triggers;

    public AmqpMessageListener(AmqpBrokerParams brokerParams, Set<AmqpBuildTrigger> triggers) {
        this.brokerParams = brokerParams;
        this.triggers = triggers;
    }

    @Override
    public void onMessage(Message message) {
        try {
            LOGGER.info("Message received on broker " + brokerParams.toString() + "; msg=" + message.toString());
            for (AmqpBuildTrigger t : triggers) {
                LOGGER.info("Remote build triggered: " + t.getProjectName());
                t.scheduleBuild(brokerParams.toString(), null);
            }
        } catch (Exception e) {
            LOGGER.warning("Exception thrown in RemoteBuildListener.onMessage(): " + e.getMessage());
        }
    }
}

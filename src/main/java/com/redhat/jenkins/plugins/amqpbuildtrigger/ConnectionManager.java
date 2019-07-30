package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import jenkins.model.Jenkins;

import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;

public class ConnectionManager implements JmsConnectionListener {

    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private Map<String, AmqpConnection> connectionMap;

    private static class InstanceHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public ConnectionManager() {
        connectionMap = new ConcurrentHashMap<String, AmqpConnection>();
    }

    protected void addBuildTrigger(AmqpBuildTrigger trigger) {
        List<AmqpBrokerParams> brokerParamsList = trigger.getAmqpBrokerParamsList();
        if (brokerParamsList != null && !brokerParamsList.isEmpty()) {
            for (AmqpBrokerParams url: brokerParamsList) {
                if (connectionMap.containsKey(url.toString())) {
                    // Add trigger to existing connection
                    if (!connectionMap.get(url.toString()).addBuildTrigger(trigger)) {
                        LOGGER.warning("ConnectionManager.addBuildTrigger(): failed to add trigger " + trigger.getProjectName() + " to existing connection");
                    }
                } else {
                    // Create new connection
                    AmqpConnection c = new AmqpConnection(url);
                    if (!c.addBuildTrigger(trigger)) {
                        LOGGER.warning("ConnectionManager.addBuildTrigger(): failed to add trigger " + trigger.getProjectName() + " to new connection");
                    }
                    connectionMap.put(url.toString(), c);
                }
            }
        }
    }

    public void initialize() {
        shutdown();
        connectionMap.clear();

        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            // Find triggers for freestyle jobs
            for (Project<?, ?> p : jenkins.getAllItems(Project.class)) {
                AmqpBuildTrigger t = p.getTrigger(AmqpBuildTrigger.class);
                if (t != null) {
                    addBuildTrigger(t);
                }
            }
           // Find triggers for Pipelines/workflow jobs
            for (WorkflowJob j : jenkins.getAllItems(WorkflowJob.class)) {
                Map<TriggerDescriptor, Trigger<?>> m = j.getTriggers();
                for (TriggerDescriptor d: m.keySet()) {
                    if (d instanceof AmqpBuildTrigger.AmqpBuildTriggerDescriptor) {
                        AmqpBuildTrigger t = (AmqpBuildTrigger) m.get(d);
                        if (t != null) {
                            addBuildTrigger(t);
                        }
                    }
                }
            }
        }
        update();
    }

    public void update() {
        for (Map.Entry<String, AmqpConnection> c: connectionMap.entrySet()) {
            c.getValue().update();
        }
    }

    public void shutdown() {
        for (Map.Entry<String, AmqpConnection> c: connectionMap.entrySet()) {
            c.getValue().shutdown();
        }
    }

    @Override
    public void onConnectionEstablished(URI remoteURI) {
        LOGGER.info(remoteURI + "Connection established");
    }

    @Override
    public void onConnectionFailure(Throwable error) {
        LOGGER.warning("Connection to broker failed: " + error.getMessage());
    }

    @Override
    public void onConnectionInterrupted(URI remoteURI) {
        LOGGER.info(remoteURI + ": Connection interrupted");
    }

    @Override
    public void onConnectionRestored(URI remoteURI) {
        LOGGER.info(remoteURI + ": Connection restored");
    }

    @Override
    public void onInboundMessage(JmsInboundMessageDispatch envelope) {
        LOGGER.info("AMQP message received: " + envelope.getMessage());
    }

    @Override
    public void onSessionClosed(Session session, Throwable cause) {
        LOGGER.info("Session " + session.toString() + " closed: " + cause.getMessage());
    }

    @Override
    public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {
        LOGGER.info("Consumer " + consumer.toString() + " closed: " + cause.getMessage());
    }

    @Override
    public void onProducerClosed(MessageProducer producer, Throwable cause) {}

}

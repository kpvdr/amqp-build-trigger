package com.redhat.jenkins.plugins.amqpbuildtrigger;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.Set;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionFactory;

public class AmqpConnection {
    private static final Logger LOGGER = Logger.getLogger(AmqpBuildTrigger.class.getName());
    private final Set<AmqpBuildTrigger> triggers = new CopyOnWriteArraySet<AmqpBuildTrigger>();
    private AmqpBrokerParams brokerParams;
    private JmsConnection connection = null;
    private Session session = null;
    private MessageConsumer messageConsumer = null;

    public AmqpConnection(AmqpBrokerParams brokerParams) {
        this.brokerParams = brokerParams;
    }

    public boolean addBuildTrigger(AmqpBuildTrigger trigger) {
        if (trigger != null) {
            return triggers.add(trigger);
        }
        return false;
    }

    public void update() {
        if (!brokerParams.isValid()) {
            shutdown();
            return;
        }
        if (connection != null &&
                !brokerParams.getBrokerUrl().equals(connection.getConnectedURI().toString().split("\\?")[0]) &&
                !brokerParams.getUsername().equals(connection.getUsername()) &&
                !brokerParams.getPassword().getPlainText().equals(connection.getPassword())) {
            if (connection != null) {
                shutdown();
            }
        }
        if (connection != null && connection.isConnected() == false) {
            shutdown();
        }
        if (connection == null) {
            open(getMessageListener());
        }
    }

    public boolean open(AmqpMessageListener listener) {
        try {
            JmsConnectionFactory factory = new JmsConnectionFactory(brokerParams.getBrokerUrl());
            if (brokerParams.getUsername().isEmpty() || brokerParams.getPassword().getPlainText().isEmpty()) {
                connection = (JmsConnection)factory.createConnection();
            } else {
                connection = (JmsConnection)factory.createConnection(brokerParams.getUsername(), brokerParams.getPassword().getPlainText());
            }
            connection.setExceptionListener(new MyExceptionListener());
            connection.addConnectionListener(ConnectionManager.getInstance());
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue queue = session.createQueue(brokerParams.getQueueName());
            messageConsumer = session.createConsumer(queue);
            messageConsumer.setMessageListener(new AmqpMessageListener(brokerParams, triggers));

            connection.start();

            LOGGER.info("Created listener for broker \"" + brokerParams.toString() + "\" containing " + triggers.size() +
                    (triggers.size() == 1 ? " trigger" : " triggers") + " " + triggers.toString());
        } catch (JMSException e) {
            LOGGER.severe(e.getMessage());
            return false;
        }
        return true;
    }

    public void shutdown() {
        if (messageConsumer != null) {
            try {
                messageConsumer.close();
            } catch (JMSException e) {
                LOGGER.warning("Cannot close message consumer for broker " + brokerParams.toString() + ". " + e.getMessage());
            } finally {
                messageConsumer = null;
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                LOGGER.warning("Cannot close session. " + e.getMessage());
            } finally {
                session = null;
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.warning("Cannot close connection." + e.getMessage());
            } finally {
                connection = null;
            }
        }

    }

    protected AmqpMessageListener getMessageListener() {
        AmqpMessageListener l = null;
        if (messageConsumer != null) {
            try {
                l = (AmqpMessageListener)messageConsumer.getMessageListener();
            } catch (JMSException e) {
                LOGGER.warning(e.getMessage());
            }
        }
        return l;
    }

    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
            LOGGER.warning(exception.getMessage());
        }
    }
}

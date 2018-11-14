package org.jenkinsci.plugins.qpidjmsbuildtrigger;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import hudson.util.Secret;

import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;

public class QJMSConnectionManager implements JmsConnectionListener {
    private static final Logger LOGGER = Logger.getLogger(QJMSConnectionManager.class.getName());

    private boolean connectedFlag = false;
	private QJMSConnection connection = null;
	
    private static class InstanceHolder {
        private static final QJMSConnectionManager INSTANCE = new QJMSConnectionManager();
    }
    
    public static QJMSConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }
	
	private QJMSConnectionManager() {}
	
	@Override
	public void onConnectionEstablished(URI remoteURI) {
		LOGGER.info(remoteURI + ": Connection established");
		connectedFlag = true;
	}

	@Override
	public void onConnectionFailure(Throwable error) {
		LOGGER.info("Connection failed: " + error.getMessage());
		connectedFlag = false;
	}

	@Override
	public void onConnectionInterrupted(URI remoteURI) {
		LOGGER.info(remoteURI + ": Connection interrupted");
		connectedFlag = false;
	}

	@Override
	public void onConnectionRestored(URI remoteURI) {
		LOGGER.info(remoteURI + ": Connection restored");
		connectedFlag = true;
	}

	@Override
	public void onInboundMessage(JmsInboundMessageDispatch envelope) {}

	@Override
	public void onSessionClosed(Session session, Throwable cause) {}

	@Override
	public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {}

	@Override
	public void onProducerClosed(MessageProducer producer, Throwable cause) {}

	public boolean isConnected() {
		return connectedFlag;
	}
	
	public void update() {
		GlobalQJMSConfiguration conf = GlobalQJMSConfiguration.get();
        String brokerUri = conf.getBrokerUri();
        String user = conf.getUserName();
        Secret pass = conf.getUserPassword();
        boolean enabledFlag = conf.isEnabledFlag();
        if (!enabledFlag || brokerUri == null) {
            if (connection != null) {
                shutdown();
            }
        }
        if (connection != null &&
                !brokerUri.equals(connection.getBrokerUri()) &&
                !user.equals(connection.getUserName()) &&
                !pass.equals(connection.getUserPassword())) {
            if (connection != null) {
                shutdown();
            }
        }
        if (connection != null && connectedFlag == false) {
            shutdown();
        }

        if (enabledFlag) {
            if (connection == null) {
            	//connection = new QJMSConnection("failover:(" + brokerUri + ")?failover.maxReconnectAttempts=10&amqp.saslLayer=false", user, pass);
            	connection = new QJMSConnection(brokerUri, user, pass);
                try {
                	connection.open();
                } catch (IOException e) {
                	shutdown();
                    LOGGER.log(Level.WARNING, "Cannot open connection.", e.getMessage());
                }
            }
            //connection.updateChannels(GlobalQJMSConfiguration.get().getConsumeItems());
        }
	}
	
    public void shutdown() {
        if (connection != null) {
            try {
            	connection.close();
            } finally {
            	connection = null;
            }
        }
    }
}

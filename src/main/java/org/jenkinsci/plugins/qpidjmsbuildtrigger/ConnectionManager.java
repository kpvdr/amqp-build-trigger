package org.jenkinsci.plugins.qpidjmsbuildtrigger;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import hudson.util.Secret;

import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;

public class ConnectionManager implements JmsConnectionListener {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static final String QUEUE_NAME = "build-trigger-queue";

	private JmsConnection connection = null;
	private Session session = null;
	private Queue queue = null;
	private MessageConsumer messageConsumer = null;
	
    private static class InstanceHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }
    
    public static ConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }
	
	private ConnectionManager() {}
	
	@Override
	public void onConnectionEstablished(URI remoteURI) {
		LOGGER.info(remoteURI + ": Connection established");
	}

	@Override
	public void onConnectionFailure(Throwable error) {
		LOGGER.info("Connection failed: " + error.getMessage());
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
		LOGGER.info("onInboundMessage");
	}

	@Override
	public void onSessionClosed(Session session, Throwable cause) {
		LOGGER.info("onSessionClosed");
	}

	@Override
	public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {
		LOGGER.info("onConsumerClosed");
	}

	@Override
	public void onProducerClosed(MessageProducer producer, Throwable cause) {
		LOGGER.info("onProducerClosed");
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
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
                !brokerUri.equals(connection.getConnectedURI().toString().split("\\?")[0]) &&
                !user.equals(connection.getUsername()) &&
                !pass.getPlainText().equals(connection.getPassword())) {
            if (connection != null) {
                shutdown();
            }
        }
        if (connection != null && connection.isConnected() == false) {
            shutdown();
        }

        if (enabledFlag) {
            if (connection == null) {
            	try {
	            	JmsConnectionFactory factory = null;
	            	if (conf.isEnableDebug()) {
	            		factory = new JmsConnectionFactory(brokerUri + "?amqp.traceFrames=true");
	            	} else {
	            		factory = new JmsConnectionFactory(brokerUri);            		
	            	}
	            	if (user.isEmpty() || pass.getPlainText().isEmpty()) {
	            		connection = (JmsConnection)factory.createConnection();
	            	} else {
	            		connection = (JmsConnection)factory.createConnection(user, pass.getPlainText());
	            	}
	            	connection.setExceptionListener(new MyExceptionListener());
	            	connection.addConnectionListener(ConnectionManager.getInstance());
	            	session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	            	queue = session.createQueue(QUEUE_NAME);
	            	messageConsumer = session.createConsumer(queue);
	            	messageConsumer.setMessageListener(new RemoteBuildListener(QUEUE_NAME));
	            	connection.start();
            	} catch (JMSException e) {
            		shutdown();
            		LOGGER.log(Level.WARNING, "Cannot open connection.", e.getMessage());
            	}
            }
        }
	}
	
    public void shutdown() {
        if (connection != null) {
            try {
            	connection.close();
            } catch (JMSException e) {
            	LOGGER.log(Level.WARNING, "Cannot close connection.", e.getMessage());
            } finally {
            	connection = null;
            }
        }
    }
    
    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
        	LOGGER.warning(exception.getMessage());
        }
    }
}

package org.jenkinsci.plugins.qpidjmsbuildtrigger;

import java.io.IOException;
import java.util.logging.Logger;
import javax.jms.Connection;
//import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import hudson.util.Secret;
import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionFactory;

// TEST CODE
import javax.jms.ExceptionListener;

public class QJMSConnection {
    private static final Logger LOGGER = Logger.getLogger(QJMSConnection.class.getName());
    
    private final String brokerUri;
    private final String userName;
    private final Secret userPassword;
    private final JmsConnectionFactory factory;
    private Connection connection = null;
    private volatile boolean closeRequestedFlag = true;

    public QJMSConnection(String brokerUri, String userName, Secret userPassword) {
    	this.brokerUri = brokerUri;
        this.userName = userName;
        this.userPassword = userPassword;
        LOGGER.info("Broker URL: " + brokerUri);
        factory = new JmsConnectionFactory("failover:(" + brokerUri + ")?failover.maxReconnectAttempts=10&amqp.saslLayer=false");
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public String getBrokerUri() {
        return brokerUri;
    }

    public String getUserName() {
        return userName;
    }

    public Secret getUserPassword() {
        return userPassword;
    }
    
    public void open() throws IOException {
        if (closeRequestedFlag) { // Not yet open
            try {
            	if (userName.isEmpty() || userPassword.getPlainText().isEmpty()) {
            		connection = factory.createConnection();
            	} else {
            		connection = factory.createConnection(userName, userPassword.getPlainText());
            	}
            	connection.setExceptionListener(new MyExceptionListener());
            	((JmsConnection)connection).addConnectionListener(QJMSConnectionManager.getInstance());
            	connection.start();
                closeRequestedFlag = false;
            } catch (JMSException e) {
            	throw new IOException(e);
            }
        } else {
            throw new IOException("Connection is already opened.");
        }    	
    }
    
    public boolean isCloseRequested() {
        return closeRequestedFlag;
    }
    
    public void close() {
        try {
            closeRequestedFlag = true;
            if (connection != null) {
            	connection.close();
            }
        } catch (JMSException e) {
        	LOGGER.warning("Failed to close connection: " + e.getMessage());
        }
    }
    
    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
        	LOGGER.warning(exception.getMessage());
        }
    }
}

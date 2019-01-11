package org.jenkinsci.plugins.amqpbuildtrigger;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import hudson.model.Project;
import hudson.util.Secret;

import jenkins.model.Jenkins;

import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;

public class ConnectionManager implements JmsConnectionListener {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());

	private JmsConnection connection = null;
	private Session session = null;
	
    private static class InstanceHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }
    
    public static ConnectionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }
	
	private ConnectionManager() {}
	
	@Override
	public void onConnectionEstablished(URI remoteURI) {}

	@Override
	public void onConnectionFailure(Throwable error) {}

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
		LOGGER.info("JMS message received: " + envelope.getMessage());
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
	public void onProducerClosed(MessageProducer producer, Throwable cause) {
		LOGGER.info("Producer " + producer.toString() + " closed: " + cause.getMessage());
	}

	public boolean isConnected() {
		return connection != null && connection.isConnected();
	}
	
	public void update() {
		GlobalATConfiguration conf = GlobalATConfiguration.get();
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
	            	
	            	// Keep a map of queues to list of triggers, as a queue may be used by multiple
	            	// triggers.
	            	Map<String, List<RemoteBuildTrigger> > queueNameMap = new HashMap<String, List<RemoteBuildTrigger> >();
	                Jenkins jenkins = Jenkins.getInstance();
	                if (jenkins != null) {
	                	for (Project<?, ?> p : jenkins.getAllItems(Project.class)) {
	                		RemoteBuildTrigger t = p.getTrigger(RemoteBuildTrigger.class);
	                		if (t != null) {
	                			List<String> queueNameList = t.getTriggerQueueList();
	                			for (String queueName: queueNameList) {
	                				if (queueNameMap.containsKey(queueName)) {
	                					queueNameMap.get(queueName).add(t);
	                				} else {
	                					List<RemoteBuildTrigger> l = new ArrayList<RemoteBuildTrigger>();
	                					l.add(t);
	                					queueNameMap.put(queueName, l);
	                				}
	                			}
	                		}
	                	}
	                }
	                
	                // Cycle through all queues in map, create a listener for each
	                for (String queueName: queueNameMap.keySet()) {
	                	createListener(session, queueName, queueNameMap.get(queueName));
	                }
	            	
	            	connection.start();
            	} catch (JMSException e) {
            		shutdown();
            		LOGGER.warning("Cannot open connection: " + e.getMessage());
            	}
            }
        }
	}
	
	public void createListener(Session session, String queueName, List<RemoteBuildTrigger> triggerList) throws JMSException {
		if (session != null) {
			Queue queue = session.createQueue(queueName);
			MessageConsumer messageConsumer = session.createConsumer(queue);
			RemoteBuildListener listener = new RemoteBuildListener(queueName);
			for (RemoteBuildTrigger t: triggerList) {
				listener.addTrigger(t);
			}
			messageConsumer.setMessageListener(listener);
			LOGGER.info("Created listener for queue \"" + queueName + "\" containing " + triggerList.size() +
					(triggerList.size() == 1 ? " trigger" : " triggers"));
		}
	}
	
    public void shutdown() {
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
    
    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
        	LOGGER.warning(exception.getMessage());
        }
    }
}

package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.jms.JmsConnectionFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class AmqpBrokerParams implements Describable<AmqpBrokerParams> {
	
    private String brokerUrl;
    private String username;
    private Secret password;
    private String queueName;
    
    @DataBoundConstructor
    public AmqpBrokerParams(String brokerUrl, String username, Secret password, String queueName) {
    	this.brokerUrl = brokerUrl;
    	this.username = username;
    	this.password = password;
    	this.queueName = queueName;
    	System.out.println("***** AmqpBroker created: " + toString());
    }
    
    public String getBrokerUrl() {
    	return brokerUrl;
    }
    
    public String getUsername() {
    	return username;
    }
    
    public Secret getPassword() {
    	return password;
    }
    
    public String getQueueName() {
    	return queueName;
    }
    
    @DataBoundSetter
    public void setBrokerUrl(String brokerUrl) {
    	this.brokerUrl = brokerUrl;
    }
    
    @DataBoundSetter
    public void setUsername(String username) {
    	this.username = username;
    }
    
    @DataBoundSetter
    public void setPassword(Secret password) {
    	this.password = password;
    }

    public void setUserPassword(String password) {
        this.password = Secret.fromString(password);
    }
    
    @DataBoundSetter
    public void setQueueName(String queueName) {
    	this.queueName = queueName;
    }
    
    public String toString() {
    	return brokerUrl + "/" + queueName;
    }
    
    public boolean isValid() {
    	return brokerUrl != null && !brokerUrl.isEmpty() && queueName != null && !queueName.isEmpty();
    }
    
    @Override
    public Descriptor<AmqpBrokerParams> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(AmqpBrokerUrlDescriptor.class);
    }

    @Extension
    public static class AmqpBrokerUrlDescriptor extends Descriptor<AmqpBrokerParams> {

        @Override
        public String getDisplayName() {
            return "AMQP Broker URL";
        }
    	
        public static ExtensionList<AmqpBrokerUrlDescriptor> all() {
            return Jenkins.getInstance().getExtensionList(AmqpBrokerUrlDescriptor.class);
        }
        
        public FormValidation doTestConnection(@QueryParameter("brokerUrl") String brokerUrl) throws ServletException {
            String uri = StringUtils.strip(StringUtils.stripToNull(brokerUrl), "/");
            // TODO: (GitHub Issue #2) Validate URL
            if (uri != null /*&& urlValidator.isValid(uri)*/) {
                try {
                    ConnectionFactory factory = (ConnectionFactory)new JmsConnectionFactory(uri);
                    Connection connection = factory.createConnection();
                    connection.setExceptionListener(new MyExceptionListener());
                    connection.start();
                    // TODO: Get connection properties
                    connection.close();
                    return FormValidation.ok("ok");
                } catch (javax.jms.JMSException e) {
                    return FormValidation.error(e.toString());
                }
            }
            return FormValidation.error("Invalid Broker URL");
        }
    }

    private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
            System.out.println("Connection ExceptionListener fired, exiting.");
            exception.printStackTrace(System.out);
            System.exit(1);
        }
    }
}

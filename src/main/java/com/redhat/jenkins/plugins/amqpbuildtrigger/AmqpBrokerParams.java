package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang3.StringUtils;
import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class AmqpBrokerParams implements Describable<AmqpBrokerParams> {
    private static final String DISPLAY_NAME = "AMQP server parameters";

    private String url;
    private String user;
    private Secret password;
    private String sourceAddr;

    @DataBoundConstructor
    public AmqpBrokerParams(String url, String username, Secret password, String sourceAddr) {
        this.url = url;
        this.user = username;
        this.password = password;
        this.sourceAddr = sourceAddr;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public Secret getPassword() {
        return password;
    }

    public String getSourceAddr() {
        return sourceAddr;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @DataBoundSetter
    public void setUser(String user) {
        this.user = user;
    }

    @DataBoundSetter
    public void setPassword(Secret password) {
        this.password = password;
    }

    public void setUserPassword(String password) {
        this.password = Secret.fromString(password);
    }

    @DataBoundSetter
    public void setSourceAddr(String sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public String toString() {
        return url + "/" + sourceAddr;
    }

    public boolean isValid() {
        return url != null && !url.isEmpty() && sourceAddr != null && !sourceAddr.isEmpty();
    }

    @Override
    public Descriptor<AmqpBrokerParams> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(AmqpBrokerUrlDescriptor.class);
    }

    @Extension
    public static class AmqpBrokerUrlDescriptor extends Descriptor<AmqpBrokerParams> {

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        public static ExtensionList<AmqpBrokerUrlDescriptor> all() {
            return Jenkins.getInstance().getExtensionList(AmqpBrokerUrlDescriptor.class);
        }

        public FormValidation doTestConnection(@QueryParameter("url") String url,
        		                               @QueryParameter("user") String user,
        		                               @QueryParameter("passowrd") String password,
        		                               @QueryParameter("sourceAddr") String sourceAddr) throws ServletException {
            String uri = StringUtils.strip(StringUtils.stripToNull(url), "/");
            // TODO: (GitHub Issue #2) Validate URL
            if (uri != null /*&& urlValidator.isValid(uri)*/) {
                try {
                    JmsConnectionFactory factory = new JmsConnectionFactory(uri);
                    JmsConnection connection;
                    Secret spw = Secret.fromString(password);
                    if (user.isEmpty() || spw.getPlainText().isEmpty()) {
                        connection = (JmsConnection)factory.createConnection();
                    } else {
                        connection = (JmsConnection)factory.createConnection(user, spw.getPlainText());
                    }
                    connection.setExceptionListener(new MyExceptionListener());
                    connection.start();

                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Queue queue = session.createQueue(sourceAddr);
                    MessageConsumer messageConsumer = session.createConsumer(queue);

                    messageConsumer.close();
                    session.close();
                    connection.close();
                    return FormValidation.ok("OK");
                } catch (javax.jms.JMSException e) {
                    return FormValidation.error(e.toString());
                }
            }
            return FormValidation.error("Invalid server URL");
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

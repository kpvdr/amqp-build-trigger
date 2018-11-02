package org.jenkinsci.plugins.qpidjmsbuildtrigger;

//import java.util.logging.Logger;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.validator.routines.UrlValidator;

import org.apache.qpid.jms.JmsConnectionFactory;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class GlobalQpidJmsConfiguration extends GlobalConfiguration {

    private static final String PLUGIN_NAME = null;
    //private static final Logger LOGGER = Logger.getLogger(GlobalQpidJmsConfiguration.class.getName());
    //private static final String[] AMQP_SCHEMES = { "amqp", "amqps" };
    //private final UrlValidator urlValidator = new UrlValidator(AMQP_SCHEMES, UrlValidator.ALLOW_LOCAL_URLS);
    
	private boolean enableFlag;
    private String brokerUri;
    private String userName;
    private Secret userPassword;
    private boolean enableDebug;

    @DataBoundConstructor
    public GlobalQpidJmsConfiguration(boolean enableFlag, String brokerUri, String userName, Secret userPassword,
    		boolean enableDebug) {
        this.enableFlag = enableFlag;
        this.brokerUri = StringUtils.strip(StringUtils.stripToNull(brokerUri), "/");
        this.userName = userName;
        this.userPassword = userPassword;    	
        this.enableDebug = enableDebug;
    }
    
    public GlobalQpidJmsConfiguration() {
        load();
    }

    @Override
    public String getDisplayName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        req.bindJSON(this, json);
    	if (true) { // if (urlValidator.isValid(serviceUri)) {
    		save();
    		return true;
    	}
    	return false;
    }
    
    public boolean isEnableDebug() {
        return enableDebug;
    }

    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }
    
    public boolean isEnableFlag() {
        return enableFlag;
    }
    
    public void setEnableFlag(boolean enableFlag) {
        this.enableFlag = enableFlag;
    }
    
    public String getBrokerUri() {
        return brokerUri;
    }
    
    public void setBrokerUri(final String brokerUri) {
        this.brokerUri = StringUtils.strip(StringUtils.stripToNull(brokerUri), "/");
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public Secret getUserPassword() {
        return userPassword;
    }
    
    public void setUserPassword(Secret userPassword) {
        this.userPassword = userPassword;
    }
    
    public void setUserPassword(String userPassword) {
        this.userPassword = Secret.fromString(userPassword);
    }
    
    public FormValidation doCheckBrokerUri(@QueryParameter String value) {
        String val = StringUtils.stripToNull(value);
        if (val == null) {
            return FormValidation.ok();
        }

        // TODO: Validate URL
        //if (urlValidator.isValid(val)) {
        //    return FormValidation.ok();
        //} else {
        //    return FormValidation.error(Messages.InvalidURI());
        //}
        return FormValidation.ok();
    }
    
    public FormValidation doTestConnection(@QueryParameter("brokerUri") String brokerUri,
            @QueryParameter("userName") String userName,
            @QueryParameter("userPassword") Secret userPassword) throws ServletException {
        String uri = StringUtils.strip(StringUtils.stripToNull(brokerUri), "/");
        if (uri != null /*&& urlValidator.isValid(uri)*/) {
            try {
            	ConnectionFactory factory = (ConnectionFactory)new JmsConnectionFactory(uri);
            	//ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            	
            	Connection connection = factory.createConnection(userName, userPassword.getPlainText());
            	connection.setExceptionListener(new MyExceptionListener());
            	connection.start();
            	// Get connection properties
            	connection.close();
                return FormValidation.ok("ok");
            } catch (javax.jms.JMSException e) {
            	return FormValidation.error(e.toString());
            }
        }
        return FormValidation.error("Invalid Broker URL");
    }
    
    public static GlobalQpidJmsConfiguration get() {
        return GlobalConfiguration.all().get(GlobalQpidJmsConfiguration.class);
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


package org.jenkinsci.plugins.qpidjmsbuildtrigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hudson.Extension;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.ParameterValue;
import hudson.model.Project;
import hudson.model.StringParameterValue;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class RemoteBuildTrigger<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob> extends Trigger<T> {
    public static final String PLUGIN_APPID = "remote-build";
    
    private static final String KEY_PARAM_NAME = "name";
    private static final String KEY_PARAM_VALUE = "value";
    private static final String PLUGIN_NAME = "Qpid JMS Build Trigger";
    
    private String remoteBuildToken;
    
    @DataBoundConstructor
    public RemoteBuildTrigger(String remoteBuildToken) {
        super();
        this.remoteBuildToken = StringUtils.stripToNull(remoteBuildToken);
    }
    
    @Override
    public void start(T project, boolean newInstance) {
        RemoteBuildListener listener = RemoteBuildListener.all().get(RemoteBuildListener.class);
        if (listener != null) {
            listener.addTrigger(this);
            removeDuplicatedTrigger(listener.getTriggers());
        }
    	super.start(project, newInstance);
    }

    @Override
    public void stop() {
        RemoteBuildListener listener = RemoteBuildListener.all().get(RemoteBuildListener.class);
        if (listener != null) {
            listener.removeTrigger(this);
        }
    	super.stop();
    }
    
    public void removeDuplicatedTrigger(Set<RemoteBuildTrigger> triggers){
        Map<String,RemoteBuildTrigger>  tempHashMap= new HashMap<String,RemoteBuildTrigger>(); 
        for(RemoteBuildTrigger trigger:triggers){
            tempHashMap.put(trigger.getProjectName(), trigger);
        }    
        triggers.clear();
        triggers.addAll(tempHashMap.values());
    }
    
    public String getRemoteBuildToken() {
        return remoteBuildToken;
    }

    public void setRemoteBuildToken(String remoteBuildToken) {
        this.remoteBuildToken = remoteBuildToken;
    }
    
    public String getProjectName() {
        if(job != null){
            return job.getFullName();
        }
        return "";
    }
    
    public void scheduleBuild(String queueName, JSONArray jsonArray) {
        if (job != null) {
          if (jsonArray != null) {
              List<ParameterValue> parameters = getUpdatedParameters(jsonArray, getDefinitionParameters(job));
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RemoteBuildCause(queueName)), new ParametersAction(parameters));
          } else {
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RemoteBuildCause(queueName)));
          }
        }
    }
    
    private List<ParameterValue> getUpdatedParameters(JSONArray jsonParameters, List<ParameterValue> definedParameters) {
        List<ParameterValue> newParams = new ArrayList<ParameterValue>();
        for (ParameterValue defParam : definedParameters) {

            for (int i = 0; i < jsonParameters.size(); i++) {
                JSONObject jsonParam = jsonParameters.getJSONObject(i);

                if (defParam.getName().toUpperCase().equals(jsonParam.getString(KEY_PARAM_NAME).toUpperCase())) {
                    newParams.add(new StringParameterValue(defParam.getName(), jsonParam.getString(KEY_PARAM_VALUE)));
                }
            }
        }
        return newParams;
    }
    
    private List<ParameterValue> getDefinitionParameters(Job<?, ?> project) {
        List<ParameterValue> parameters = new ArrayList<ParameterValue>();
        ParametersDefinitionProperty properties = project
                .getProperty(ParametersDefinitionProperty.class);

        if (properties != null) {
            for (ParameterDefinition paramDef : properties.getParameterDefinitions()) {
                ParameterValue param = paramDef.getDefaultParameterValue();
                if (param != null) {
                    parameters.add(param);
                }
            }
        }

        return parameters;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Extension @Symbol("qjmsRemoteBuild")
    public static class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }

        @Extension
        public static class ItemListenerImpl extends ItemListener {

            @Override
            public void onLoaded() {
                RemoteBuildListener listener = RemoteBuildListener.all().get(RemoteBuildListener.class);
                Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    for (Project<?, ?> p : jenkins.getAllItems(Project.class)) {
                        RemoteBuildTrigger t = p.getTrigger(RemoteBuildTrigger.class);
                        if (t != null) {
                            listener.addTrigger(t);
                        }
                    }
                }
            }
        }
    }
}

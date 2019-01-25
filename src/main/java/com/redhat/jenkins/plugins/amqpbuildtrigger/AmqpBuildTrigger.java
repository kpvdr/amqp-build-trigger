package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class AmqpBuildTrigger<T extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob> extends Trigger<T> {
    private static final String KEY_PARAM_NAME = "name";
    private static final String KEY_PARAM_VALUE = "value";
    private static final String PLUGIN_NAME = "AMQP Build Trigger";
    private List<AmqpBrokerParams> amqpBrokerParamsList = new ArrayList<AmqpBrokerParams>();

    @DataBoundConstructor
    public AmqpBuildTrigger(List<AmqpBrokerParams> amqpBrokerParamsList) {
        super();
        this.amqpBrokerParamsList = amqpBrokerParamsList;
    }

    public List<AmqpBrokerParams> getAmqpBrokerParamsList() {
        return amqpBrokerParamsList;
    }

    @DataBoundSetter
    public void setAmqpBrokerParamsList(List<AmqpBrokerParams> amqpBrokerParamsList) {
        this.amqpBrokerParamsList = amqpBrokerParamsList;
    }

    @Override
    public String toString() {
        return getProjectName();
    }

    public String getProjectName() {
        if(job != null){
            return job.getFullName();
        }
        return "";
    }

    public void scheduleBuild(String messageSource, JSONArray jsonArray) {
        if (job != null) {
          if (jsonArray != null) {
              List<ParameterValue> parameters = getUpdatedParameters(jsonArray, getDefinitionParameters(job));
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RemoteBuildCause(messageSource)), new ParametersAction(parameters));
          } else {
              ParameterizedJobMixIn.scheduleBuild2(job, 0, new CauseAction(new RemoteBuildCause(messageSource)));
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
        ParametersDefinitionProperty properties = project.getProperty(ParametersDefinitionProperty.class);
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
    public AmqpBuildTriggerDescriptor getDescriptor() {
        return (AmqpBuildTriggerDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class AmqpBuildTriggerDescriptor extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }
    }
}

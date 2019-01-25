package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.model.Cause;

public class RemoteBuildCause extends Cause {

    private final String messageSource;

    public RemoteBuildCause(String messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String getShortDescription() {
        return "Triggered by remote build message from AMQP source: " + messageSource;
    }

}

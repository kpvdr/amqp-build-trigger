package org.jenkinsci.plugins.amqpbuildtrigger;

import hudson.model.Cause;

import org.kohsuke.stapler.export.Exported;

public class RemoteBuildCause extends Cause {

    private final String queueName;

    public RemoteBuildCause(String queueName) {
        this.queueName = queueName;
    }

    @Override
    @Exported(visibility = 3)
    public String getShortDescription() {
        return "Triggered by remote build message from AMQP queue: " + queueName;
    }

}

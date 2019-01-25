# AMQP Build Trigger for Jenkins
This Jenkins plugin will trigger builds when AMQP messages are received from brokers. Each job may specify one or more brokers and queues from which to recieve messages. If any AMQP message is received from any of the configured queues, then a build is triggered.

## Building and Installing
After cloning from Github, use maven to build the package with the command `mvn package`. The compiled package will be located at `target\amqp-build-trigger.hpi`.

To install, make the hpi file available on the Jenkins machine and install using the Jenkins package manager. Select:

**Manage Jenkins** -> **Manage Plugins**

then select the **Advanced** tab. Under the **Upload Plugin** section, select the hpi file.

After a restart, the plugin should be active in Jenkins.

## Basic Configuration
Within each job, scroll down to the **AMQP Build Trigger** section. Each job may specify multiple brokers and/or queues on which to listen for trigger messages. Initially, the list will be empty. Select the **AMQP Build Trigger** checkbox to enable the trigger. Click the **Add** button to add a broker/queue, then select **AMQP Broker URL** from the drop-down list. This will create a new empty broker block. To complete the broker block:

* Enter the **Broker URL** in the format `amqp://ip-addr:port`, eg `amqp://localhost:5672` or `amqp://10.0.0.5:5672`
* If necessary, enter a **Username** and **Password** for logging onto the broker. If supplied, the connection will use SASL `PLAIN` authentication, otherwise if left blank, `ANONYMOUS`. Make sure the broker is configured for this type of access and that the user is known to the broker.
* Enter a **Queue Name** from which to receive messages
* A **Test Connection** button if clicked will establish a temporary connection to the broker and report `ok` if it worked, otherwise an error message will be displayed.

To add additional brokers, click the **Add** button. To remove a broker, click the red **X** button at the top of each block.

Finally, click the **Save** button at the bottom of the form to save the settings. Once these settings are saved, a new connection to each broker will be established and a listener will wait for messages.

**NOTE:** It is possible to perform a simple test of the trigger by using the maven command `mvn hpi:run` from the project top level directory. This will start a simplified version of Jenkins on a Jetty server instance, and the trigger plugin will be installed and running. Only Freestyle projects are available, but it makes for a quick and easy way of testing the trigger. The web interface will be available on `localhost:8080/jenkins`, and all jenkins logs, project files, etc. will be located in the project top-level directory under `work`.

**NOTE:** It is possible to quickly send trigger messages using `qpid-send` as follows:
```
qpid-send -a <queue-name> -m1
```
where `<queue-name>` is replaced by the queue name configured for your project.

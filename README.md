# Qpid JMS Build Trigger for Jenkins
This Jenkins plugin adds a build trigger for Jenkins. It will trigger builds based on JMS messages received from a broker on a queue named `qjbt.trigger-queue`. Any JMS message containing the following properties will trigger a build:

* `msg_type` = `QJMS_build_trigger`
* `jenkins_project_action` = `build`
* `jenkins_project_token` matches the token set for the individual project(s) to be triggered.

## Building and Installing
After cloning from Github, use maven to build the package with the command `mvn package`. The compiled package will be located at `target\qpid-jms-build-trigger.hpi`.

To install, make the hpi file available on the Jenkins machine and install using the Jenkins package manager. Select:

**Manage Jenkins** -> **Manage Plugins**

then select the **Advanced** tab. Under the **Upload Plugin** section, select the hpi file.

After a restart, the plugin should be active in Jenkins.

## Basic Configuration
There are two parts to configuring the trigger:

### Global Configuration
This sets up the address of the AMQP broker from which trigger messages will be received. It will be necessary to set up the address and the name of a queue from which messages will be consumed as they are delivered.

The global configuration can be found at **Manage Jenkins** -> **Configure System**

Scroll down to the **Qpid JMS Build Trigger** section:
* Select **Enable** to allow the trigger to be active. No connection will be made until this is checked.
* Enter the **Broker URI** in the format `amqp://ip-addr:port`, eg `amqp://localhost:5672` or `amqp://10.0.0.5:5672`
* If necessary, enter a **Username** and **Password** for logging onto the broker. If supplied, the connection will use SASL `PLAIN` authentication, otherwise if left blank, `ANONYMOUS`. Make sure the broker is configured for this type of access and that the user is known to the broker.
* A **Test Connection** button will establish a temporary connection to the broker and report `ok` if it worked, otherwise an error message will be displayed.

When these settings are saved, a new connection to the broker will be established and a listener will wait for messages.

### Per-Project configuration
For each project to be triggered, it is necessary to enable the trigger and set a build token. This is a string which is checked against the incoming message, and if a match is found, will trigger the build.

The per-project configuration can be found at **Project Name** -> **Configure**

Scroll down to the **Build Triggers** section.
* Select **Qpid JMS Build Trigger**
* Enter a token which when matched on an incoming message will trigger the build. This is a string.

**NOTE:** The same token may be used by several projects. When an incoming message matches the token, then all the projects using that token will be triggered together. The order of triggering is not guaranteed, however. If ordering is important, then sparate trigger tokens will be needed and each triggered using a separate message.

**NOTE:** It is possible to perform a simple test of the trigger by using the maven command `mvn hpi:run` from the project top level directory. This will start a simplified version of Jenkins on a Jetty server instance, and the trigger plugin will be installed and running. Only Freestyle projects are available, but it makes for a quick and easy way of testing the trigger. The web interface will be available on `localhost:8080/jenkins`, and all jenkins logs, project files, etc. will be located in the project top-level directory under `work`.

**NOTE:** It is possible to quickly send trigger messages using `qpid-send` as follows:
```
qpid-send -a qjbt.trigger-queue -m1 -P msg_type="QJMS_build_trigger" -P jenkins_project_action="build" -P jenkins_project_token="<your test token>"
```
where `<your test token>` is replaced by the token string set up in the per-project section above.

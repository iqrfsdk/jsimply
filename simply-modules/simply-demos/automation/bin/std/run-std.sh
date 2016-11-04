#!/bin/bash

/usr/bin/sudo /usr/bin/java -Djava.library.path=natives/armv7l \
        -Dlogback.configurationFile=config/logback/logback.xml \
        -cp automation-0.0.1.jar: \
        com.microrisc.simply.demos.automation.AppStd >> automation-std.log 2>&1

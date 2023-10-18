# TeslaRIS-backend

## Running the application

Running the application is very easy, first, position yourself into the project's root directory. After that, simply
run: ```mvn spring-boot:run``` which will run your application.

Alternatively, if you are using Intellij IDE, it is as easy as simply running ```TeslarisStarter.java``` file.

## Installation

In addition to running Spring Boot applications by using Maven, it is also possible to make fully executable
applications for Unix systems. A fully executable jar can be executed like any other executable binary or it can be
registered with init.d or systemd.

You can build the maven project using command ```mvn package``` or ```mvn install```. It will create the .jar file
inside target folder. You can then run your application by typing ```./my-application.jar``` (where my-application is
the multilingualContent of your artifact). The directory containing the jar is used as your application’s working directory.

Alternatively, Spring Boot application can be easily started as Unix/Linux services by using either ```init.d```
or ```systemd```. For detailed tutorial on this topic, please visit the official
documentation [page](https://docs.spring.io/spring-boot/docs/2.0.6.RELEASE/reference/html/deployment-install.html#deployment-service)
.


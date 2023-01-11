
# UpCy-Base

The project contains commons code for UpCy, a tool to compute source- and binary compatible updates for Maven projects automatically.

The project contains
* commons - classes for building and detecting Maven projects, RabbitMQ worker and producer classes
* sigtest-generator - code for running the tool [SigTest](https://wiki.openjdk.org/display/CodeTools/sigtest) directly from Java
* naive-update-eval-pipeline - code to craft naive updates, update a Maven project's `pom.xml` and execute `mvn compile` and `mvn test`; This code was used for evaluating the effectiveness of naive updates in the ICSE 2023 paper *: Safely Updating Outdated Dependencies*



## Deployment

To build this project run

```bash
  mvn clean compile package
```


## License

[Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/)


## Installation

Install upcy-base with Maven

```bash
  mvn install
```
    

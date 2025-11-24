This project takes in a Maven project and generates a `module-info.java` file based on the project's dependencies and structure.

The primary goal right now is to protect against [maven class hijack attacks](https://arxiv.org/abs/2407.18760).
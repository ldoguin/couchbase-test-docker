# couchbase-test-docker


JUnit test example with Couchbase in a Docker container and [TestContainers](http://testcontainers.viewdocs.io/testcontainers-java/).

TestContainers allow you to run a container before tests, either for all the class or each methods. This example runs a container with Couchbase Server preconfigured and loaded with the beer-sample. Than test the beer-sample. To run this test successfuly you need to build the DockerFile at the root with the tag 'mycouchbase:latest'.

## Building the container

    docker build -t mycouchbase:latest

## Running the Rest

    gradle build

## How it works

The [GenericContainer](http://testcontainers.viewdocs.io/testcontainers-java/usage/generic_containers/) is used with a custom wait strategy. It wait for the HTTP endpoint `/pools/default` to be accessible and for the node to have the `healthy` status.
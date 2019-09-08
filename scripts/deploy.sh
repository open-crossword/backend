#!/bin/bash

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
sbt ++$TRAVIS_SCALA_VERSION docker:publish

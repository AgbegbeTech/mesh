#!/bin/bash

BASEDIR=$(dirname "$0")
cd $BASEDIR/..
echo "Using includes: $1"
tests=$(paste -sd "," $1 | sed 's/\.java//g' | sed 's/\//./g')

if [ -z "$tests" ] ; then
  echo "Did not collect valid test set. Set was empty."
  exit 10
fi
echo "Running tests: $tests"

time mvn -X -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -e -pl '!demo,!doc,!performance-tests' test -Dtest=$tests -DfailIfNoTests=false

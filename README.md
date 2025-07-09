[![build status](https://img.shields.io/github/actions/workflow/status/mizdebsk/dola-gleaner/ci.yml?branch=master)](https://github.com/mizdebsk/dola-gleaner/actions/workflows/ci.yml?query=branch%3Amaster)
[![License](https://img.shields.io/github/license/mizdebsk/dola-gleaner.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central version](https://img.shields.io/maven-central/v/io.kojan/dola-gleaner.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.kojan/dola-gleaner)
![Fedora Rawhide version](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fmdapi.fedoraproject.org%2Frawhide%2Fpkg%2Fdola-gleaner&query=%24.version&label=Fedora%20Rawhide)
[![Javadoc](https://javadoc.io/badge2/io.kojan/dola-gleaner/javadoc.svg)](https://javadoc.io/doc/io.kojan/dola-gleaner)

Dola Gleaner
============

Maven 4 extension for extracting build dependencies

Dola Gleaner is an extension for Apache Maven 4 that extracts build
dependencies without actually executing the build.  Instead of running
plugins (MOJOs), it analyzes the project and prints the dependencies
that would be required to complete the specified build.

This tool is especially useful for tools and environments that need to
understand build requirements without performing the build itself.

Usage: Run Maven as usual, but activate the extension by including
`-Dmaven.ext.class.path=/path/to/dola-gleaner.jar`.  Specify your
intended build goals (e.g., `clean package`), activate any necessary
profiles, and define properties just as you would in a normal Maven
invocation.

This is free software. You can redistribute and/or modify it under the
terms of Apache License Version 2.0.

This software was written by Mikolaj Izdebski.

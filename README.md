
# Source code repository

https://github.com/itesla/CGMES.git

# Testing

The project contains a module `tools` that can be used to test import/export of CGMES data and conversion to IIDM.

## Test tool

There is a convenience bash script, `runcgmes`, that calls the test tool using the following command:

	mvn test-compile exec:java --projects tools -Dexec.mainClass=com.powsybl.cgmes.tools.Main -Dexec.args="$*" -Ditools.config.dir=~/.itools -Dexec.classpathScope=test

The test tool accepts the following parameters:

`--path`, `--basename`, `--compression` are used to provide the network/case to import use. The input data may be a set of XML files in a folder, a single archive file (ZIP) ...

`--tripleStore` to indicate the implementation for the CGMES data store. The available options are `rdf4j`, `jena` and `blazegraph`.

`--action` the testing action to perform:<br>

`numObjectsByType` outputs a table with the number of objects of each class that can be compared with CGMES conformity documentation.

## Examples

Some example calls:

	runcgmes --path data/cim14/ieee14 --tripleStore rdf4j --action numObjectsByType
	
	runcgmes --path data/conformity/cas-1.1.3-data-4.0.3/MicroGrid/BaseCase/CGMES_v2.4.15_MicroGridTestConfiguration_BC_BE_v2 --tripleStore rdf4j --action numObjectsByType
	
	runcgmes --path data/conformity/cas-2.0/MicroGrid/BaseCase_BC --basename CGMES_v2.4.15_MicroGridTestConfiguration_BC_NL_v2 --compression zip --tripleStore rdf4j --action numObjectsByType
	
# Build

This project depends on `powsybl-core`. It can be obtained and built using:

	git clone https://github.com/powsybl/powsybl-core powsybl/powsybl-core
	cd powsybl/powsybl-core && mvn install -DskipTests=true

Currently master branch of this projects depends on `cgmes_validation` branch of `powsybl-core`.

# Logging

Jena and Blazegraph both depend directly on `log4j`. 

When we try to run code from these libraries together with powsybl code that uses `slf4j` way of logging with a `slf4-simple` implementation, it complaints about multiple bindings:

	SLF4J: Class path contains multiple SLF4J bindings
	SLF4J: Found binding in [jar:file:.m2/repository/org/slf4j/slf4j-log4j12/1.7.12/slf4j-log4j12-1.7.12.jar!/org/slf4j/impl/StaticLoggerBinder.class]
	SLF4J: Found binding in [jar:file:.m2/repository/org/slf4j/slf4j-simple/1.7.22/slf4j-simple-1.7.22.jar!/org/slf4j/impl/StaticLoggerBinder.class]

To correctly use `slf4j` we must (see https://www.slf4j.org/legacy.html):

 - Exclude `log4j` from Jena and Blazegraph dependencies:

		<exclusions>
			<exclusion>
				<groupId>org.slf4j</groupId>
				<artifactId>slf4j-log4j12</artifactId>
			</exclusion>
			<exclusion>
				<groupId>log4j</groupId>
				<artifactId>log4j</artifactId>
			</exclusion>
		</exclusions>
			
 - Add the dependency `log4j-over-slf4j` to bridge the legacy `log4j` API to the new `slf4j`:
 
 		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>log4j-over-slf4j</artifactId>
			<version>1.7.25</version>
		</dependency>
 

# Set-up development environment for Eclipse (Photon)

Steps for importing powsybl-core and CGMES maven projects are the same that in Eclipse Oxygen.

There is no need to create Eclipse .project files running `mvn eclipse:eclipse`. After maven projects import all natures are automatically recognized and set (Java, Groovy).

To fix the warnings about potential mismatch Groovy compiler version, it is enough to uncheck this option in Groovy Compiler settings in Eclipse.

To create a Java formatter from Checkstyle, first create and configure a new Global Checkstyle Configuration that uses the file `checkstyle.xml` (use the option external configuration file) and set it as default. Then, select any project in the workspace, right-click and select Checkstyle - Create Formatter profile. Then configure Java Code Style - Formatter as workspace settings (or by use it as project-specific settings).
 
# Set-up development environment for Eclipse (Oxygen)

## From git repository

First import project `powsybl-core` from git to local workspace.

- https://github.com/powsybl/powsybl-core

## Import maven projects

Then import existing Maven projects.

Projects may be assigned to separate working sets.

## Create Eclipse project files

Create Eclipse `.project` files from maven `pom.xml` files using `mvn eclipse:eclipse`. This will add Java nature to the projects

First run `mvn eclipse:eclipse` from the root folder of the `powsybl-core` project

? It is possible that some individual projects may require `mvn eclipse:eclipse` to be run directly from its specific folder

## Add support for Groovy
	
Some projects contain Groovy code, so we must install the corresponding plugin

Oxygen version is available at [org.codehaus.groovy.eclipse.site](http://dist.springsource.org/snapshot/GRECLIPSE/e4.7)

Install all components: Compilers, Eclipse plugin, Maven support and the uncategorized JDT patch

Fix the potential mismatch compiler version problem:

- Project uses groovy compiler version 2.4, latest Groovy plugin uses 2.5
- Go to project properties from a project containing groovy (action-dsl), 
- Then goto workspace settings and switch to compiler version 2.4.x

After installing the plugin, Eclipse may continue ignoring groovy code (not compiling it, a red exclamation mark will be put in every file). We have to convert all projects with groovy code to Groovy projects (right-click on project, configure, convert to groovy project).

After converting to Groovy projects it is possible that we have to fix the source configuration in Java build path:

- Groovy classes compile (after previous step) but are not found from Eclipse
- Eclipse says all types from groovy files are not found
- For each project with groovy files:
    - Go to properties, java build path, source tab
    - For every source code folder related to groovy:
        - Change included list of files from `**/*.java` to `**/*.groovy`

# JMonicelli
**Monicelli 2.0 to Java bytecode compiler**

This project is a port of the Monicelli esoteric programming language to the
JVM, it builds a class file from a Monicelli source file.

For more information about Monicelli, check out the reference implementation's
GitHub repo at https://github.com/esseks/monicelli

## How to build

This is a standard Gradle project, you should use the Gradle wrapper already
included to build it from source. Just issue the command:

    ./gradlew shadowJar

A fat JAR will be generated into the `build/libs` directory.

## Usage

The program expects two command line arguments: source file path and output
class name. Packages are not supported at the moment, the .class file will be
written inside of the current directory at invocation time.

For example:

    java -jar JMonicelli.jar program.mc Program

Will generate a file named `Program.class` in the current directory. The class
name doesn't have to match the source file name.

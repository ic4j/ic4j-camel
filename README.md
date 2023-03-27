## Apache Camel Internet Computer Component

Apache Camel is an Open Source integration framework that empowers you to quickly and easily integrate various systems consuming or producing data.

<a href="https://camel.apache.org/">
https://camel.apache.org/
</a>

The IC4J Camel component allows native execution of Internet Computer smart contracts from Apache Camel.


# Downloads / Accessing Binaries

To add Java IC4J Apache Camel Internet Computer Component library to your Java project use Maven or Gradle import from Maven Central.

<a href="https://search.maven.org/artifact/ic4j/ic4j-camel/0.6.19/jar">
https://search.maven.org/artifact/ic4j/ic4j-camel/0.6.19/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-camel</artifactId>
  <version>0.6.19</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-camel:0.6.19'
```

To install IC4J Camel component to Apache Karavan Visual Studio Code plug-in add 

```
ic
```

to .vscode/extensions/camel-karavan.karavan-3.20.0/components/components.properties file

and content of [src/ic.json](./src/ic.json) file to .vscode/extensions/camel-karavan.karavan-3.20.0/components/components.json file.

To enable the loading of IC4J libraries, you should move the [src/application.properties](./src/application.properties) file to the root of your project. 


# Build

You need JDK 11+ to build IC4J Apache Camel Internet Computer Component.

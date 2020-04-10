# Hacks

To download the generator JAR:
```bash
wget -O openapi-generator-cli.jar https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/4.3.0/openapi-generator-cli-4.3.0.jar
```

To use a snapshot:
```bash
wget -O openapi-generator-cli.jar https://oss.sonatype.org/content/repositories/snapshots/org/openapitools/openapi-generator-cli/4.3.1-SNAPSHOT/openapi-generator-cli-4.3.1-20200410.073949-59.jar
```

To download extra OpenAPI spec from the `kubernetes-client/gen` repo:
```bash
wget https://raw.githubusercontent.com/kubernetes-client/gen/master/openapi/custom_objects_spec.json
```

To generate the API:
```bash
./generate.sh
```

To interact with the generator:
```bash
java -jar openapi-generator-cli.jar help
java -jar openapi-generator-cli.jar config-help -g scala-sttp
```

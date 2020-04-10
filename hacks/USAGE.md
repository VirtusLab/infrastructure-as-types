# Hacks

To download the generator JAR:
```bash
wget https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/4.2.3/openapi-generator-cli-4.2.3.jar -O openapi-generator-cli.jar
```

To use a snapshot:
```bash
wget https://oss.sonatype.org/content/repositories/snapshots/org/openapitools/openapi-generator-cli/4.3.0-SNAPSHOT/openapi-generator-cli-4.3.0-20200224.135343-126.jar -O openapi-generator-cli.jar
```

To download extra OpenAPI spec from the `kubernetes-client/gen` repo:
```bash
wget https://raw.githubusercontent.com/kubernetes-client/gen/master/openapi/custom_objects_spec.json
```

To generate the API:
```bash
./generate.sh
```


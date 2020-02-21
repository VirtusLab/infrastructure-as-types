# Hacks

```bash
wget https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/4.2.3/openapi-generator-cli-4.2.3.jar -O openapi-generator-cli.jar
```

```bash
kubectl get --raw /openapi/v2 > swagger.json
```

```bash
java -jar openapi-generator-cli.jar generate \
    --config scala-akka.yaml \
    --generator-name scala-akka \
    --input-spec swagger.json \
    --skip-validate-spec \
    --git-user-id VirtusLab \
    --git-repo-id kubernetes-client-scala \
    --artifact-id kubernetes-client-scala \
    --group-id com.virtuslab \
    --type-mappings int-or-string=IntOrString,quantity=Quantity,patch=V1Patch \
    --import-mappings IntOrString=io.kubernetes.client.custom.IntOrString,Quantity=io.kubernetes.client.custom.Quantity,V1Patch=io.kubernetes.client.custom.V1Patch \
    --output ../kubernetes | tee generate.log
```


#!/usr/bin/env sh

set -o errexit
set -o nounset

# Download the swagger.json and pre-process
python preprocess_spec.py java v1.15.10 swagger.json kubernetes kubernetes

# Cleanup already generated stuff
rm -rf ../kubernetes/src/main/scala/com/virtuslab/kubernetes/client/openapi

# Run the generator
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

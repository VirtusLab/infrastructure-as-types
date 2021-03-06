#!/usr/bin/env sh

set -o errexit
set -o nounset

# Download the swagger.json and pre-process
python preprocess_spec.py scala v1.15.10 swagger.json kubernetes kubernetes | tee preprocess.log

# Cleanup already generated stuff
rm -rf ../kubernetes/src/main/scala/com/virtuslab/kubernetes/client/openapi
rm -rf ../kubernetes/src/main/scala/org/openapitools/client

# Run the generator
java -jar openapi-generator-cli.jar generate \
    --config scala-sttp.yaml \
    --generator-name scala-sttp \
    --input-spec swagger.json \
    --skip-validate-spec \
    --git-user-id VirtusLab \
    --git-repo-id kubernetes-client-scala \
    --artifact-id kubernetes-client-scala \
    --group-id com.virtuslab \
    --output ../kubernetes 2>&1 | tee generate.log

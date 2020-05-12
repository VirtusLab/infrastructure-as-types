# Infrastructure as Types
Infrastructure as Types project provides tools for micro-system infrastructure development.

## The Problem

Main problems we want to solve:
- address the definition scalability issues of hundreds of interdependent YAMLs
- allow for refactoring safer than untyped, text heavy YAML
- effortless security rules, based on strongly typed code
- allow to tackle the complexity that comes with service mesh's and ingress controllers

## Vision

Use your language to express it all.

We want every JVM developer (starting with Scala) to **feel at home in a cloud** native 
environment (starting with Kubernetes). 

We believe that a **developer friendly** infrastructure abstractions are increasingly necessary in the age of the cloud.
Bootstrapping and maintaining a distributed system required by the business is often challenging and costly.
Real-world use cases come with complexity that YAMLs ans JSONs can't reliably express.

The plan is simple, provide a set of tools for **micro-system infrastructure development**:
- High-level Scala DSL describing a **graph of your (micro)services and their connectivity**
- Low-level Scala DSL for Kubernetes manifest resources (incl. CRDs)
- Scala Kubernetes Controllers/Operators library
- Scala Kubernetes API Client library
- YAML/JSON generators

Design considerations:
- easy to read and maintain code for the users
- 80% of use cases should be easy and straightforward to express (with high-to-mid-level abstractions)
- provide many extension points for the remaining 20% of use cases

Additional opportunities and future development:
- Unit and integrations test frameworks (with dry-run capabilities)
- Custom deployment and release strategies library (blue-green, canary, A/B, etc.)
- Custom control plane for a service mesh (e.g. Envoy based)
- Serverless support (e.g. Knative)
- Native container image support (e.g. GraalVM)
- Self-deployment pattern support
- Take advantage of Scala 3 unique features

## Status

The project is **pre-alpha** and under heavy development, any and all APIs can change without notice.

| API/Technology | Status |
| -------------- | ------ |
| kubernetes     | alpha  |
| istio          | -      |
| linkerd        | -      |
| envoy          | -      |
| knative        | -      |

| Kubernetes Client | Status       |
| ----------------- | ------------ |
| skuber            | alpha        |
| OpenAPI / sttp    | experimental |

| Use case                   | Status       |
| -------------------------- | ------------ |
| JSON/YAML generation       | alpha        |
| Kubernetes Deployment      | experimental |
| JSON/YAML diff             | -            |
| Kubernetes Operator        | -            |
| Unit and integration tests | basic        |

## Development

Test cluster config is available with:
```bash
gcloud container clusters get-credentials standard-cluster-1 --zone us-central1-a --project infrastructure-as-types
```

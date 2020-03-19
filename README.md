# Infrastructure as Types
Infrastructure as Types project provides tools for custom microsystem control planes development.

## Vision

One API to rule them all, custom DSL to express it all.

We want every JVM developer (starting with Scala) to **feel at home in a cloud** native environemnt. 

We believe that a **developer friendly** abstractions that allow to define any distributed system 
required by the business are increasingly necessary in the age of the cloud.
Real-world use cases come with complexity that YAMLs can't reliably express.

The plan is simple, provide a set of tools for **custom microsystem control plane development**:
- High-level Scala DSL describing a **graph of your microservices and service meshes**
- Low-level Scala DSL for Kubernetes JSON/YAML resources (incl. CRDs)
- Scala Kubernetes Controllers/Operators library
- Scala Kubernetes Client library

Additional opportunities:
- Unit and integrations test framewroks (with dry-run capability)
- Custom deployment and release strategies library
- Knative serverless support
- GraalVM container support
- self-deployment pattern support

## Development

Test cluster config is available with:
```bash
gcloud container clusters get-credentials standard-cluster-1 --zone us-central1-a --project infrastructure-as-types
```

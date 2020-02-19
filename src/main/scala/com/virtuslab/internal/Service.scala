package com.virtuslab.internal

case class ServiceSpec(selector: Any)

case class Service(metadata: Metadata, spec: ServiceSpec)

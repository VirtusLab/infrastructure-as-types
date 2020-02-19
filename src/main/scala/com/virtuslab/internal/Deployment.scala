package com.virtuslab.internal

case class Template(spec: PodSpec, metadata: Metadata)

case class Selector(matchLabels: List[Label] = Nil)

case class DeploymentSpec(template: Template, selector: Selector)

case class Deployment(spec: DeploymentSpec, metadata: Metadata)

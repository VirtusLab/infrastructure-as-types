package com.virtuslab.internal

case class Annotation(key: String, value: String)

case class Label(key: String, value: String)

case class Metadata(labels: List[Label], annotations: List[Annotation])

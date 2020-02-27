package com.virtuslab.dsl

case class System(
    name: String,
    applications: List[Application] = Nil,
    configurations: List[Configuration] = Nil) {
  def addApplication(application: Application): System = {
    copy(applications = application :: applications)
  }

  def addConfiguration(configuration: Configuration): System = {
    copy(configurations = configuration :: configurations)
  }
}

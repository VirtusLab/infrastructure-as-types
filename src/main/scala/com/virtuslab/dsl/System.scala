package com.virtuslab.dsl

case class System(name: String, applications: List[Application] = Nil) {
  def addApplication(application: Application): System = {
    copy(applications = application :: applications)
  }
}

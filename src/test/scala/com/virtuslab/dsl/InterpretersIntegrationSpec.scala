package com.virtuslab.dsl

import com.virtuslab.dsl.{Application, HttpApplication, HttpApplicationInterpreter, System, SystemInterpreter}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpretersIntegrationSpec extends AnyFlatSpec with Matchers {

  it should "create a system" in {
    val appOne = HttpApplication("app-one", "image-app-one")
      .listensOn(9090)

    val appTwo = HttpApplication("app-two", "image-app-two")
      .listensOn(9090, "http-port")

    val system = System("test-system")
      .addApplication(appOne)
      .addApplication(appTwo)

    val httpApplicationInterpreter = new HttpApplicationInterpreter(system)
    val systemInterpreter = new SystemInterpreter({
      case _: HttpApplication => httpApplicationInterpreter
    })
    systemInterpreter(system)
  }

}

package com.cypherlabs.akka.playground.scala.iot

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.cypherlabs.akka.playground.scala.iot.Device.Passivate
import com.cypherlabs.akka.playground.scala.iot.DeviceManager.{DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice}
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceGroupSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "DeviceGroup actor" must {

    "be able to register a device actor and return it" in {

      // test data
      val probeForDeviceRegistered = createTestProbe[DeviceRegistered]()
      val deviceGroupActor = spawn(DeviceGroup("testDeviceGroup"))

      // invocations and validations
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice", probeForDeviceRegistered.ref)
      val registeredDevice1 = probeForDeviceRegistered.receiveMessage.device
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice", probeForDeviceRegistered.ref)
      val registeredDevice2 = probeForDeviceRegistered.receiveMessage().device
      registeredDevice1 should ===(registeredDevice2)
    }

    "be able to ignore list request if group id is a mismatch" in {

      // test data
      val probeForDeviceRegistered = createTestProbe[DeviceRegistered]()
      val deviceGroupActor = spawn(DeviceGroup("testDeviceGroup"))

      // invocations and validations
      deviceGroupActor ! RequestTrackDevice("testDeviceGroupDiff", "testDevice", probeForDeviceRegistered.ref)
      probeForDeviceRegistered.expectNoMessage()
    }

    "be able to list active devices" in {

      // test data
      val probeForDeviceRegistered = createTestProbe[DeviceRegistered]()
      val deviceGroupActor = spawn(DeviceGroup("testDeviceGroup"))

      // invocations and validations
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice1", probeForDeviceRegistered.ref)
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice2", probeForDeviceRegistered.ref)
      val probeForRequestDeviceList = createTestProbe[ReplyDeviceList]()
      deviceGroupActor ! RequestDeviceList(0 , "testDeviceGroup", probeForRequestDeviceList.ref)
      probeForRequestDeviceList.expectMessage(ReplyDeviceList(requestId = 0, Set("testDevice1", "testDevice2")))
    }

    "be able to list active devices after one shuts down" in {

      // test data
      val probeForDeviceRegistered = createTestProbe[DeviceRegistered]()
      val deviceGroupActor = spawn(DeviceGroup("testDeviceGroup"))

      // invocations and validations
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice1", probeForDeviceRegistered.ref)
      val deviceToShutDown = probeForDeviceRegistered.receiveMessage().device
      deviceGroupActor ! RequestTrackDevice("testDeviceGroup", "testDevice2", probeForDeviceRegistered.ref)

      val probeForRequestDeviceList = createTestProbe[ReplyDeviceList]()
      deviceGroupActor ! RequestDeviceList(0 , "testDeviceGroup", probeForRequestDeviceList.ref)
      probeForRequestDeviceList.expectMessage(ReplyDeviceList(requestId = 0, Set("testDevice1", "testDevice2")))

      // shut down device actor
      deviceToShutDown ! Passivate
      probeForDeviceRegistered.expectTerminated(deviceToShutDown, probeForDeviceRegistered.remainingOrDefault)

      probeForDeviceRegistered.awaitAssert {
        deviceGroupActor ! RequestDeviceList(1, "testDeviceGroup", probeForRequestDeviceList.ref)
        probeForRequestDeviceList.expectMessage(ReplyDeviceList(requestId = 1, Set("testDevice2")))
      }
    }
  }
}

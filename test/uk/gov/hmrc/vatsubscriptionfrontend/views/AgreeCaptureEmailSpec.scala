/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatsubscriptionfrontend.views

import assets.MessageLookup.{AgreeCaptureEmail => messages}
import play.api.i18n.Messages.Implicits._
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.vatsubscriptionfrontend.config.AppConfig

class AgreeCaptureEmailSpec extends ViewSpec {

  val env = Environment.simple()
  val configuration = Configuration.load(env)
  val testVrn = ""

  lazy val messagesApi = app.injector.instanceOf[MessagesApi]

  lazy val page = uk.gov.hmrc.vatsubscriptionfrontend.views.html.agree_capture_email(
    postAction = testCall)(
    FakeRequest(),
    applicationMessages,
    new AppConfig(configuration, env)
  )

  "The Agree Capture email view" should {

    val testPage = TestView(
      name = "Agree Capture Email View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.shouldHaveParaSeq(
      messages.line1,
      messages.line2
    )

    testPage.shouldHaveForm("Capture email Form")(actionCall = testCall)

    testPage.shouldHaveAgreeAndContinueButton()

    testPage.shouldHaveSignOutLink()
  }

}


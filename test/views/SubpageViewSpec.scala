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

package views

import models.VatEnrolment
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.domain.{CtUtr, Vrn}
import views.behaviours.ViewBehaviours
import views.html.subpage

class SubpageViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "subpage"

  val utr = Vrn("this-is-a-vrn")
  val vatEnrolment = VatEnrolment(utr, isActivated = true)

  def createView = () => subpage(frontendAppConfig, vatEnrolment, Html("<p id=\"partial-content\">hello world</p>"))(HtmlFormat.empty)(fakeRequest, messages)

  "Subpage view" must {
    behave like normalPage(createView, messageKeyPrefix)
    "contain correct content" in {
      val doc = asDocument(createView())
      doc.getElementsByTag("h1").first().text() mustBe "VAT details"
      doc.getElementById("payments-notice").text() mustBe
        "Information Payments will take 4 to 7 working days to show on this page. Completed return amounts will take 1 to 2 days."
    }

    "render the provided partial content" in {
      val doc = asDocument(createView()).getElementById("partial-content").text mustBe "hello world"
    }
  }

  "Subpage sidebar" must {

    "exist" in {
      assertRenderedByTag(asDocument(createView()), "aside")
    }
  }
}
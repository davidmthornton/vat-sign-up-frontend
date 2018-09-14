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

package controllers

import connectors.models.VatNoData
import controllers.actions._
import controllers.helpers.AccountSummaryHelper
import models.requests.AuthenticatedRequest
import models.{VatDecEnrolment, VatNoEnrolment}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{MandationStatusFetcherService, VatService}
import uk.gov.hmrc.domain.Vrn
import views.html.partial

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PartialControllerSpec extends ControllerSpecBase with MockitoSugar {

  //TODO: Needs VatModel
  val mockAccountSummaryHelper: AccountSummaryHelper = mock[AccountSummaryHelper]
  when(mockAccountSummaryHelper.getAccountSummaryView(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Html(""))
  val fakeSummary = Html("<p>This is the account summary</p>")
  val fakeVatVarInfo = Html("<p>This is the vat var info</p>")

  val mockVatService = mock[VatService]
  when(mockVatService.fetchVatModel(Matchers.any())(Matchers.any())).thenReturn(Future(VatNoData))

  val mockMandationStatusFetcherService = mock[MandationStatusFetcherService]
  when(mockMandationStatusFetcherService.getMandationStatus).thenReturn(false)

  def controller() =
    new PartialController(messagesApi, FakeAuthAction, FakeServiceInfoAction, mockAccountSummaryHelper, frontendAppConfig, mockVatService, mockMandationStatusFetcherService)

  def vrnEnrolment(activated: Boolean = true) = VatDecEnrolment(Vrn("vrn"), isActivated = true)

  def requestWithEnrolment(activated: Boolean): AuthenticatedRequest[AnyContent] = {
    AuthenticatedRequest[AnyContent](FakeRequest(), "", vrnEnrolment(activated), VatNoEnrolment())
  }

  def viewStandardPartialAsString() = partial(Vrn("vrn"), frontendAppConfig, Html(""))(fakeRequest, messages).toString

  def viewMtdPartialAsString() = partial(Vrn("vrn"), frontendAppConfig, Html(""))(fakeRequest, messages).toString

  "Partial Controller" must {

    "return OK and the standard partial view for a GET" in {

      val mockMandationFetcherService = mock[MandationStatusFetcherService]
      when(mockMandationFetcherService.getMandationStatus).thenReturn(false)

      controller()

      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewStandardPartialAsString()
    }

    "return OK and the 'Your agent has signed you up for MTD VAT' partial view for a GET" in {

      val mockMandationFetcherService = mock[MandationStatusFetcherService]
      when(mockMandationFetcherService.getMandationStatus).thenReturn(true)

      controller()

      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewMtdPartialAsString()
    }
  }
}





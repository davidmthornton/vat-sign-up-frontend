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

package uk.gov.hmrc.vatsignupfrontend.controllers.principal

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.CtKnownFactsIdentityVerification
import uk.gov.hmrc.vatsignupfrontend.config.mocks.MockControllerComponents
import uk.gov.hmrc.vatsignupfrontend.helpers.TestConstants.{testCompanyNumber, testVatNumber}
import uk.gov.hmrc.vatsignupfrontend.services.mocks.MockStoreCompanyNumberService

class ConfirmCompanyNumberControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockControllerComponents
  with MockStoreCompanyNumberService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(CtKnownFactsIdentityVerification)
  }

  object TestConfirmCompanyNumberController extends ConfirmCompanyNumberController(mockControllerComponents, mockStoreCompanyNumberService)

  val testGetRequest = FakeRequest("GET", "/confirm-company-number")

  val testPostRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("POST", "/confirm-company-number")

  "Calling the show action of the Confirm Company Number controller" when {
    "there is a company number in the session" should {
      "go to the Confirm Company Number page" in {
        mockAuthAdminRole()
        val request = testGetRequest.withSession(
          SessionKeys.vatNumberKey -> testVatNumber,
          SessionKeys.companyNumberKey -> testCompanyNumber
        )

        val result = TestConfirmCompanyNumberController.show(request)
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }
    "there isn't a vat number in the session" should {
      "go to the your vat number page" in {
        mockAuthAdminRole()

        val result = TestConfirmCompanyNumberController.show(testGetRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.ResolveVatNumberController.resolve().url)
      }
    }
    "there isn't a company number in the session" should {
      "go to the capture company number page" in {
        mockAuthAdminRole()

        val result = TestConfirmCompanyNumberController.show(testGetRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CaptureCompanyNumberController.show().url)
      }
    }
  }


  "Calling the submit action of the Confirm Company Number controller" when {
    "vat number is in session" when {
      "CtKnownFactsIdentityVerification is disabled and store vat is successful" should {
        "go to the 'agree to receive emails' page" in {
          mockAuthAdminRole()
          mockStoreCompanyNumberSuccess(vatNumber = testVatNumber, companyNumber = testCompanyNumber, companyUtr = None)

          val result = TestConfirmCompanyNumberController.submit(testPostRequest.withSession(
            SessionKeys.vatNumberKey -> testVatNumber,
            SessionKeys.companyNumberKey -> testCompanyNumber
          ))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.AgreeCaptureEmailController.show().url)
        }
      }
      "CtKnownFactsIdentityVerification is enabeled" should {
        "go to the 'agree to receive emails' page" in {
          mockAuthAdminRole()
          enable(CtKnownFactsIdentityVerification)

          val result = TestConfirmCompanyNumberController.submit(testPostRequest.withSession(
            SessionKeys.vatNumberKey -> testVatNumber,
            SessionKeys.companyNumberKey -> testCompanyNumber
          ))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.CaptureCompanyUtrController.show().url)
        }
      }
    }
    "vat number is in session but store vat is unsuccessful" should {
      "throw internal server exception" in {
        mockAuthAdminRole()
        mockStoreCompanyNumberFailure(vatNumber = testVatNumber, companyNumber = testCompanyNumber, companyUtr = None)

        intercept[InternalServerException] {
          await(TestConfirmCompanyNumberController.submit(testPostRequest.withSession(
            SessionKeys.vatNumberKey -> testVatNumber,
            SessionKeys.companyNumberKey -> testCompanyNumber
          )))
        }
      }
    }
    "vat number is not in session" should {
      "redirect to your vat number" in {
        mockAuthAdminRole()

        val result = TestConfirmCompanyNumberController.submit(testPostRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.ResolveVatNumberController.resolve().url)
      }
    }
    "company number is not in session" should {
      "redirect to capture company number" in {
        mockAuthAdminRole()

        val result = TestConfirmCompanyNumberController.submit(testPostRequest.withSession(SessionKeys.vatNumberKey -> testVatNumber))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CaptureCompanyNumberController.show().url)
      }
    }
  }
}
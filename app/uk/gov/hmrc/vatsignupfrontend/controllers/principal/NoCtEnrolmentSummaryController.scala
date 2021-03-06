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

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignupfrontend.SessionKeys
import uk.gov.hmrc.vatsignupfrontend.config.ControllerComponents
import uk.gov.hmrc.vatsignupfrontend.config.auth.AdministratorRolePredicate
import uk.gov.hmrc.vatsignupfrontend.config.featureswitch.CtKnownFactsIdentityVerification
import uk.gov.hmrc.vatsignupfrontend.controllers.AuthenticatedController
import uk.gov.hmrc.vatsignupfrontend.httpparsers.StoreCompanyNumberHttpParser.{CtReferenceMismatch, StoreCompanyNumberSuccess}
import uk.gov.hmrc.vatsignupfrontend.models._
import uk.gov.hmrc.vatsignupfrontend.services.StoreCompanyNumberService
import uk.gov.hmrc.vatsignupfrontend.utils.SessionUtils._
import uk.gov.hmrc.vatsignupfrontend.views.html.principal.no_ct_enrolment_summary

import scala.concurrent.Future

@Singleton
class NoCtEnrolmentSummaryController @Inject()(val controllerComponents: ControllerComponents,
                                               val storeCompanyNumberService: StoreCompanyNumberService)
  extends AuthenticatedController(AdministratorRolePredicate, featureSwitches = Set(CtKnownFactsIdentityVerification)) {

  def show: Action[AnyContent] = Action.async { implicit request =>
    authorised() {

      val optBusinessEntity = request.session.getModel[BusinessEntity](SessionKeys.businessEntityKey)
      val optCompanyNumber = request.session.get(SessionKeys.companyNumberKey).filter(_.nonEmpty)
      val optCompanyUtr = request.session.get(SessionKeys.companyUtrKey).filter(_.nonEmpty)

      (optBusinessEntity, optCompanyNumber, optCompanyUtr) match {
        case (Some(entity@(SoleTrader | LimitedCompany)), Some(companyNumber), Some(companyUtr)) =>
          Future.successful(
            Ok(no_ct_enrolment_summary(
              companyNumber = companyNumber,
              companyUtr = companyUtr,
              businessEntity = entity,
              routes.NoCtEnrolmentSummaryController.submit()))
          )
        case (None, _, _) =>
          Future.successful(
            Redirect(routes.CaptureBusinessEntityController.show())
          )
        case (_, None, _) =>
          Future.successful(
            Redirect(routes.CaptureCompanyNumberController.show())
          )
        case (_, _, None) =>
          Future.successful(
            Redirect(routes.CaptureCompanyUtrController.show())
          )
      }
    }
  }

  private def storeCompanyNumber(vatNumber: String, companyNumber: String, companyUtr: String)(implicit hc: HeaderCarrier) =
    storeCompanyNumberService.storeCompanyNumber(vatNumber, companyNumber, Some(companyUtr)).map {
      case Right(StoreCompanyNumberSuccess) => Redirect(routes.AgreeCaptureEmailController.show())
      case Left(CtReferenceMismatch) =>
        //TODO confirm redirection and content for CT mismatch
        Redirect(routes.CouldNotConfirmBusinessController.show())
      case Left(failure) => throw new InternalServerException("unexpected response on store company number " + failure.status)
    }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authorised() {

      val optVatNumber = request.session.get(SessionKeys.vatNumberKey).filter(_.nonEmpty)
      val optBusinessEntity = request.session.getModel[BusinessEntity](SessionKeys.businessEntityKey)
      val optCompanyNumber = request.session.get(SessionKeys.companyNumberKey).filter(_.nonEmpty)
      val optCompanyUtr = request.session.get(SessionKeys.companyUtrKey).filter(_.nonEmpty)

      (optBusinessEntity, optCompanyNumber, optCompanyUtr, optVatNumber) match {
        case (Some(SoleTrader | LimitedCompany), Some(companyNumber), Some(companyUtr), Some(vatNumber)) =>
          storeCompanyNumber(vatNumber, companyNumber, companyUtr)
        case (None, _, _, _) =>
          Future.successful(
            Redirect(routes.CaptureBusinessEntityController.show())
          )
        case (_, None, _, _) =>
          Future.successful(
            Redirect(routes.CaptureCompanyNumberController.show())
          )
        case (_, _, None, _) =>
          Future.successful(
            Redirect(routes.CaptureCompanyUtrController.show())
          )
        case (_, _, _, None) =>
          Future.successful(
            Redirect(routes.CaptureVatNumberController.show())
          )
      }
    }
  }
}

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

import uk.gov.hmrc.vatsubscriptionfrontend.assets.MessageLookup.{Base => common}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsubscriptionfrontend.config.AppConfig

trait ViewSpec extends UnitSpec with GuiceOneAppPerSuite {

  val testBackUrl = "/test-back-url"
  val testCall = Call("POST", "/test-url")
  val viewTestRequest = FakeRequest("POST", "/test-url")

  trait ElementTest {
    val name: String

    val element: Element

    def shouldHavePara(paragraph: String): Unit =
      s"$name should have the paragraph (P) '$paragraph'" in {
        element.getElementsByTag("p").text() should include(paragraph)
      }

    def shouldHaveParaSeq(paragraphs: String*): Unit = {
      if (paragraphs.isEmpty) fail("Should provide at least 1 paragraph for this test")
      val ps = paragraphs.mkString(" ")
      s"$name should have the paragraphs (P) [${paragraphs.mkString("], [")}]" in {
        element.getElementsByTag("p").text() should include(ps)
      }
    }

    def shouldHaveBulletSeq(bullets: String*): Unit = {
      if (bullets.isEmpty) fail("Should provide at least 1 bullet point for this test")
      val bs = bullets.mkString(" ")
      s"$name should have the bulletPoints (LI) [${bullets.mkString("], [")}]" in {
        element.getElementsByTag("li").text() should include(bs)
      }
    }

    def shouldHaveH2(text: String): Unit =
      s"$name have a Heading 2 (H2) for '$text'" in {
        element.getElementsByTag("h2").text() should include(text)
      }

    def shouldHaveH3(text: String): Unit =
      s"$name have a Heading 3 (H3) for '$text'" in {
        element.getElementsByTag("h3").text() should include(text)
      }

    def shouldHaveHint(hint: String): Unit =
      s"$name should have the hint text '$hint'" in {
        element.getElementsByTag("span").hasClass("form-hint") shouldBe true
        element.getElementsByTag("span").text() should include(hint)
      }

    def shouldHaveALink(text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.select("a")
        if (link == null) fail(s"Unable to locate any links in $name\n$element\n")
        if (link.size() > 1) fail(s"Multiple links located in $name, please specify an id")
        link.attr("href") shouldBe href
        link.text() shouldBe text
      }

    def shouldHaveALink(id: String, text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.getElementById(id)
        if (link == null) fail(s"Unable to locate $id")
        if (!link.tagName().equals("a")) fail(s"The element with id=$id is not a link")
        link.attr("href") shouldBe href
        link.text() shouldBe text
      }

    def shouldHaveSignOutLink(text: String = common.signOut): Unit = {
      val id = "sign-out"
      shouldHaveALink(id, text, app.injector.instanceOf[AppConfig].ggSignOutUrl())
    }

    def shouldHaveSignOutButton(text: String = common.signOut): Unit = {
      val id = "sign-out-button"
      shouldHaveALink(id, text, app.injector.instanceOf[AppConfig].ggSignOutUrl())
    }

    def shouldHaveTextField(name: String,
                            label: String
                           ): Unit = {

      s"${this.name} should have an input field '$name'" which {

        s"is a text field" in {
          import collection.JavaConversions._
          val eles = element.select(s"""input[name=$name]""")
          if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
          if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
          val ele = eles.head
          ele.attr("type") shouldBe "text"
        }

        lazy val labelField = element.select(s"label[for=$name]")

        s"with the expected label '$label'" in {
          labelField.text() shouldBe label
        }

        s"and the label should be visuallyhidden" in
          withClue(s"$name does not have the class 'visuallyhidden'\n") {
            labelField.hasClass("visuallyhidden") shouldBe true
          }
      }
    }

    private def shouldHaveSubmitButton(text: String): Unit =
      s"$name should have the a submit button (Button) '$text'" in {
        import collection.JavaConversions._
        val submitButtons = element.getElementsByTag("input").filter(_.attr("type") == "submit")
        submitButtons.size shouldBe 1
        submitButtons.head.attr("class") should include("button")
        submitButtons.head.attr("value") shouldBe text
      }

    def shouldHaveContinueButton(): Unit = shouldHaveSubmitButton(common.continue)

    def shouldHaveConfirmAndContinueButton(): Unit = shouldHaveSubmitButton(common.confirmAndContinue)

    def shouldHaveAcceptAndContinueButton(): Unit = shouldHaveSubmitButton(common.acceptAndContinue)

    def shouldHaveAgreeAndContinueButton(): Unit = shouldHaveSubmitButton(common.agreeAndContinue)

    def shouldHaveContinueToSignUpButton(): Unit = shouldHaveSubmitButton(common.continueToSignUp)

    def shouldHaveForm(formName: String, id: Option[String] = None)(actionCall: => Call): Unit = {
      val selector =
        id match {
          case Some(i) => s"#$i"
          case _ => "form"
        }

      lazy val method = actionCall.method
      lazy val url = actionCall.url
      // this test is put in here because it doesn't make sense for it to be called on anything
      // other than a form
      s"$formName in $name must have a $method action to '$url'" in {
        val formSelector = element.select(selector)
        formSelector.attr("method") shouldBe method.toUpperCase
        formSelector.attr("action") shouldBe url
      }
    }
  }


  class TestView(override val name: String,
                 title: String,
                 heading: String,
                 page: => Html) extends ElementTest {

    lazy val document: Document = Jsoup.parse(page.body)
    override lazy val element: Element = document.getElementById("content")

    s"$name should have the title '$title'" in {
      document.title() shouldBe title
    }

    s"$name should have the heading (H1) '$heading'" in {
      val h1 = document.getElementsByTag("H1")
      h1.size() shouldBe 1
      h1.text() shouldBe heading
    }

  }

  object TestView {
    def apply(name: String,
              title: String,
              heading: String,
              page: => Html): TestView = new TestView(name, title, heading, page)
  }

}
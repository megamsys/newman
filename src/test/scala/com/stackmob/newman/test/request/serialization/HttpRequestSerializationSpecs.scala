/**
 * Copyright 2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.newman.test
package request.serialization

import scalaz._
import Scalaz._
import org.specs2.Specification
import org.specs2.execute.{Result => SpecsResult}
import com.stackmob.newman._
import java.net.URL
import com.stackmob.newman.request.{HttpRequestWithoutBody, HttpRequest, HttpRequestWithBody}
import org.specs2.matcher.{MatchSuccess, MatchFailure, MatchResult}

class HttpRequestSerializationSpecs extends Specification { def is =
  "HttpRequestSerializationSpecs".title                                                                                 ^
  """
  HttpRequest serialization is responsible for changing an HttpRequest into a JValue and back. It can be
  used to serialize an HttpRequest for storage in a cache, for output to the console, etc...
  """                                                                                                                   ^
  "HttpRequestSerialization should"                                                                                     ^
    "round trip correctly"                                                                                              ! RoundTrip().succeeds ^
  end

  trait Context extends BaseContext {

    protected lazy val url = new URL("http://stackmob.com")
    protected lazy val headers = Headers("header1" -> "header1")
    protected lazy val body = RawBody("abcd")
    protected lazy val getReq = client.get(url, headers)
    protected lazy val postReq = client.post(url, headers, body)
    protected lazy val putReq = client.put(url, headers, body)
    protected lazy val deleteReq = client.delete(url, headers)
    protected lazy val headReq = client.head(url, headers)
    protected lazy val client = new DummyHttpClient

    private def ensure(t: HttpRequest)(extra: HttpRequest => MatchResult[_]): MatchResult[_] = {
      val json = t.toJson(false)(client)
      HttpRequest.fromJson(json)(client).fold(
        success = { deser: HttpRequest =>
          (deser.url must beEqualTo(t.url)) and
          (deser.headers must haveTheSameHeadersAs(t.headers)) and
          (extra(deser))
        },
        failure = { eNel =>
          MatchFailure("ok", "deserialization failed with %d errors".format(eNel.list.length), (false must beTrue).expectable)
        }
      )
    }

    protected def ensure(t: HttpRequestWithoutBody): MatchResult[_] = {
      ensure(t: HttpRequest) { r: HttpRequest =>
        MatchSuccess("ok", "error", (true must beTrue).expectable)
      }
    }

    protected def ensure(t: HttpRequestWithBody): MatchResult[_] = {
      ensure(t: HttpRequest) { r: HttpRequest =>
        r.cast[HttpRequestWithBody].map { d: HttpRequestWithBody =>
          (d.body must beEqualTo(t.body)): MatchResult[_]
        } | {
          MatchFailure("ok", "deserialization returned a %s, not HttpRequestWithBody as expected".format(r.getClass.getCanonicalName), (false must beTrue).expectable)
        }
      }
    }
  }

  case class RoundTrip() extends Context {
    def succeeds: SpecsResult = {
      val getRes = ensure(getReq)
      val postRes = ensure(postReq)
      val putRes = ensure(putReq)
      val deleteRes = ensure(deleteReq)
      val headRes = ensure(headReq)

      getRes and postRes and putRes and deleteRes and headRes
    }
  }

}

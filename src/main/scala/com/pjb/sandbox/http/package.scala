package com.pjb.sandbox

package object http {
  case class LatestStateResponse(uptoSeqNo:Option[Int], data: Option[String])
}

package com.adform.dspr.discrepancy

import org.slf4j.Logger

trait Logging {
  val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)
}

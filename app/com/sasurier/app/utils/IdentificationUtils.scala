package com.sasurier.app.utils

import java.security.SecureRandom

object IdentificationUtils {
  private val RNG = new SecureRandom
  private val VALID_CHARS = Array[Char]('1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')

  def generateUid(length: Int): String = {
    val builder = new StringBuilder(length)
    var i = 0
    while ( {
      i < length
    }) {
      builder.append(VALID_CHARS(RNG.nextInt(VALID_CHARS.length)))

      {
        i += 1; i
      }
    }
    builder.toString
  }

  def addCheckDigit(original: String): String = if (original != null && original.length > 0) {
    val checkDigit = computeCheckDigit(original, 0, original.length - 1)
    original + checkDigit
  }
  else null

  def verifyCheckDigit(encoded: String): String = {
    if (encoded.length > 1) {
      val expectedCheckDigit = computeCheckDigit(encoded, 0, encoded.length - 2)
      if (expectedCheckDigit == encoded.charAt(encoded.length - 1)) return encoded.substring(0, encoded.length - 1)
    }
    null
  }

  private def computeCheckDigit(value: String, startIndex: Int, endIndex: Int) = {
    var total = 0
    var position = startIndex
    while ( {
      position <= endIndex
    }) {
      total += position * value.charAt(position)

      {
        position += 1; position
      }
    }
    VALID_CHARS(total % VALID_CHARS.length)
  }
}
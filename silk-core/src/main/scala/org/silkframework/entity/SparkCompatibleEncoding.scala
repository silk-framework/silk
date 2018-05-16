package org.silkframework.entity

import java.net.{URLDecoder, URLEncoder}

import org.silkframework.util.Uri

/**
  * Functions for encoding strings.
  */
object SparkCompatibleEncoding {

  /**
    * Encode a given String with the URLEncoder nad the given encoding.
    * The encoding is not applied if the String already was encoded.
    *
    * @param original Input String
    * @param encoding Encoding, e.g. "UTF-8"
    * @return Encoded but not twice encoded String
    */
  def encode(original: String, encoding: String = "UTF-8"): Uri = {
    val orifUri = Uri.parse(original)
    if(orifUri.isValidUri)
      return orifUri

    val encoded = URLEncoder.encode(original, encoding)
    if (encoded.contains("+")) {
      if (original.equals(encoded.replaceAll("\\+", " "))) {
        encoded
      }
      else {
        original.replaceAll(" ", "+")
      }
    }
    else {
      original.replaceAllLiterally("\\","%5C").replaceAllLiterally("/","%2F").replaceAll(" ", "+")
    }

  }

  def decode(uri: Uri, encoding: String = "UTF-8"): String ={
    URLDecoder.decode(uri.uri, encoding)
  }
}
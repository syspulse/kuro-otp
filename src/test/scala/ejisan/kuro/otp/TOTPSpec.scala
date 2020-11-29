package ejisan.kuro.otp

import org.scalatest._

class TOTPSpec extends FlatSpec with Matchers {

  val otpkeySHA1 = OTPKey.fromHex(
    "3132333435363738393031323334353637383930")
  val otpkeySHA256 = OTPKey.fromHex(
    "3132333435363738393031323334353637383930" +
    "313233343536373839303132")
  val otpkeySHA512 = OTPKey.fromHex(
    "3132333435363738393031323334353637383930" +
    "3132333435363738393031323334353637383930" +
    "3132333435363738393031323334353637383930" +
    "31323334")

  val totpSHA1 = TOTP(OTPAlgorithm.SHA1, 8, 30, otpkeySHA1)
  val totpSHA256 = TOTP(OTPAlgorithm.SHA256, 8, 30, otpkeySHA256)
  val totpSHA512 = TOTP(OTPAlgorithm.SHA512, 8, 30, otpkeySHA512)

  "TOTP" should "generate and validate a TOTP code by current time" in {
    val codeSHA1 = totpSHA1.generate()
    totpSHA1.validate(codeSHA1) should be (true)
    totpSHA1.validate(10, codeSHA1) should be (Some(0))
    val codeSHA256 = totpSHA256.generate()
    totpSHA256.validate(codeSHA256) should be (true)
    totpSHA256.validate(10, codeSHA256) should be (Some(0))
    val codeSHA512 = totpSHA512.generate()
    totpSHA512.validate(codeSHA512) should be (true)
    totpSHA512.validate(10, codeSHA512) should be (Some(0))
  }

  // https://tools.ietf.org/html/rfc6238#appendix-B
  "TOTP.generate" should "generate a TOTP code (Appendix B.  Test Vectors)" in {
    totpSHA1.generate(59L) should not be ("00000000")
    totpSHA1.generate(59L) should not be ("111111")

    totpSHA1.generate(59L) should be ("94287082")
    totpSHA256.generate(59L) should be ("46119246")
    totpSHA512.generate(59L) should be ("90693936")

    totpSHA1.generate(1111111109L) should be ("07081804")
    totpSHA256.generate(1111111109L) should be ("68084774")
    totpSHA512.generate(1111111109L) should be ("25091201")

    totpSHA1.generate(1111111111L) should be ("14050471")
    totpSHA256.generate(1111111111L) should be ("67062674")
    totpSHA512.generate(1111111111L) should be ("99943326")

    totpSHA1.generate(1234567890L) should be ("89005924")
    totpSHA256.generate(1234567890L) should be ("91819424")
    totpSHA512.generate(1234567890L) should be ("93441116")

    totpSHA1.generate(2000000000L) should be ("69279037")
    totpSHA256.generate(2000000000L) should be ("90698825")
    totpSHA512.generate(2000000000L) should be ("38618901")

    totpSHA1.generate(20000000000L) should be ("65353130")
    totpSHA256.generate(20000000000L) should be ("77737706")
    totpSHA512.generate(20000000000L) should be ("47863826")
  }

  it should "generate TOTP codes with window" in {
    totpSHA1.generate(1111112040L, 2) should === (Map(
      37037067L -> "79453447",
      37037068L -> "95565820",
      37037069L -> "19570641",
      37037070L -> "93804954",
      37037066L -> "88393293"))
  }

  "TOTP.validate" should "validate the TOTP code (Appendix B.  Test Vectors)" in {
    totpSHA1.validate(59L, "00000000") should be (false)
    totpSHA1.validate(59L, "111111") should be (false)

    totpSHA1.validate(59L, "94287082") should be (true)
    totpSHA256.validate(59L, "46119246") should be (true)
    totpSHA512.validate(59L, "90693936") should be (true)

    totpSHA1.validate(1111111109L, "07081804") should be (true)
    totpSHA256.validate(1111111109L, "68084774") should be (true)
    totpSHA512.validate(1111111109L, "25091201") should be (true)

    totpSHA1.validate(1111111111L, "14050471") should be (true)
    totpSHA256.validate(1111111111L, "67062674") should be (true)
    totpSHA512.validate(1111111111L, "99943326") should be (true)

    totpSHA1.validate(1234567890L, "89005924") should be (true)
    totpSHA256.validate(1234567890L, "91819424") should be (true)
    totpSHA512.validate(1234567890L, "93441116") should be (true)

    totpSHA1.validate(2000000000L, "69279037") should be (true)
    totpSHA256.validate(2000000000L, "90698825") should be (true)
    totpSHA512.validate(2000000000L, "38618901") should be (true)

    totpSHA1.validate(20000000000L, "65353130") should be (true)
    totpSHA256.validate(20000000000L, "77737706") should be (true)
    totpSHA512.validate(20000000000L, "47863826") should be (true)
  }

  it should "validate the TOTP code with window and returns the gap" in {
    totpSHA1.validate(1111112040L, 1, "79453447") should be (Some(-1))
    totpSHA1.validate(1111112040L, 1, "95565820") should be (Some(0))
    totpSHA1.validate(1111112040L, 1, "19570641") should be (Some(1))
    totpSHA1.validate(1111112040L, 1, "93804954") should be (None)
  }

  "TOTP.toURI" should "returns `otpauth` [java.net.URI]." in {
    val account = "Account Name"
    val issuer = Some("Ejisan Kuro")
    val decoded = OTPAuthURICodec.decode(totpSHA1.toURI(account, issuer))
    decoded.get.protocol should be ("totp")
    decoded.get.account should be (account)
    decoded.get.otpkey should be (otpkeySHA1)
    decoded.get.issuer should be (issuer)
    decoded.get.params("algorithm") should be (OTPAlgorithm.SHA1.name)
    decoded.get.params("digits") should be ("8")
    decoded.get.params("period") should be ("30")
  }
}

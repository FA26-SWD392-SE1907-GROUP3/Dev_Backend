package com.example.swd392_se1907_aives.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Date;

class GoogleTokenVerifierTest {
 @Test void validatesRsaSignatureAudienceExpiryVerifiedEmailAndNonce() throws Exception {
  var key=new RSAKeyGenerator(2048).generate();
  var verifier=new GoogleTokenVerifier("expected-client",NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build());
  var claims=new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience("expected-client").subject("google-sub")
   .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("email_verified",true).claim("nonce","expected-nonce").build();
  var token=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims);token.sign(new RSASSASigner(key));
  assertEquals("google-sub",verifier.verify(token.serialize(),"expected-nonce").getSubject());
  assertThrows(ResponseStatusException.class,() -> verifier.verify(token.serialize(),"wrong-nonce"));
  for(var bad : new JWTClaimsSet[]{new JWTClaimsSet.Builder(claims).audience("wrong-client").build(),
    new JWTClaimsSet.Builder(claims).expirationTime(Date.from(Instant.now().minusSeconds(180))).build(),
    new JWTClaimsSet.Builder(claims).claim("email_verified",false).build(),
    new JWTClaimsSet.Builder(claims).issuer("https://attacker.example").build()}) {
   var invalid=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),bad);invalid.sign(new RSASSASigner(key));
   assertThrows(ResponseStatusException.class,() -> verifier.verify(invalid.serialize(),"expected-nonce"));
  }
  var forged=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims);forged.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
  assertThrows(ResponseStatusException.class,() -> verifier.verify(forged.serialize(),"expected-nonce"));
 }
}

package br.com.opin.mopclient.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtPayloadSignerTest {

    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();
    }

    @Test
    void shouldSignPayloadAsJwtPs256WithKidTypAndOrgIdClaims() throws Exception {
        JwtPayloadSigner signer = new JwtPayloadSigner(privateKey, "kid-1", "org-test");

        byte[] payload = "{\"hello\":\"world\"}".getBytes();
        String token = signer.sign(payload);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        SignedJWT sjwt = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.PS256, sjwt.getHeader().getAlgorithm());
        assertEquals(JOSEObjectType.JWT, sjwt.getHeader().getType());
        assertEquals("kid-1", sjwt.getHeader().getKeyID());
        assertTrue(sjwt.verify(new RSASSAVerifier(publicKey)));

        assertEquals("org-test", sjwt.getJWTClaimsSet().getStringClaim("orgId"));
        assertEquals("world", sjwt.getJWTClaimsSet().getStringClaim("hello"));
        assertNotNull(sjwt.getJWTClaimsSet().getIssueTime());
        assertNotNull(sjwt.getJWTClaimsSet().getExpirationTime());
    }

    @Test
    void shouldFailFastWhenKidIsNull() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new JwtPayloadSigner(privateKey, null, "org-test"));
        assertTrue(ex.getMessage().toLowerCase().contains("kid"),
                "message should mention kid, was: " + ex.getMessage());
    }

    @Test
    void shouldFailFastWhenKidIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtPayloadSigner(privateKey, "", "org-test"));
        assertTrue(ex.getMessage().toLowerCase().contains("kid"),
                "message should mention kid, was: " + ex.getMessage());
    }

    @Test
    void shouldFailFastWhenKidIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtPayloadSigner(privateKey, "   \t  ", "org-test"));
        assertTrue(ex.getMessage().toLowerCase().contains("kid"),
                "message should mention kid, was: " + ex.getMessage());
    }

    @Test
    void shouldFailFastWhenOrgIdIsNull() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new JwtPayloadSigner(privateKey, "kid-1", null));
        assertTrue(ex.getMessage().toLowerCase().contains("orgid"),
                "message should mention orgId, was: " + ex.getMessage());
    }

    @Test
    void shouldFailFastWhenOrgIdIsEmpty() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtPayloadSigner(privateKey, "kid-1", ""));
        assertTrue(ex.getMessage().toLowerCase().contains("orgid"),
                "message should mention orgId, was: " + ex.getMessage());
    }

    @Test
    void shouldFailFastWhenOrgIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtPayloadSigner(privateKey, "kid-1", "   \n  "));
        assertTrue(ex.getMessage().toLowerCase().contains("orgid"),
                "message should mention orgId, was: " + ex.getMessage());
    }

    @Test
    void shouldNotReachSignCallWhenKidIsBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    JwtPayloadSigner signer = new JwtPayloadSigner(privateKey, "", "org-test");
                    signer.sign("{\"hello\":\"world\"}".getBytes());
                },
                "construction must fail before sign() is ever called");
    }

    @Test
    void shouldNotReachSignCallWhenOrgIdIsBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    JwtPayloadSigner signer = new JwtPayloadSigner(privateKey, "kid-1", "");
                    signer.sign("{\"hello\":\"world\"}".getBytes());
                },
                "construction must fail before sign() is ever called");
    }
}

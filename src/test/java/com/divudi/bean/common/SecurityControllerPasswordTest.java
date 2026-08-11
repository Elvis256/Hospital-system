package com.divudi.bean.common;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the password hashing / verification in {@link SecurityController}.
 *
 * New passwords are hashed with the strong (salted, iterated) scheme, while
 * {@code matchPassword} must still verify legacy MD5 hashes so existing users
 * are not locked out.
 */
@DisplayName("SecurityController password hashing")
public class SecurityControllerPasswordTest {

    private final SecurityController security = new SecurityController();

    @Test
    @DisplayName("a freshly hashed password verifies and is not the weak legacy format")
    void newHashRoundTrips() {
        String hash = security.hashAndCheck("s3cret!");
        assertNotNull(hash);
        // BasicPasswordEncryptor (legacy MD5) emits a 24-char base64 digest; the
        // strong scheme is noticeably longer. Guards against silently reverting.
        assertTrue(hash.length() > 32, "expected a strong hash longer than legacy MD5, was " + hash.length());
        assertTrue(SecurityController.matchPassword("s3cret!", hash), "correct password must verify");
        assertFalse(SecurityController.matchPassword("wrong", hash), "wrong password must not verify");
    }

    @Test
    @DisplayName("an existing legacy MD5 hash still verifies via the fallback (no lockout)")
    void legacyHashStillVerifies() {
        String legacy = new BasicPasswordEncryptor().encryptPassword("oldpass");
        assertTrue(SecurityController.matchPassword("oldpass", legacy),
                "existing MD5-hashed passwords must still authenticate");
        assertFalse(SecurityController.matchPassword("nope", legacy));
    }

    @Test
    @DisplayName("null inputs never match")
    void nullInputsNeverMatch() {
        assertFalse(SecurityController.matchPassword(null, "x"));
        assertFalse(SecurityController.matchPassword("x", null));
        assertFalse(SecurityController.matchPassword(null, null));
    }
}

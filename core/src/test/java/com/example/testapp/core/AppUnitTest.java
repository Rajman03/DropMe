package com.example.testapp.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppUnitTest {

    @Test
    public void testSocialPlatformDetection() {
        assertEquals(SocialPlatform.INSTAGRAM, SocialPlatform.detectFromUrl("https://www.instagram.com/my_profile"));
        assertEquals(SocialPlatform.FACEBOOK, SocialPlatform.detectFromUrl("https://facebook.com/profile"));
        assertEquals(SocialPlatform.TIKTOK, SocialPlatform.detectFromUrl("https://tiktok.com/@creator"));
        assertEquals(SocialPlatform.SNAPCHAT, SocialPlatform.detectFromUrl("https://snapchat.com/add/kamil"));
        assertEquals(SocialPlatform.YOUTUBE, SocialPlatform.detectFromUrl("https://youtube.com/@channel"));
        assertEquals(SocialPlatform.LINKEDIN, SocialPlatform.detectFromUrl("https://linkedin.com/in/john-doe"));
        assertEquals(SocialPlatform.TWITTER, SocialPlatform.detectFromUrl("https://x.com/tech"));
        assertEquals(SocialPlatform.DISCORD, SocialPlatform.detectFromUrl("https://discord.gg/community"));
        assertEquals(SocialPlatform.GITHUB, SocialPlatform.detectFromUrl("https://github.com/my-repo"));
        assertEquals(SocialPlatform.SPOTIFY, SocialPlatform.detectFromUrl("https://open.spotify.com/artist/123"));
        assertEquals(SocialPlatform.CUSTOM, SocialPlatform.detectFromUrl("https://mywebsite.com/about"));
    }

    @Test
    public void testPlatformBuildUrlFromHandles() {
        assertEquals("https://instagram.com/rajman", SocialPlatform.INSTAGRAM.buildUrl("@rajman"));
        assertEquals("https://instagram.com/rajman", SocialPlatform.INSTAGRAM.buildUrl("rajman"));
        assertEquals("https://www.tiktok.com/@rajman", SocialPlatform.TIKTOK.buildUrl("@rajman"));
        assertEquals("https://snapchat.com/add/rajman", SocialPlatform.SNAPCHAT.buildUrl("rajman"));
        assertEquals("https://x.com/rajman", SocialPlatform.TWITTER.buildUrl("@rajman"));
        assertEquals("https://custom-site.pl", SocialPlatform.CUSTOM.buildUrl("https://custom-site.pl"));
    }

    @Test
    public void testCredentialHasher() {
        String rawSecret = "superSecretPassword123";
        String hashed = CredentialHasher.hash(rawSecret);

        assertTrue(CredentialHasher.isHashed(hashed));
        assertTrue(CredentialHasher.verify(rawSecret, hashed));
        assertFalse(CredentialHasher.verify("wrongPassword", hashed));

        // Test PIN trivial check
        assertTrue(CredentialHasher.isTrivialPin("1234"));
        assertTrue(CredentialHasher.isTrivialPin("0000"));
        assertTrue(CredentialHasher.isTrivialPin("7777"));
        assertFalse(CredentialHasher.isTrivialPin("5829"));
    }

    @Test
    public void testAuthUserRoles() {
        AuthUser guest = new AuthUser("guest", "Gość", "", UserProfile.RANK_GUEST, "Gość", "👤");
        assertFalse(guest.isCreator());
        assertFalse(guest.isAdmin());

        AuthUser admin = new AuthUser("admin1", "Admin", "pass", UserProfile.RANK_ADMIN, "Mod", "🛡️");
        assertFalse(admin.isCreator());
        assertTrue(admin.isAdmin());

        AuthUser creator = new AuthUser("creator1", "Twórca", "pass", UserProfile.RANK_CREATOR, "Właściciel", "👑");
        assertTrue(creator.isCreator());
        assertTrue(creator.isAdmin());
    }

    @Test
    public void testUserSerialization() throws Exception {
        AuthUser original = new AuthUser("rajman", "Rajman", "secret",
                UserProfile.RANK_CREATOR, "Developer", "👑", true, "5829");
        AuthUser restored = AuthUser.fromJsonObject(original.toJsonObject());

        assertEquals(original.getUsername(), restored.getUsername());
        assertEquals(original.getDisplayName(), restored.getDisplayName());
        assertEquals(original.getRank(), restored.getRank());
        assertEquals(original.getTitle(), restored.getTitle());
        assertEquals(original.getAvatar(), restored.getAvatar());
        assertEquals(original.isPurchasedPremium(), restored.isPurchasedPremium());
        assertEquals(original.getPin(), restored.getPin());
    }

    @Test
    public void testMultiLinkProfileVCard() {
        MultiLinkProfile ml = new MultiLinkProfile();
        ml.setName("Kamil Rajman");
        ml.setBio("Developer");
        ml.setInstagram("rajman");
        ml.setTiktok("rajman04");
        ml.setSnapchat("rajman_sc");

        String vCard = ml.buildVCard();
        assertTrue(vCard.contains("BEGIN:VCARD"));
        assertTrue(vCard.contains("FN:Kamil Rajman"));
        assertTrue(vCard.contains("instagram.com/rajman"));
        assertTrue(vCard.contains("tiktok.com/@rajman04"));
        assertTrue(vCard.contains("snapchat.com/add/rajman_sc"));
        assertTrue(vCard.contains("END:VCARD"));
    }
}

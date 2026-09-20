package com.example.testapp.core;

public class SocialPlatformHelper {

    public static final String PLATFORM_INSTAGRAM = SocialPlatform.INSTAGRAM.getKey();
    public static final String PLATFORM_FACEBOOK = SocialPlatform.FACEBOOK.getKey();
    public static final String PLATFORM_TIKTOK = SocialPlatform.TIKTOK.getKey();
    public static final String PLATFORM_SNAPCHAT = SocialPlatform.SNAPCHAT.getKey();
    public static final String PLATFORM_YOUTUBE = SocialPlatform.YOUTUBE.getKey();
    public static final String PLATFORM_LINKEDIN = SocialPlatform.LINKEDIN.getKey();
    public static final String PLATFORM_TWITTER = SocialPlatform.TWITTER.getKey();
    public static final String PLATFORM_DISCORD = SocialPlatform.DISCORD.getKey();
    public static final String PLATFORM_GITHUB = SocialPlatform.GITHUB.getKey();
    public static final String PLATFORM_SPOTIFY = SocialPlatform.SPOTIFY.getKey();
    public static final String PLATFORM_CUSTOM = SocialPlatform.CUSTOM.getKey();

    public static String detectPlatform(String url) {
        return SocialPlatform.detectFromUrl(url).getKey();
    }

    public static int getIconRes(String platformKey) {
        return SocialPlatform.fromKey(platformKey).getIconRes();
    }

    public static String getDisplayName(String platformKey) {
        return SocialPlatform.fromKey(platformKey).getDisplayName();
    }

    public static String suggestTitle(String platformKey, String url) {
        return getDisplayName(platformKey);
    }
}

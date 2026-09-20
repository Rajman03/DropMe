package com.example.testapp.core;

public enum SocialPlatform {
    INSTAGRAM("instagram", "Instagram", R.drawable.ic_instagram, new String[]{"instagram.com", "instagr.am"}, Category.SOCIAL, "np. @twoj_nick lub twoj_nick", "https://instagram.com/"),
    TIKTOK("tiktok", "TikTok", R.drawable.ic_tiktok, new String[]{"tiktok.com"}, Category.SOCIAL, "np. @twoj_nick lub twoj_nick", "https://www.tiktok.com/@"),
    SNAPCHAT("snapchat", "Snapchat", R.drawable.ic_snapchat, new String[]{"snapchat.com"}, Category.SOCIAL, "np. twoj_nick lub @twoj_nick", "https://snapchat.com/add/"),
    FACEBOOK("facebook", "Facebook", R.drawable.ic_facebook, new String[]{"facebook.com", "fb.com", "fb.me"}, Category.SOCIAL, "np. twoj_nick lub link", "https://www.facebook.com/"),
    YOUTUBE("youtube", "YouTube", R.drawable.ic_youtube, new String[]{"youtube.com", "youtu.be"}, Category.SOCIAL, "np. @twoj_kanal lub link", "https://www.youtube.com/@"),
    LINKEDIN("linkedin", "LinkedIn", R.drawable.ic_linkedin, new String[]{"linkedin.com"}, Category.BIZ, "np. twoj-profil lub link", "https://www.linkedin.com/in/"),
    TWITTER("twitter", "X (Twitter)", R.drawable.ic_twitter_x, new String[]{"twitter.com", "x.com"}, Category.SOCIAL, "np. @twoj_nick", "https://x.com/"),
    DISCORD("discord", "Discord", R.drawable.ic_discord, new String[]{"discord.gg", "discord.com"}, Category.SOCIAL, "np. twoj_tag#0000 lub link", "https://discord.com/users/"),
    GITHUB("github", "GitHub", R.drawable.ic_github, new String[]{"github.com"}, Category.BIZ, "np. twoj_user", "https://github.com/"),
    SPOTIFY("spotify", "Spotify", R.drawable.ic_spotify, new String[]{"spotify.com"}, Category.SOCIAL, "np. link do profilu / playlisty", "https://open.spotify.com/user/"),
    CUSTOM("custom", "Strona WWW", R.drawable.ic_globe, new String[]{}, Category.BIZ, "np. twojadomena.pl", "https://");

    public enum Category { ALL, SOCIAL, BIZ, FAV }

    private final String key;
    private final String displayName;
    private final int iconRes;
    private final String[] domains;
    private final Category category;
    private final String inputHint;
    private final String basePrefix;

    SocialPlatform(String key, String displayName, int iconRes, String[] domains, Category category, String inputHint, String basePrefix) {
        this.key = key;
        this.displayName = displayName;
        this.iconRes = iconRes;
        this.domains = domains;
        this.category = category;
        this.inputHint = inputHint;
        this.basePrefix = basePrefix;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Category getCategory() {
        return category;
    }

    public String getInputHint() {
        return inputHint;
    }

    public String getBasePrefix() {
        return basePrefix;
    }

    public static SocialPlatform fromKey(String key) {
        if (key == null) return CUSTOM;
        String normalized = key.toLowerCase().trim();
        for (SocialPlatform p : values()) {
            if (p.key.equals(normalized)) {
                return p;
            }
        }
        // Legacy emoji fallbacks
        switch (key) {
            case "📸": return INSTAGRAM;
            case "🎵": return TIKTOK;
            case "👻": return SNAPCHAT;
            case "💼": return LINKEDIN;
            case "🎥": return YOUTUBE;
            case "💬":
            case "🎮": return DISCORD;
            case "💻": return GITHUB;
            default: return CUSTOM;
        }
    }

    public static SocialPlatform detectFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return CUSTOM;
        }
        String lower = url.toLowerCase().trim();
        for (SocialPlatform p : values()) {
            for (String domain : p.domains) {
                if (lower.contains(domain)) {
                    return p;
                }
            }
        }
        return CUSTOM;
    }

    /**
     * Converts a raw username (e.g. "@rajman" or "rajman") or full URL
     * into a valid clickable URL for the platform.
     */
    public String buildUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        // Clean user handle (strip leading @)
        String handle = trimmed;
        if (handle.startsWith("@")) {
            handle = handle.substring(1).trim();
        }

        switch (this) {
            case INSTAGRAM:
                return "https://instagram.com/" + handle;
            case TIKTOK:
                return "https://www.tiktok.com/@" + handle;
            case SNAPCHAT:
                return "https://snapchat.com/add/" + handle;
            case FACEBOOK:
                return "https://www.facebook.com/" + handle;
            case YOUTUBE:
                return "https://www.youtube.com/@" + handle;
            case LINKEDIN:
                if (handle.startsWith("in/")) {
                    return "https://www.linkedin.com/" + handle;
                }
                return "https://www.linkedin.com/in/" + handle;
            case TWITTER:
                return "https://x.com/" + handle;
            case DISCORD:
                if (handle.contains("discord.gg/") || handle.contains("discord.com/")) {
                    return "https://" + handle;
                }
                return "https://discord.com/users/" + handle;
            case GITHUB:
                return "https://github.com/" + handle;
            case SPOTIFY:
                return "https://open.spotify.com/user/" + handle;
            case CUSTOM:
            default:
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    return "https://" + trimmed;
                }
                return trimmed;
        }
    }

    /**
     * Deep-link scheme if installed on device
     */
    public String getDeepLink(String input) {
        String clean = input != null && input.startsWith("@") ? input.substring(1) : input;
        switch (this) {
            case INSTAGRAM:
                return "instagram://user?username=" + clean;
            case SNAPCHAT:
                return "snapchat://add/" + clean;
            case TWITTER:
                return "twitter://user?screen_name=" + clean;
            default:
                return buildUrl(input);
        }
    }
}

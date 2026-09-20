package com.example.testapp.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MultiLinkProfile {

    private static final String PREFS_NAME = "multilink_profile_prefs";
    private static final String KEY_DATA = "multilink_json";
    private static final String KEY_WEB_URL = "multilink_last_web_url";

    private String name = "";
    private String bio = "Moja wizytówka kontaktowa";
    private String instagram = "";
    private String tiktok = "";
    private String snapchat = "";
    private String discord = "";
    private String twitter = "";
    private String linkedin = "";
    private String phone = "";
    private String website = "";
    private String themeColor = "#0F172A";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MultiLinkProfile() {}

    public MultiLinkProfile(Context context) {
        if (context != null) {
            MultiLinkProfile loaded = load(context);
            this.name = loaded.name;
            this.bio = loaded.bio;
            this.instagram = loaded.instagram;
            this.tiktok = loaded.tiktok;
            this.snapchat = loaded.snapchat;
            this.discord = loaded.discord;
            this.twitter = loaded.twitter;
            this.linkedin = loaded.linkedin;
            this.phone = loaded.phone;
            this.website = loaded.website;
            this.themeColor = loaded.themeColor;
        }
    }

    public void syncWithUserProfile(UserProfile profile) {
        if (profile == null) return;
        if (this.name == null || this.name.isEmpty()) {
            this.name = profile.getName();
        }
    }

    public static MultiLinkProfile load(Context context) {
        MultiLinkProfile p = new MultiLinkProfile();
        if (context == null) return p;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_DATA, null);
        if (jsonStr != null) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                p.name = obj.optString("name", "");
                p.bio = obj.optString("bio", "Moja wizytówka kontaktowa");
                p.instagram = obj.optString("instagram", "");
                p.tiktok = obj.optString("tiktok", "");
                p.snapchat = obj.optString("snapchat", "");
                p.discord = obj.optString("discord", "");
                p.twitter = obj.optString("twitter", "");
                p.linkedin = obj.optString("linkedin", "");
                p.phone = obj.optString("phone", "");
                p.website = obj.optString("website", "");
                p.themeColor = obj.optString("themeColor", "#0F172A");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return p;
    }

    public void save(Context context) {
        if (context == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("name", name != null ? name : "");
            obj.put("bio", bio != null ? bio : "");
            obj.put("instagram", instagram != null ? instagram : "");
            obj.put("tiktok", tiktok != null ? tiktok : "");
            obj.put("snapchat", snapchat != null ? snapchat : "");
            obj.put("discord", discord != null ? discord : "");
            obj.put("twitter", twitter != null ? twitter : "");
            obj.put("linkedin", linkedin != null ? linkedin : "");
            obj.put("phone", phone != null ? phone : "");
            obj.put("website", website != null ? website : "");
            obj.put("themeColor", themeColor != null ? themeColor : "#0F172A");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_DATA, obj.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLastWebUrl(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WEB_URL, null);
    }

    public void setLastWebUrl(Context context, String url) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEB_URL, url).apply();
    }

    public boolean hasAnyLink() {
        return (instagram != null && !instagram.trim().isEmpty())
                || (tiktok != null && !tiktok.trim().isEmpty())
                || (snapchat != null && !snapchat.trim().isEmpty())
                || (discord != null && !discord.trim().isEmpty())
                || (twitter != null && !twitter.trim().isEmpty())
                || (linkedin != null && !linkedin.trim().isEmpty())
                || (website != null && !website.trim().isEmpty())
                || (phone != null && !phone.trim().isEmpty());
    }

    /**
     * Standard RFC vCard 3.0 that every phone camera parses instantly into contacts
     */
    public String buildVCard() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");
        String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Wizytówka";
        sb.append("FN:").append(displayName).append("\n");
        sb.append("N:;").append(displayName).append(";;;\n");
        if (bio != null && !bio.trim().isEmpty()) {
            sb.append("TITLE:").append(bio.trim()).append("\n");
        }
        if (phone != null && !phone.trim().isEmpty()) {
            sb.append("TEL;TYPE=CELL:").append(phone.trim()).append("\n");
        }
        if (instagram != null && !instagram.trim().isEmpty()) {
            sb.append("X-SOCIALPROFILE;type=instagram:").append(SocialPlatform.INSTAGRAM.buildUrl(instagram)).append("\n");
        }
        if (tiktok != null && !tiktok.trim().isEmpty()) {
            sb.append("X-SOCIALPROFILE;type=tiktok:").append(SocialPlatform.TIKTOK.buildUrl(tiktok)).append("\n");
        }
        if (snapchat != null && !snapchat.trim().isEmpty()) {
            sb.append("X-SOCIALPROFILE;type=snapchat:").append(SocialPlatform.SNAPCHAT.buildUrl(snapchat)).append("\n");
        }
        if (twitter != null && !twitter.trim().isEmpty()) {
            sb.append("X-SOCIALPROFILE;type=twitter:").append(SocialPlatform.TWITTER.buildUrl(twitter)).append("\n");
        }
        if (linkedin != null && !linkedin.trim().isEmpty()) {
            sb.append("URL:").append(SocialPlatform.LINKEDIN.buildUrl(linkedin)).append("\n");
        } else if (website != null && !website.trim().isEmpty()) {
            sb.append("URL:").append(SocialPlatform.CUSTOM.buildUrl(website)).append("\n");
        }
        sb.append("END:VCARD");
        return sb.toString();
    }

    public interface WebCardCallback {
        void onSuccess(String publicWebUrl);
        void onError(String error);
    }

    /**
     * Creates or publishes an online micro-card via open web API so any phone browser can open it.
     */
    public void publishWebCard(Context context, WebCardCallback callback) {
        final String accessToken = BuildConfig.TELEGRAPH_TOKEN;
        if (accessToken == null || accessToken.trim().isEmpty()) {
            // Brak tokenu (nie ma go w local.properties ani w zmiennych srodowiskowych).
            // Nie blokujemy reszty aplikacji - tylko ta jedna funkcja jest wylaczona.
            if (callback != null) {
                callback.onError("Publikacja karty online jest wylaczona (brak tokenu DROPME_TELEGRAPH_TOKEN)");
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Prepare rich Telegraph article JSON
                JSONObject payload = new JSONObject();
                payload.put("access_token", accessToken.trim());
                String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Wizytówka Cyfrowa";
                payload.put("title", displayName + " – Wizytówka Cyfrowa");
                payload.put("author_name", "DropMe • Zweryfikowany Profil");
                payload.put("author_url", "https://t.me");
                payload.put("return_content", false);

                JSONArray content = new JSONArray();

                // 1. Avatar figure at the top
                try {
                    String seed = java.net.URLEncoder.encode(displayName, "UTF-8");
                    String avatarUrl = "https://api.dicebear.com/7.x/initials/png?seed=" + seed + "&backgroundColor=0f172a&textColor=ffffff&size=240";
                    JSONObject fig = new JSONObject();
                    fig.put("tag", "figure");
                    JSONArray figChildren = new JSONArray();
                    JSONObject img = new JSONObject();
                    img.put("tag", "img");
                    JSONObject imgAttrs = new JSONObject();
                    imgAttrs.put("src", avatarUrl);
                    img.put("attrs", imgAttrs);
                    figChildren.put(img);
                    fig.put("children", figChildren);
                    content.put(fig);
                } catch (Exception ignored) {}

                // 2. Greeting
                JSONObject h3 = new JSONObject();
                h3.put("tag", "h3");
                JSONArray h3Children = new JSONArray();
                h3Children.put("👋 " + displayName);
                h3.put("children", h3Children);
                content.put(h3);

                // 3. Bio / description in stylized blockquote
                JSONObject quoteBio = new JSONObject();
                quoteBio.put("tag", "blockquote");
                JSONArray quoteChildren = new JSONArray();
                JSONObject boldBio = new JSONObject();
                boldBio.put("tag", "b");
                JSONArray boldBioChildren = new JSONArray();
                boldBioChildren.put((bio != null && !bio.trim().isEmpty()) ? bio.trim() : "Moja oficjalna wizytówka kontaktowa");
                boldBio.put("children", boldBioChildren);
                quoteChildren.put(boldBio);
                quoteChildren.put(new JSONObject().put("tag", "br"));
                quoteChildren.put("✨ Oficjalna lista profili społecznościowych i linków kontaktowych:");
                quoteBio.put("children", quoteChildren);
                content.put(quoteBio);

                // 4. Divider
                content.put(new JSONObject().put("tag", "hr"));

                // 5. Section Header
                JSONObject h4 = new JSONObject();
                h4.put("tag", "h4");
                JSONArray h4Children = new JSONArray();
                h4Children.put("🔗 Profile i Social Media:");
                h4.put("children", h4Children);
                content.put(h4);

                // 6. Styled platform cards
                addLinkCard(content, "🎵 TIKTOK", cleanHandle(tiktok), SocialPlatform.TIKTOK.buildUrl(tiktok), "Otwórz profil na TikTok");
                addLinkCard(content, "📸 INSTAGRAM", cleanHandle(instagram), SocialPlatform.INSTAGRAM.buildUrl(instagram), "Otwórz profil na Instagramie");
                addLinkCard(content, "👻 SNAPCHAT", cleanHandle(snapchat), SocialPlatform.SNAPCHAT.buildUrl(snapchat), "Dodaj profil na Snapchacie");
                addLinkCard(content, "💬 DISCORD", discord != null ? discord.trim() : "", SocialPlatform.DISCORD.buildUrl(discord), "Dołącz na Discord");
                addLinkCard(content, "🌐 X (TWITTER)", cleanHandle(twitter), SocialPlatform.TWITTER.buildUrl(twitter), "Odwiedź profil na X");
                addLinkCard(content, "💼 LINKEDIN", linkedin != null ? linkedin.trim() : "", SocialPlatform.LINKEDIN.buildUrl(linkedin), "Zobacz profil LinkedIn");
                addLinkCard(content, "🌍 STRONA WWW", website != null ? website.trim() : "", SocialPlatform.CUSTOM.buildUrl(website), "Przejdź na stronę WWW");

                if (phone != null && !phone.trim().isEmpty()) {
                    addLinkCard(content, "📞 TELEFON", phone.trim(), "tel:" + phone.trim().replaceAll("\\s+", ""), "Zadzwoń bezpośrednio");
                }

                // 7. Footer
                content.put(new JSONObject().put("tag", "hr"));
                JSONObject footerP = new JSONObject();
                footerP.put("tag", "p");
                JSONArray footerChildren = new JSONArray();
                JSONObject footerI = new JSONObject();
                footerI.put("tag", "i");
                JSONArray fiText = new JSONArray();
                fiText.put("📱 Wygenerowano za pomocą aplikacji DropMe • Wszystkie Twoje linki w 1 kodzie");
                footerI.put("children", fiText);
                footerChildren.put(footerI);
                footerP.put("children", footerChildren);
                content.put(footerP);

                payload.put("content", content);

                URL url = new URL("https://api.telegra.ph/createPage");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setDoOutput(true);

                byte[] bytes = payload.toString().getBytes("UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    byte[] buf = new byte[4096];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while ((len = in.read(buf)) != -1) {
                        sb.append(new String(buf, 0, len, "UTF-8"));
                    }
                    in.close();

                    JSONObject res = new JSONObject(sb.toString());
                    if (res.optBoolean("ok", false)) {
                        String pageUrl = res.getJSONObject("result").getString("url");
                        setLastWebUrl(context, pageUrl);
                        if (callback != null) callback.onSuccess(pageUrl);
                        return;
                    }
                }
                throw new Exception("Blad publikacji");
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    private void addLinkCard(JSONArray content, String platformName, String handle, String url, String ctaText) throws Exception {
        if (url == null || url.trim().isEmpty() || url.equals("https://") || url.equals("https://instagram.com/")) return;

        JSONObject card = new JSONObject();
        card.put("tag", "blockquote");
        JSONArray children = new JSONArray();

        // Line 1: Bold platform name
        JSONObject boldHeader = new JSONObject();
        boldHeader.put("tag", "b");
        JSONArray bhText = new JSONArray();
        bhText.put(platformName);
        boldHeader.put("children", bhText);
        children.put(boldHeader);

        children.put(new JSONObject().put("tag", "br"));

        // Line 2: Account handle
        if (handle != null && !handle.trim().isEmpty()) {
            String dispHandle = handle.startsWith("@") ? handle : "@" + handle;
            children.put("👤 Konto: " + dispHandle);
            children.put(new JSONObject().put("tag", "br"));
        }

        // Line 3: Big clickable button/link
        children.put("👉 ");
        JSONObject a = new JSONObject();
        a.put("tag", "a");
        JSONObject attrs = new JSONObject();
        attrs.put("href", url);
        a.put("attrs", attrs);
        JSONArray aText = new JSONArray();
        aText.put(ctaText + " ↗");
        a.put("children", aText);
        children.put(a);

        card.put("children", children);
        content.put(card);
    }

    private String cleanHandle(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.startsWith("@") ? t.substring(1) : t;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
    public String getTiktok() { return tiktok; }
    public void setTiktok(String tiktok) { this.tiktok = tiktok; }
    public String getSnapchat() { return snapchat; }
    public void setSnapchat(String snapchat) { this.snapchat = snapchat; }
    public String getDiscord() { return discord; }
    public void setDiscord(String discord) { this.discord = discord; }
    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
}

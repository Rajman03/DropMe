package com.example.testapp.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class PayPalConfig {

    /**
     * Adres konta PayPal wpisany na stałe w kodzie aplikacji.
     * Może to być:
     * - nick PayPal.me (np. "twoj_nick" lub "https://paypal.me/twoj_nick")
     * - adres e-mail (np. "kontakt@domena.pl")
     */
    public static final String PAYPAL_RECEIVER = "paypal.me/Rajman04";

    public static String getReceiver() {
        return PAYPAL_RECEIVER;
    }

    public static String getFormattedReceiverDisplay() {
        String receiver = getReceiver();
        if (receiver == null || receiver.isEmpty()) return "";
        if (receiver.contains("@")) {
            return receiver;
        }
        String handle = cleanHandle(receiver);
        return "paypal.me/" + handle;
    }

    private static String cleanHandle(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();
        if (cleaned.startsWith("http://")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("https://")) cleaned = cleaned.substring(8);
        if (cleaned.startsWith("www.")) cleaned = cleaned.substring(4);
        if (cleaned.startsWith("paypal.me/")) cleaned = cleaned.substring(10);
        if (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        return cleaned;
    }

    public static String buildCheckoutUrl(String planTitle, double amount) {
        String receiver = getReceiver();
        String formattedAmount = String.format(Locale.US, "%.2f", amount);

        if (receiver.contains("@")) {
            // PayPal standard direct checkout / webscr
            try {
                return "https://www.paypal.com/cgi-bin/webscr?"
                        + "cmd=_xclick"
                        + "&business=" + URLEncoder.encode(receiver, "UTF-8")
                        + "&item_name=" + URLEncoder.encode("Wizytowka QR - " + planTitle, "UTF-8")
                        + "&amount=" + formattedAmount
                        + "&currency_code=PLN"
                        + "&no_shipping=1";
            } catch (UnsupportedEncodingException e) {
                return "https://www.paypal.com";
            }
        } else {
            // PayPal.me checkout link
            String handle = cleanHandle(receiver);
            return "https://paypal.me/" + handle + "/" + formattedAmount + "PLN";
        }
    }
}

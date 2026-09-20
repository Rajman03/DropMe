package com.example.testapp.data;

import com.example.testapp.core.AuthUser;
import com.example.testapp.core.PendingPurchase;
import com.example.testapp.core.UserProfile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class SupabaseClient {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private final SupabaseConfig config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static boolean lastSyncSuccess = false;
    private static String lastSyncErrorMessage = null;

    public static boolean isLastSyncSuccess() {
        return lastSyncSuccess;
    }

    public static String getLastSyncErrorMessage() {
        return lastSyncErrorMessage;
    }

    public SupabaseClient(Context context) {
        this.config = new SupabaseConfig(context);
    }

    public static String parseErrorMessage(int code, String rawDetails) {
        if (code >= 200 && code < 300) return null;
        if (code == 400) {
            if (rawDetails != null && rawDetails.contains("PGRST204")) {
                return "Niezgodność struktury bazy Supabase (brakująca kolumna)";
            }
            return "Nieprawidłowe zapytanie do bazy (400)";
        }
        if (code == 401 || code == 403) {
            return "Brak uprawnień do zapisu w Supabase (blokada RLS)";
        }
        if (code == 404) {
            return "Nie odnaleziono tabeli w bazie Supabase (404)";
        }
        if (code == 409) {
            return "Użytkownik o takim loginie już istnieje";
        }
        if (code >= 500) {
            return "Błąd serwera Supabase (" + code + ")";
        }
        return "Błąd bazy danych (" + code + ")";
    }

    public void syncUser(AuthUser user, Callback<Boolean> callback) {
        if (user == null) return;

        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?on_conflict=username";
                JSONObject body = new JSONObject();
                body.put("username", user.getUsername());
                body.put("display_name", user.getDisplayName());
                body.put("password", user.getPassword());
                body.put("rank", user.getRank());
                body.put("title", user.getTitle());
                body.put("avatar", user.getAvatar());
                body.put("purchased_premium", user.isPurchasedPremium());
                body.put("pin", user.getPin());

                HttpURLConnection conn = openConnection(endpoint, "POST");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
                writeJson(conn, body.toString());

                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                String errDetails = "";
                if (!success) {
                    try {
                        errDetails = readStream(conn.getErrorStream());
                    } catch (Exception ignored) {}
                }

                final boolean finalSuccess = success;
                final String finalErr = parseErrorMessage(code, errDetails);
                if (finalSuccess) {
                    lastSyncSuccess = true;
                    lastSyncErrorMessage = null;
                } else {
                    lastSyncSuccess = false;
                    lastSyncErrorMessage = finalErr;
                }
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (finalSuccess) callback.onSuccess(true);
                        else callback.onError(finalErr);
                    }
                });
            } catch (Exception e) {
                lastSyncSuccess = false;
                lastSyncErrorMessage = "Brak połączenia";
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(lastSyncErrorMessage);
                });
            }
        });
    }

    public void registerUser(AuthUser user, Callback<AuthUser> callback) {
        if (user == null) return;
        executor.execute(() -> {
            try {
                String insertEndpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?on_conflict=username";
                JSONObject body = new JSONObject();
                body.put("username", user.getUsername());
                body.put("display_name", user.getDisplayName());
                body.put("password", user.getPassword());
                body.put("rank", user.getRank());
                body.put("title", user.getTitle());
                body.put("avatar", user.getAvatar());
                body.put("purchased_premium", user.isPurchasedPremium());
                body.put("pin", user.getPin());

                HttpURLConnection conn = openConnection(insertEndpoint, "POST");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation");
                writeJson(conn, body.toString());

                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                String errDetails = "";
                if (!success) {
                    try {
                        errDetails = readStream(conn.getErrorStream());
                    } catch (Exception ignored) {}
                }

                final boolean finalSuccess = success;
                final String finalErr = parseErrorMessage(code, errDetails);
                if (finalSuccess) {
                    lastSyncSuccess = true;
                    lastSyncErrorMessage = null;
                } else {
                    lastSyncSuccess = false;
                    lastSyncErrorMessage = finalErr;
                }

                mainHandler.post(() -> {
                    if (callback != null) {
                        if (finalSuccess) callback.onSuccess(user);
                        else callback.onError(finalErr);
                    }
                });
            } catch (Exception e) {
                lastSyncSuccess = false;
                lastSyncErrorMessage = "Brak połączenia";
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(lastSyncErrorMessage);
                });
            }
        });
    }

    public void loginUser(String username, String rawPassword, Callback<AuthUser> callback) {
        if (username == null || rawPassword == null) {
            if (callback != null) callback.onError("Wpisz login i hasło");
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username.trim().toLowerCase() + "&select=*";
                HttpURLConnection conn = openConnection(endpoint, "GET");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String response = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(response);
                    if (arr.length() == 0) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError("Nie znaleziono użytkownika @" + username + " w bazie Supabase.");
                        });
                        return;
                    }

                    JSONObject obj = arr.getJSONObject(0);
                    String storedPass = obj.optString("password", "");
                    String pin = obj.optString("pin", "");
                    AuthUser user = new AuthUser(
                            obj.optString("username", ""),
                            obj.optString("display_name", "Użytkownik"),
                            storedPass,
                            obj.optString("rank", UserProfile.RANK_GUEST),
                            obj.optString("title", "Użytkownik"),
                            obj.optString("avatar", "👤"),
                            parseBooleanField(obj, "purchased_premium", "purchasedPremium"),
                            pin
                    );

                    // Weryfikacja hasła
                    if (!user.verifyPassword(rawPassword)) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError("Błędny login lub hasło!");
                        });
                        return;
                    }

                    lastSyncSuccess = true;
                    lastSyncErrorMessage = null;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(user);
                    });
                } else {
                    String errDetails = "";
                    try {
                        errDetails = readStream(conn.getErrorStream());
                    } catch (Exception ignored) {}
                    final String err = parseErrorMessage(code, errDetails);
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError(err != null ? err : "Błąd bazy danych (" + code + ")");
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Błąd połączenia: " + e.getMessage());
                });
            }
        });
    }

    public void updateUserRank(String username, String newRank, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username;
                JSONObject body = new JSONObject();
                body.put("rank", newRank);

                HttpURLConnection conn = openConnection(endpoint, "PATCH");
                writeJson(conn, body.toString());

                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Update error code: " + code);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void deleteUser(String username, Callback<Boolean> callback) {
        if (username == null || username.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid username");
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username.trim().toLowerCase();
                HttpURLConnection conn = openConnection(endpoint, "DELETE");
                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Delete error code: " + code);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void recordPurchase(String username, String plan, double amount, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_PURCHASES);
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("plan", plan);
                body.put("amount", amount);
                body.put("timestamp", System.currentTimeMillis());

                HttpURLConnection conn = openConnection(endpoint, "POST");
                writeJson(conn, body.toString());

                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);

                // Also update user profile in Supabase to mark purchased_premium = true and rank = Premium if not admin
                String userEndpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username;
                JSONObject updateBody = new JSONObject();
                updateBody.put("purchased_premium", true);
                HttpURLConnection updateConn = openConnection(userEndpoint, "PATCH");
                writeJson(updateConn, updateBody.toString());
                updateConn.getResponseCode();

                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Purchase error code: " + code);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void submitPendingPurchase(String username, String plan, double amount, String proof, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_PURCHASES);
                JSONObject body = new JSONObject();
                body.put("username", username);
                body.put("plan", "pending:" + plan + ":" + (proof != null ? proof.trim() : ""));
                body.put("amount", amount);
                body.put("timestamp", System.currentTimeMillis());

                HttpURLConnection conn = openConnection(endpoint, "POST");
                writeJson(conn, body.toString());

                int code = conn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Submit error: " + code);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void fetchPendingPurchases(Callback<List<PendingPurchase>> callback) {
        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_PURCHASES) + "?plan=like.pending:*&select=*&order=id.desc";
                HttpURLConnection conn = openConnection(endpoint, "GET");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String response = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(response);
                    List<PendingPurchase> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        PendingPurchase pp = PendingPurchase.fromJsonObject(obj);
                        if (pp != null) list.add(pp);
                    }
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(list);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("Fetch pending error: " + code);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void approvePendingPurchase(int purchaseId, String username, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // 1. Mark purchase as approved in app_purchases
                String purchaseEndpoint = config.getRestEndpoint(SupabaseConfig.TABLE_PURCHASES) + "?id=eq." + purchaseId;
                JSONObject pBody = new JSONObject();
                pBody.put("plan", "approved");
                HttpURLConnection pConn = openConnection(purchaseEndpoint, "PATCH");
                writeJson(pConn, pBody.toString());
                pConn.getResponseCode();

                // 2. Grant Premium in app_users
                String userEndpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username.trim().toLowerCase();
                JSONObject uBody = new JSONObject();
                uBody.put("purchased_premium", true);
                uBody.put("rank", UserProfile.RANK_PREMIUM);
                HttpURLConnection uConn = openConnection(userEndpoint, "PATCH");
                writeJson(uConn, uBody.toString());
                int uCode = uConn.getResponseCode();
                boolean success = (uCode >= 200 && uCode < 300);

                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Approve error: " + uCode);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void rejectPendingPurchase(int purchaseId, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String purchaseEndpoint = config.getRestEndpoint(SupabaseConfig.TABLE_PURCHASES) + "?id=eq." + purchaseId;
                HttpURLConnection pConn = openConnection(purchaseEndpoint, "DELETE");
                int code = pConn.getResponseCode();
                boolean success = (code >= 200 && code < 300);
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (success) callback.onSuccess(true);
                        else callback.onError("Reject error: " + code);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public void fetchUser(String username, Callback<AuthUser> callback) {
        if (username == null || username.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(null);
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?username=eq." + username.trim().toLowerCase() + "&select=*";
                HttpURLConnection conn = openConnection(endpoint, "GET");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String response = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(response);
                    AuthUser user = null;
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        user = new AuthUser(
                                obj.optString("username", ""),
                                obj.optString("display_name", "Użytkownik"),
                                obj.optString("password", ""),
                                obj.optString("rank", UserProfile.RANK_GUEST),
                                obj.optString("title", "Użytkownik"),
                                obj.optString("avatar", "👤"),
                                parseBooleanField(obj, "purchased_premium", "purchasedPremium"),
                                obj.optString("pin", "")
                        );
                    }
                    final AuthUser finalUser = user;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(finalUser);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError("Fetch user code: " + code);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        });
    }

    public static boolean parseBooleanField(JSONObject obj, String... keys) {
        if (obj == null) return false;
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                Object val = obj.opt(key);
                if (val instanceof Boolean) return (Boolean) val;
                if (val instanceof Number) return ((Number) val).intValue() != 0;
                if (val instanceof String) {
                    String s = ((String) val).trim().toLowerCase();
                    return "true".equals(s) || "1".equals(s) || "t".equals(s) || "yes".equals(s);
                }
            }
        }
        return false;
    }

    public void fetchUsers(Callback<List<AuthUser>> callback) {
        executor.execute(() -> {
            try {
                String endpoint = config.getRestEndpoint(SupabaseConfig.TABLE_USERS) + "?select=*";
                HttpURLConnection conn = openConnection(endpoint, "GET");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    String response = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(response);
                    List<AuthUser> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        AuthUser user = new AuthUser(
                                obj.optString("username", ""),
                                obj.optString("display_name", "Użytkownik"),
                                obj.optString("password", ""),
                                obj.optString("rank", UserProfile.RANK_GUEST),
                                obj.optString("title", "Użytkownik"),
                                obj.optString("avatar", "👤"),
                                parseBooleanField(obj, "purchased_premium", "purchasedPremium"),
                                obj.optString("pin", "")
                        );
                        list.add(user);
                    }
                    lastSyncSuccess = true;
                    lastSyncErrorMessage = null;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(list);
                    });
                } else {
                    lastSyncSuccess = false;
                    String errDetails = "";
                    try {
                        errDetails = readStream(conn.getErrorStream());
                    } catch (Exception ignored) {}
                    lastSyncErrorMessage = parseErrorMessage(code, errDetails);
                    final String fErr = lastSyncErrorMessage;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError(fErr);
                    });
                }
            } catch (Exception e) {
                lastSyncSuccess = false;
                lastSyncErrorMessage = "Brak połączenia";
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(lastSyncErrorMessage);
                });
            }
        });
    }

    private void setRequestMethodSafe(HttpURLConnection conn, String method) throws Exception {
        try {
            conn.setRequestMethod(method);
        } catch (java.net.ProtocolException e) {
            if ("PATCH".equalsIgnoreCase(method)) {
                try {
                    java.lang.reflect.Field methodField = HttpURLConnection.class.getDeclaredField("method");
                    methodField.setAccessible(true);
                    methodField.set(conn, "PATCH");
                } catch (Exception ex) {
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    private HttpURLConnection openConnection(String endpoint, String method) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setRequestMethodSafe(conn, method);
        conn.setRequestProperty("apikey", config.getAnonKey());
        conn.setRequestProperty("Authorization", "Bearer " + config.getAnonKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        return conn;
    }

    private void writeJson(HttpURLConnection conn, String json) throws Exception {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes("UTF-8"));
            os.flush();
        }
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}

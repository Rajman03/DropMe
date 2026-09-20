package com.example.testapp.core;

import org.json.JSONObject;

public class PendingPurchase {
    private final int id;
    private final String username;
    private final String plan;
    private final double amount;
    private final String proof;
    private final long timestamp;

    public PendingPurchase(int id, String username, String plan, double amount, String proof, long timestamp) {
        this.id = id;
        this.username = username;
        this.plan = plan;
        this.amount = amount;
        this.proof = proof;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPlan() {
        return plan;
    }

    public double getAmount() {
        return amount;
    }

    public String getProof() {
        return proof;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static PendingPurchase fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        int id = obj.optInt("id", 0);
        String user = obj.optString("username", "");
        String rawPlan = obj.optString("plan", "");
        double amount = obj.optDouble("amount", 0.0);
        long ts = obj.optLong("timestamp", 0);

        // plan format: "pending:<plan_type>:<proof>"
        String proof = "";
        String cleanPlan = rawPlan;
        if (rawPlan.startsWith("pending:")) {
            String[] parts = rawPlan.split(":", 3);
            if (parts.length > 1) cleanPlan = parts[1];
            if (parts.length > 2) proof = parts[2];
        }

        return new PendingPurchase(id, user, cleanPlan, amount, proof, ts);
    }
}

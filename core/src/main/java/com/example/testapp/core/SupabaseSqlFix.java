package com.example.testapp.core;

/**
 * Jedno źródło prawdy dla skryptu SQL naprawiającego tabele Supabase.
 *
 * <p>Wcześniej ten sam, ponad 30-liniowy skrypt był wklejony w dwóch kontrolerach
 * ({@code feature-auth} i {@code feature-profile}). Teraz jest w jednym miejscu.</p>
 */
public final class SupabaseSqlFix {

    private SupabaseSqlFix() {
        // klasa narzędziowa - bez instancji
    }

    public static final String SQL =
            "DROP TABLE IF EXISTS public.profiles CASCADE;\n"
          + "CREATE TABLE public.profiles (\n"
          + "    username TEXT PRIMARY KEY,\n"
          + "    email TEXT DEFAULT '',\n"
          + "    password TEXT DEFAULT '',\n"
          + "    display_name TEXT NOT NULL DEFAULT 'User',\n"
          + "    rank TEXT NOT NULL DEFAULT 'Guest',\n"
          + "    title TEXT NOT NULL DEFAULT 'DropMe Profile',\n"
          + "    avatar TEXT NOT NULL DEFAULT '',\n"
          + "    purchased_premium BOOLEAN NOT NULL DEFAULT false,\n"
          + "    pin TEXT DEFAULT '',\n"
          + "    created_at TIMESTAMPTZ NOT NULL DEFAULT now()\n"
          + ");\n"
          + "ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;\n"
          + "CREATE POLICY \"Allow all for profiles\" ON public.profiles FOR ALL USING (true) WITH CHECK (true);\n"
          + "GRANT ALL ON public.profiles TO anon, authenticated, service_role;\n"
          + "DROP TABLE IF EXISTS public.app_purchases CASCADE;\n"
          + "CREATE TABLE public.app_purchases (\n"
          + "    id BIGSERIAL PRIMARY KEY,\n"
          + "    username TEXT NOT NULL,\n"
          + "    plan TEXT NOT NULL,\n"
          + "    amount NUMERIC DEFAULT 0,\n"
          + "    timestamp BIGINT,\n"
          + "    created_at TIMESTAMPTZ DEFAULT now()\n"
          + ");\n"
          + "ALTER TABLE public.app_purchases ENABLE ROW LEVEL SECURITY;\n"
          + "CREATE POLICY \"Allow all for app_purchases\" ON public.app_purchases FOR ALL USING (true) WITH CHECK (true);\n"
          + "GRANT ALL ON public.app_purchases TO anon, authenticated, service_role;\n"
          + "DROP TABLE IF EXISTS public.user_qr_codes CASCADE;\n"
          + "CREATE TABLE public.user_qr_codes (\n"
          + "    id BIGINT PRIMARY KEY,\n"
          + "    username TEXT NOT NULL,\n"
          + "    name TEXT NOT NULL,\n"
          + "    icon TEXT DEFAULT '',\n"
          + "    url TEXT NOT NULL,\n"
          + "    date TEXT DEFAULT '',\n"
          + "    favorite BOOLEAN DEFAULT false,\n"
          + "    qr_color TEXT DEFAULT '',\n"
          + "    created_at TIMESTAMPTZ DEFAULT now()\n"
          + ");\n"
          + "ALTER TABLE public.user_qr_codes ENABLE ROW LEVEL SECURITY;\n"
          + "CREATE POLICY \"Allow all for user_qr_codes\" ON public.user_qr_codes FOR ALL USING (true) WITH CHECK (true);\n"
          + "GRANT ALL ON public.user_qr_codes TO anon, authenticated, service_role;\n"
          + "DROP TABLE IF EXISTS public.verification_codes CASCADE;\n"
          + "NOTIFY pgrst, 'reload schema';\n";
}

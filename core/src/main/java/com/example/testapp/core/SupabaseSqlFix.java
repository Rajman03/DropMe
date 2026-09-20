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

    public static final String SQL = """
            -- DROPME - NAPRAWA TABELI SUPABASE
            DROP TABLE IF EXISTS public.profiles CASCADE;
            CREATE TABLE public.profiles (
                username TEXT PRIMARY KEY,
                password TEXT DEFAULT '',
                display_name TEXT NOT NULL DEFAULT 'Użytkownik',
                rank TEXT NOT NULL DEFAULT 'Guest',
                title TEXT NOT NULL DEFAULT 'Profil wizytówki DropMe',
                avatar TEXT NOT NULL DEFAULT '👤',
                purchased_premium BOOLEAN NOT NULL DEFAULT false,
                pin TEXT DEFAULT '',
                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
            );
            ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
            GRANT ALL ON public.profiles TO anon, authenticated, service_role;
            CREATE TABLE IF NOT EXISTS public.app_purchases (
                id BIGSERIAL PRIMARY KEY,
                username TEXT NOT NULL,
                plan TEXT NOT NULL,
                amount NUMERIC DEFAULT 0,
                timestamp BIGINT,
                created_at TIMESTAMPTZ DEFAULT now()
            );
            ALTER TABLE public.app_purchases DISABLE ROW LEVEL SECURITY;
            GRANT ALL ON public.app_purchases TO anon, authenticated, service_role;
            NOTIFY pgrst, 'reload schema';
            """;
}

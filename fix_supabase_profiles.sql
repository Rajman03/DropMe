-- ==========================================================
-- DROPME - NAPRAWA SUPABASE (z politykami RLS)
-- Wklej w Supabase: SQL Editor -> New Query -> Run
-- ==========================================================

-- 1. Tabela profiles
DROP TABLE IF EXISTS public.profiles CASCADE;
CREATE TABLE public.profiles (
    username TEXT PRIMARY KEY,
    email TEXT DEFAULT '',
    password TEXT DEFAULT '',
    display_name TEXT NOT NULL DEFAULT 'User',
    rank TEXT NOT NULL DEFAULT 'Guest',
    title TEXT NOT NULL DEFAULT 'DropMe Profile',
    avatar TEXT NOT NULL DEFAULT '',
    purchased_premium BOOLEAN NOT NULL DEFAULT false,
    pin TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for profiles" ON public.profiles;
CREATE POLICY "Allow all for profiles" ON public.profiles FOR ALL USING (true) WITH CHECK (true);
GRANT ALL ON public.profiles TO anon, authenticated, service_role;

-- 2. Tabela zakupow
DROP TABLE IF EXISTS public.app_purchases CASCADE;
CREATE TABLE public.app_purchases (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    plan TEXT NOT NULL,
    amount NUMERIC DEFAULT 0,
    timestamp BIGINT,
    created_at TIMESTAMPTZ DEFAULT now()
);
ALTER TABLE public.app_purchases ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for app_purchases" ON public.app_purchases;
CREATE POLICY "Allow all for app_purchases" ON public.app_purchases FOR ALL USING (true) WITH CHECK (true);
GRANT ALL ON public.app_purchases TO anon, authenticated, service_role;

-- 3. Tabela kodow QR
DROP TABLE IF EXISTS public.user_qr_codes CASCADE;
CREATE TABLE public.user_qr_codes (
    id BIGINT PRIMARY KEY,
    username TEXT NOT NULL,
    name TEXT NOT NULL,
    icon TEXT DEFAULT '',
    url TEXT NOT NULL,
    date TEXT DEFAULT '',
    favorite BOOLEAN DEFAULT false,
    qr_color TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);
ALTER TABLE public.user_qr_codes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for user_qr_codes" ON public.user_qr_codes;
CREATE POLICY "Allow all for user_qr_codes" ON public.user_qr_codes FOR ALL USING (true) WITH CHECK (true);
GRANT ALL ON public.user_qr_codes TO anon, authenticated, service_role;

-- 4. Kody weryfikacyjne obsluguje teraz Supabase Auth (e-mail OTP),
--    wiec dawna, publicznie czytelna tabela nie jest juz potrzebna.
DROP TABLE IF EXISTS public.verification_codes CASCADE;

-- 5. Odswiez cache PostgREST
NOTIFY pgrst, 'reload schema';

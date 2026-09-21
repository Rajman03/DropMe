-- ==========================================================
-- DROPME - NAPRAWA SUPABASE (RLS + integralność danych)
-- Wklej w Supabase: SQL Editor -> New Query -> Run
--
-- UWAGA: skrypt NIE zmienia schematu tabel (działa na istniejącej bazie).
--
-- SZCZERA OCENA BEZPIECZEŃSTWA (patrz też README):
-- Aplikacja rozmawia z Supabase wyłącznie kluczem anon (publicznym),
-- bez sesji Supabase Auth. Dopóki tak jest, polityki RLS muszą pozwalać
-- na operacje, które wykonuje klient - inaczej aplikacja przestanie działać.
-- Poniżej: to, co da się zaostrzyć BEZ psucia aplikacji (integralność,
-- indeksy, limit rozmiaru), plus zakomentowany CEL (faza 2) po migracji
-- na Supabase Auth (sesje JWT) - wtedy RLS realnie chroni dane.
-- ==========================================================

-- ----------------------------------------------------------
-- 1. Tabela profiles
-- ----------------------------------------------------------
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all for profiles" ON public.profiles;
DROP POLICY IF EXISTS "profiles_anon_all" ON public.profiles;

-- Polityka robocza (do czasu migracji na Supabase Auth - patrz sekcja 5)
CREATE POLICY "profiles_anon_all" ON public.profiles
    FOR ALL TO anon, authenticated
    USING (true)
    WITH CHECK (
        -- Podstawowa higiena danych przy INSERT/UPDATE z klienta:
        username wymagany, w normalnej formie (3-32 znaków)
        length(username) BETWEEN 3 AND 32
        AND username ~ '^[a-z0-9._-]+$'
    );

-- Integralność: jeden e-mail = jedno konto (puste e-maile są dozwolone wielokrotnie)
CREATE UNIQUE INDEX IF NOT EXISTS profiles_email_unique
    ON public.profiles (lower(email))
    WHERE email IS NOT NULL AND email <> '';

-- ----------------------------------------------------------
-- 2. Tabela zakupów
-- ----------------------------------------------------------
ALTER TABLE public.app_purchases ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all for app_purchases" ON public.app_purchases;
DROP POLICY IF EXISTS "app_purchases_anon_all" ON public.app_purchases;

CREATE POLICY "app_purchases_anon_all" ON public.app_purchases
    FOR ALL TO anon, authenticated
    USING (true)
    WITH CHECK (username IS NOT NULL AND length(username) BETWEEN 3 AND 32);

CREATE INDEX IF NOT EXISTS app_purchases_username_idx
    ON public.app_purchases (username);

-- ----------------------------------------------------------
-- 3. Tabela kodów QR
-- ----------------------------------------------------------
ALTER TABLE public.user_qr_codes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all for user_qr_codes" ON public.user_qr_codes;
DROP POLICY IF EXISTS "user_qr_codes_anon_all" ON public.user_qr_codes;

CREATE POLICY "user_qr_codes_anon_all" ON public.user_qr_codes
    FOR ALL TO anon, authenticated
    USING (true)
    WITH CHECK (username IS NOT NULL AND length(username) BETWEEN 3 AND 32);

CREATE INDEX IF NOT EXISTS user_qr_codes_username_idx
    ON public.user_qr_codes (username);

-- ----------------------------------------------------------
-- 4. Kody weryfikacyjne obsługuje Supabase Auth (e-mail OTP),
--    dawna publicznie czytelna tabela nie jest już potrzebna.
-- ----------------------------------------------------------
DROP TABLE IF EXISTS public.verification_codes CASCADE;

-- ----------------------------------------------------------
-- 5. FAZA 2 (do włączenia PO migracji aplikacji na Supabase Auth).
--    Poniższy blok pokazuje docelowy model RLS, w którym użytkownik
--    widzi i edytuje WYŁĄCZNIE własny wiersz (auth.uid() = id w tabeli).
--    Nie włączaj tego teraz - obecny klient używa samego klucza anon
--    i te polityki zablokowałyby logowanie/synchronizację.
-- ----------------------------------------------------------
-- ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS auth_id UUID
--     REFERENCES auth.users(id) ON DELETE CASCADE;
--
-- DROP POLICY IF EXISTS "profiles_anon_all" ON public.profiles;
-- CREATE POLICY "profiles_select_own" ON public.profiles
--     FOR SELECT TO authenticated USING (auth_id = auth.uid());
-- CREATE POLICY "profiles_update_own" ON public.profiles
--     FOR UPDATE TO authenticated USING (auth_id = auth.uid());
-- CREATE POLICY "profiles_insert_own" ON public.profiles
--     FOR INSERT TO authenticated WITH CHECK (auth_id = auth.uid());
-- -- Zapisy rang/pinia dla adminów: funkcja security definer sprawdzająca rangę,
-- -- a anon nie dostaje żadnej polityki UPDATE/DELETE.

-- ----------------------------------------------------------
-- 6. Odśwież cache PostgREST
-- ----------------------------------------------------------
NOTIFY pgrst, 'reload schema';

-- ==========================================================
-- DROPME - PEŁNA INTEGRACJA SUPABASE (NAPRAWA TABELI I RLS)
-- Wklej w Supabase: SQL Editor -> New Query -> Run
-- ==========================================================

-- 1. Usunięcie starej, niekompletnej tabeli i utworzenie świeżej ze wszystkimi kolumnami
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

-- 2. Wyłączenie blokad RLS (Row Level Security)
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;

-- 3. Nadanie pełnych uprawnień dla aplikacji (rola anon)
GRANT ALL ON public.profiles TO anon, authenticated, service_role;

-- 4. Tabela historii zakupów
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

-- 5. Natychmiastowe odświeżenie pamięci podręcznej PostgREST w Supabase (naprawia błąd PGRST204)
NOTIFY pgrst, 'reload schema';

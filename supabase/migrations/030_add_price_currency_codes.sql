-- Store the currency that belongs to user-entered or receipt-detected prices.

ALTER TABLE public.ratings
    ADD COLUMN IF NOT EXISTS currency_code TEXT;

ALTER TABLE public.review_group_items
    ADD COLUMN IF NOT EXISTS currency_code TEXT;


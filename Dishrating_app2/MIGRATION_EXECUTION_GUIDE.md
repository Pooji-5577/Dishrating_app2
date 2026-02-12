# UUID to TEXT Migration Execution Guide

## Critical Issue Being Fixed

**Error**: `invalid input syntax for type uuid: "2kNcLi5hV0Ya7wxxmPCwlbm712"`

**Root Cause**: The Supabase database still has UUID-type columns, but the app uses Firebase UIDs (TEXT format).

**Solution**: This migration comprehensively converts ALL UUID columns to TEXT type.

---

## Prerequisites

1. Access to Supabase Dashboard
2. Database backup (automatic in Supabase, but verify backup exists)
3. No active users submitting data during migration (recommended)

---

## Step 1: Execute the Migration Script

### 1.1 Open Supabase SQL Editor

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Select your project: **SmackCheck** (or your project name)
3. Navigate to: **SQL Editor** (left sidebar)
4. Click **"New query"**

### 1.2 Run the Migration

1. Open the file: `definitive_uuid_to_text_fix.sql`
2. Copy the **entire contents** of the file
3. Paste into Supabase SQL Editor
4. Click **"Run"** (or press `Ctrl+Enter` / `Cmd+Enter`)

### 1.3 Monitor Execution

Watch the **Messages** tab in the Results panel. You should see:

```
NOTICE: ==================== PRE-MIGRATION AUDIT ====================
NOTICE: Checking all columns with UUID or TEXT types...
NOTICE:   badges.id - Type: uuid, Default: gen_random_uuid()
NOTICE:   dishes.id - Type: uuid, Default: gen_random_uuid()
... (more columns listed)

(Conversion happens here...)

NOTICE: ==================== POST-MIGRATION AUDIT ====================
NOTICE: Verifying all ID columns are now TEXT type...
NOTICE:   ✓ badges.id - Type: TEXT, Default: generate_text_id('badge_'::text)
NOTICE:   ✓ dishes.id - Type: TEXT, Default: generate_text_id('dish_'::text)
... (more columns listed)

NOTICE: Testing ID generation functions...
NOTICE:   Sample dish ID: dish_Kj3mN9xLqP2rT5vW8yZ
NOTICE:   Sample rating ID: rating_A1bC2dE3fG4hI5jK6lM
NOTICE:   Sample restaurant ID: rst_N7oP8qR9sT0uV1wX2yZ

NOTICE: ✓✓✓ MIGRATION SUCCESSFUL! ✓✓✓
NOTICE: All UUID columns converted to TEXT
NOTICE: All DEFAULT generators updated to generate_text_id()
NOTICE: Database ready for Firebase UIDs and Google Place IDs

COMMIT
```

**Expected Duration**: 5-15 seconds

### 1.4 Check for Errors

- If you see **ERROR** messages, copy them and read the error details
- Common errors and solutions:
  - **"relation does not exist"**: Table doesn't exist (safe to ignore if it's an optional table)
  - **"column does not exist"**: Column doesn't exist in that table (safe to ignore)
  - **"constraint does not exist"**: Already dropped or never existed (safe to ignore)
  - **Any other errors**: STOP and report the error before proceeding

---

## Step 2: Verify the Migration

### 2.1 Run Verification Script

1. In Supabase SQL Editor, open a **new query**
2. Open the file: `verify_migration.sql`
3. Copy and paste the entire contents
4. Click **"Run"**

### 2.2 Check Verification Results

You'll see multiple result tables:

#### Result 1: Column Type Verification
All rows should show `✓ CORRECT` in the status column:

| table_name | column_name | data_type | column_default | status |
|------------|-------------|-----------|----------------|--------|
| badges | id | text | generate_text_id('badge_'::text) | ✓ CORRECT |
| dishes | id | text | generate_text_id('dish_'::text) | ✓ CORRECT |
| ratings | user_id | text | (none) | ✓ CORRECT |

**Warning**: If any row shows `✗ STILL UUID`, the migration is incomplete!

#### Result 2: Foreign Key Constraints
Should show all foreign key relationships with `✓ FK Exists`:

| table_name | column_name | foreign_table_name | status |
|------------|-------------|-------------------|--------|
| ratings | user_id | profiles | ✓ FK Exists |
| ratings | dish_id | dishes | ✓ FK Exists |

#### Result 3: Index Verification
Should show all indexes rebuilt with `✓ Index Exists`.

#### Result 4: Function Verification
Should show `generate_text_id` function exists with `✓ Function Exists`.

#### Result 5: RLS Verification
All tables should show `✓ RLS Enabled`.

#### Result 6: Migration Summary
Final result should show:

| total_columns | columns_converted_to_text | columns_still_uuid | migration_status |
|---------------|---------------------------|-------------------|------------------|
| 25 | 25 | 0 | ✓✓✓ MIGRATION COMPLETE ✓✓✓ |

(Numbers may vary based on your exact schema)

---

## Step 3: Test the App

### 3.1 Clear App Data (Recommended)

On your Android device/emulator:
1. Go to: **Settings** → **Apps** → **SmackCheck**
2. Tap: **Storage & cache**
3. Tap: **Clear storage** (or "Clear data")
4. Confirm

This ensures the app fetches fresh schema information.

### 3.2 Test Rating Submission

1. **Launch the app**
2. **Sign in** with Firebase (Google or email/password)
3. **Tap camera icon** to capture a dish photo
4. **Wait for AI detection** (dish name auto-fills)
5. **Select restaurant** from GPS nearby list
6. **Enter rating** (1-5 stars) and optional comment
7. **Tap "Submit"**

**Expected Result**: ✓ Rating submits successfully (no UUID error)

### 3.3 Verify in Supabase

1. Go to: **Table Editor** in Supabase Dashboard
2. Open the **`ratings`** table
3. Check the most recent row:
   - `id` should be TEXT format (e.g., `rating_Abc123XyzDef456`)
   - `user_id` should be Firebase UID (e.g., `2kNcLi5hV0Ya7wxxmPCwlbm712`)
   - `dish_id` should be TEXT format
   - `restaurant_id` should be TEXT or Google Place ID

---

## Step 4: Additional Testing

### Test Case 1: New User Signup
1. Sign up with new account
2. Check `profiles` table: `id` should be Firebase UID (TEXT)

### Test Case 2: Restaurant with Google Place ID
1. Select restaurant from GPS list (Google Places API)
2. Submit rating
3. Check `restaurants` table: `id` should be Google Place ID (starts with "ChI")

### Test Case 3: Like a Rating
1. In feed, tap heart icon on a rating
2. Check `likes` table: all ID columns should be TEXT format

### Test Case 4: View Profile
1. Tap profile icon
2. View your ratings and stats
3. No UUID errors should appear

---

## Troubleshooting

### Issue: Migration shows errors

**Solution**:
1. Copy the full error message
2. Check if it's for an optional table that doesn't exist (safe to ignore)
3. If it's a critical error, restore from backup and investigate

### Issue: Verification shows columns still UUID

**Symptoms**: Verification script shows `✗ STILL UUID` for some columns

**Solution**:
1. Identify which table/column is still UUID
2. Manually convert that specific column:
   ```sql
   ALTER TABLE <table_name> ALTER COLUMN <column_name> DROP DEFAULT;
   ALTER TABLE <table_name> ALTER COLUMN <column_name> TYPE TEXT USING CAST(<column_name> AS TEXT);
   ALTER TABLE <table_name> ALTER COLUMN <column_name> SET DEFAULT generate_text_id('<prefix_>');
   ```
3. Re-run verification script

### Issue: Rating submission still fails

**Symptoms**: Still getting "invalid input syntax for type uuid" error

**Diagnosis**:
1. Run verification script to check column types
2. Check the exact error message - which table/column?
3. Verify the error is actually from the database, not client-side validation

**Solution**:
1. If verification shows all TEXT but error persists:
   - Clear app cache/storage
   - Rebuild app: `./gradlew clean assembleDebug`
2. If verification shows UUID columns remain:
   - Run the migration script again (it's idempotent)
   - Or manually fix specific columns as above

### Issue: Foreign key constraint violations

**Symptoms**: Errors about foreign key constraints when inserting data

**Solution**:
1. Check that parent records exist (e.g., profile exists before creating rating)
2. Verify foreign keys were recreated:
   ```sql
   SELECT * FROM information_schema.table_constraints
   WHERE constraint_type = 'FOREIGN KEY'
   AND table_schema = 'public';
   ```
3. If missing, add manually (see migration script for examples)

---

## Rollback Plan

### Option 1: Restore from Supabase Backup (Recommended)

1. Go to: **Settings** → **Backups** in Supabase Dashboard
2. Find the backup from before migration
3. Click **"Restore"**
4. Wait for restoration to complete
5. **Warning**: This will lose any data entered after the backup

### Option 2: Manual Rollback (Not Recommended)

Converting TEXT back to UUID is complex and not recommended if you already have Firebase UIDs stored as TEXT. Data loss would occur.

---

## Post-Migration Checklist

- [ ] Migration script executed successfully
- [ ] Verification script shows all columns as TEXT
- [ ] Verification script shows 0 columns still UUID
- [ ] Test user can submit rating without UUID error
- [ ] Rating appears in `ratings` table with TEXT IDs
- [ ] Restaurant with Google Place ID works correctly
- [ ] New user signup creates TEXT Firebase UID in `profiles`
- [ ] Likes functionality works
- [ ] Profile page loads without errors
- [ ] No UUID-related errors in app logs

---

## Success Criteria

✓ All ID columns in database are TEXT type
✓ All DEFAULT generators use `generate_text_id()` function
✓ Foreign key constraints recreated for TEXT types
✓ Indexes rebuilt for TEXT columns
✓ RLS enabled on all tables
✓ Firebase UIDs (TEXT) can be inserted into `user_id` columns
✓ Google Place IDs (TEXT) can be inserted into `restaurant_id` column
✓ Rating submission works end-to-end without UUID errors

---

## Support

If you encounter issues:

1. **Check the verification results** - This tells you exactly what's wrong
2. **Review error messages** - Copy the full error text
3. **Test with minimal case** - Try inserting a simple test row directly in Supabase SQL Editor:
   ```sql
   INSERT INTO profiles (id, username, email, created_at, updated_at)
   VALUES ('2kNcLi5hV0Ya7wxxmPCwlbm712', 'testuser', 'test@example.com', NOW(), NOW());
   ```
   This should succeed if migration worked.

---

## Files Created

1. `definitive_uuid_to_text_fix.sql` - Main migration script (execute first)
2. `verify_migration.sql` - Verification queries (execute second)
3. `MIGRATION_EXECUTION_GUIDE.md` - This guide (read first)

---

## Why This Migration is Definitive

Previous migrations failed because they were:
- **Partial**: Only targeted specific tables
- **Incomplete**: Missed some columns
- **Fragmented**: Multiple scripts with dependencies
- **Unverified**: No systematic verification

This migration succeeds because it:
- **Comprehensive**: Converts ALL UUID columns in one pass
- **Atomic**: Single transaction (BEGIN...COMMIT)
- **Explicit**: Uses CAST to force conversion
- **Verified**: Pre/post audits confirm success
- **Idempotent**: Safe to run multiple times

---

**Ready to execute?** Start with Step 1 above!

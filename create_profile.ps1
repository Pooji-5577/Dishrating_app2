# Script to create profile in Supabase
$supabaseUrl = "https://ayopmvhtfuwbsjxhpfgd.supabase.co"
$supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF5b3Btdmh0ZnV3YnNqeGhwZmdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyNjAyMTksImV4cCI6MjA4NDgzNjIxOX0.2siGUJfE3iLoaEKae5gycw_6mo748KKyi5C7YEHuUlQ"

$userId = "8b6156e1-7793-45c7-9e46-a30ada4a83c5"
$email = "manasareddycherukupalli@gmail.com"
$name = "Manasa Cherukupalli"

Write-Host "Creating profile for user: $name" -ForegroundColor Cyan
Write-Host "User ID: $userId" -ForegroundColor Yellow
Write-Host ""

$headers = @{
    "apikey" = $supabaseKey
    "Authorization" = "Bearer $supabaseKey"
    "Content-Type" = "application/json"
    "Prefer" = "return=representation"
}

# Check if profile exists
Write-Host "Step 1: Checking if profile exists..." -ForegroundColor Cyan
$checkUrl = "$supabaseUrl/rest/v1/profiles?id=eq.$userId"

try {
    $existingProfile = Invoke-RestMethod -Uri $checkUrl -Method Get -Headers $headers -ErrorAction Stop
    
    if ($existingProfile.Count -gt 0) {
        Write-Host "Profile already exists!" -ForegroundColor Green
        $existingProfile | ConvertTo-Json -Depth 3
        exit 0
    }
    
    Write-Host "No existing profile found." -ForegroundColor Yellow
}
catch {
    Write-Host "Could not check for existing profile. Will try to create..." -ForegroundColor Yellow
}

# Create profile
Write-Host ""
Write-Host "Step 2: Creating profile..." -ForegroundColor Cyan

$profileData = @{
    id = $userId
    email = $email
    name = $name
    level = 1
    xp = 0
    streak_count = 0
} | ConvertTo-Json

$createUrl = "$supabaseUrl/rest/v1/profiles"

try {
    $result = Invoke-RestMethod -Uri $createUrl -Method Post -Headers $headers -Body $profileData -ErrorAction Stop
    
    Write-Host "Profile created successfully!" -ForegroundColor Green
    Write-Host ""
    $result | ConvertTo-Json -Depth 3
}
catch {
    Write-Host "Error creating profile:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    
    # Still try to verify
}

# Verify profile
Write-Host ""
Write-Host "Step 3: Verifying profile..." -ForegroundColor Cyan

try {
    $verifiedProfile = Invoke-RestMethod -Uri $checkUrl -Method Get -Headers $headers -ErrorAction Stop
    
    if ($verifiedProfile.Count -gt 0) {
        Write-Host "Profile verified!" -ForegroundColor Green
        Write-Host ""
        $verifiedProfile | ConvertTo-Json -Depth 3
        Write-Host ""
        Write-Host "SUCCESS! You can now test commenting in the app." -ForegroundColor Green
    }
    else {
        Write-Host "Profile not found after creation." -ForegroundColor Yellow
    }
}
catch {
    Write-Host "Could not verify profile:" -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor Yellow
}

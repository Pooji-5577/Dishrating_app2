# Test script for Gemini API with gemini-3-flash-preview model
# This tests the API key and model availability
# Reads API key securely from local.properties

$LOCAL_PROPERTIES = "C:/Users/manas/Desktop/smackcheck3/Dishrating_app2/local.properties"

# Check if local.properties exists
if (-not (Test-Path $LOCAL_PROPERTIES)) {
    Write-Host "ERROR: local.properties not found at $LOCAL_PROPERTIES" -ForegroundColor Red
    exit 1
}

# Read API key from local.properties
$API_KEY = ""
Get-Content $LOCAL_PROPERTIES | ForEach-Object {
    if ($_ -match "^GEMINI_API_KEY=(.+)$") {
        $API_KEY = $matches[1].Trim()
    }
}

if ([string]::IsNullOrEmpty($API_KEY)) {
    Write-Host "ERROR: GEMINI_API_KEY not found in local.properties" -ForegroundColor Red
    exit 1
}

$MODEL = "gemini-3-flash-preview"
$URL = "https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${API_KEY}"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Gemini API" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Model: $MODEL"
Write-Host "API Key: $($API_KEY.Substring(0, 10))...***" -ForegroundColor Gray
Write-Host ""

# Create test request body
$requestBody = @{
    contents = @(
        @{
            parts = @(
                @{
                    text = "Hello! Can you respond with just OK to confirm you are working?"
                }
            )
        }
    )
    generationConfig = @{
        temperature = 0.4
        maxOutputTokens = 100
    }
} | ConvertTo-Json -Depth 10

Write-Host "Sending test request..." -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $URL -Method POST -Body $requestBody -ContentType "application/json" -UseBasicParsing -ErrorAction Stop

    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "The API key works with $MODEL!" -ForegroundColor Green
    Write-Host "The model exists and is accessible!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}
catch {
    Write-Host "FAILED!" -ForegroundColor Red

    # Get status code safely
    $statusCode = $null
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
    } else {
        Write-Host "Status Code: N/A (Network/Connection Error)" -ForegroundColor Red
    }
    Write-Host ""

    # Try to get error details
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorBody = $reader.ReadToEnd()
            Write-Host "Error Response:" -ForegroundColor Red
            Write-Host $errorBody
            $reader.Close()
        }
        catch {
            Write-Host "Could not read error response: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "Error Details:" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        Write-Host ""
        Write-Host "Full Exception:" -ForegroundColor Yellow
        Write-Host $_.Exception -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow

    if ($null -eq $statusCode) {
        Write-Host "Network/Connection Error - Possible causes:" -ForegroundColor Red
        Write-Host "  1. No internet connection" -ForegroundColor Yellow
        Write-Host "  2. Firewall blocking the request" -ForegroundColor Yellow
        Write-Host "  3. DNS resolution failed" -ForegroundColor Yellow
        Write-Host "  4. Invalid API endpoint" -ForegroundColor Yellow
    } else {
        switch ($statusCode) {
            403 {
                Write-Host "403 PERMISSION DENIED - This means:" -ForegroundColor Red
                Write-Host "  1. The API key is valid BUT does not have permission" -ForegroundColor Yellow
                Write-Host "  2. You need to enable Generative Language API:" -ForegroundColor Yellow
                Write-Host "     https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com" -ForegroundColor Cyan
                Write-Host "  3. Select your project and click Enable button" -ForegroundColor Yellow
            }
            404 {
                Write-Host "404 NOT FOUND - This means:" -ForegroundColor Red
                Write-Host "  1. The model gemini-3-flash-preview does not exist or is not available" -ForegroundColor Yellow
                Write-Host "  2. Try using a stable model instead:" -ForegroundColor Yellow
                Write-Host "     - gemini-1.5-flash (recommended)" -ForegroundColor Cyan
                Write-Host "     - gemini-1.5-pro" -ForegroundColor Cyan
                Write-Host "     - gemini-2.0-flash-exp" -ForegroundColor Cyan
            }
            429 {
                Write-Host "429 RATE LIMIT - Too many requests" -ForegroundColor Red
                Write-Host "  Wait a few minutes and try again" -ForegroundColor Yellow
            }
            default {
                Write-Host "HTTP $statusCode error" -ForegroundColor Red
                Write-Host "  Check the error response above for details" -ForegroundColor Yellow
            }
        }
    }
}

// Supabase Edge Function for AI Dish Analysis using Gemini
import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from 'npm:@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface DishAnalysisRequest {
  imageBase64: string  // Base64 encoded image
  mimeType?: string    // e.g., "image/jpeg", "image/png"
}

interface DishAnalysisResponse {
  dishName: string
  cuisine: string
  confidence: number
  alternatives: string[]
  itemType: string  // "food", "beverage", or "unknown"
  restaurantChain: string  // e.g. "Starbucks" when branded packaging/logo visible, "" otherwise
  restaurantType: string   // e.g. "cafe", "fast food", "pizzeria", "sushi bar", "" if unclear
  error?: string
}

const DETECTION_PROMPT = `Identify the main visible food dish or beverage.
Return ONLY compact JSON with these exact keys:
{"dish_name":"","cuisine":"","confidence":0,"item_type":"","alternatives":[],"restaurant_chain":"","restaurant_type":""}
Rules:
- item_type is "food", "beverage", or "unknown".
- Use "Unknown" and confidence 0 only when no food or drink is visible.
- alternatives: max 2 short names.
- restaurant_chain only when clear branding/logo is visible, else "".
- restaurant_type: short lowercase venue type like "restaurant", "cafe", "pizzeria", "fast food", or "".
- No markdown, prose, description, or ingredients.`

Deno.serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Verify the caller is authenticated
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: 'Missing authorization header', dishName: 'Unknown', cuisine: '', confidence: 0, alternatives: [], itemType: 'unknown', restaurantChain: '', restaurantType: '' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_ANON_KEY')!
    )
    const { data: { user }, error: authError } = await supabase.auth.getUser(authHeader.replace('Bearer ', ''))
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: 'Invalid or expired token', dishName: 'Unknown', cuisine: '', confidence: 0, alternatives: [], itemType: 'unknown', restaurantChain: '', restaurantType: '' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Get the Gemini API key from environment (stored as secret)
    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY')
    if (!GEMINI_API_KEY) {
      throw new Error('GEMINI_API_KEY not configured on server')
    }

    // Parse request body
    const { imageBase64, mimeType = 'image/jpeg' }: DishAnalysisRequest = await req.json()

    if (!imageBase64) {
      throw new Error('imageBase64 is required')
    }

    console.log(`Analyzing dish image: ${imageBase64.length} chars, mimeType: ${mimeType}`)

    // Call Gemini API with vision capabilities.
    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1alpha/models/gemini-3.1-flash-lite:generateContent?key=${GEMINI_API_KEY}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          contents: [{
            parts: [
              {
                inline_data: {
                  mime_type: mimeType,
                  data: imageBase64
                }
              },
              {
                text: DETECTION_PROMPT
              }
            ]
          }],
          generationConfig: {
            temperature: 0.1,
            topK: 16,
            topP: 0.8,
            maxOutputTokens: 192,
            responseMimeType: "application/json",
          }
        })
      }
    )

    if (!geminiResponse.ok) {
      const errorText = await geminiResponse.text()
      console.error(`Gemini API error (${geminiResponse.status}): ${errorText}`)
      
      // Handle specific error codes
      const errorMessage = geminiResponse.status === 429 
        ? 'Rate limit exceeded. Please try again later.'
        : geminiResponse.status === 401 || geminiResponse.status === 403
        ? 'API key invalid or unauthorized.'
        : `Gemini API error: ${geminiResponse.status}`
      
      throw new Error(errorMessage)
    }

    const geminiData = await geminiResponse.json()
    
    // Extract the text response from Gemini
    const textResponse = geminiData.candidates?.[0]?.content?.parts?.[0]?.text
    
    if (!textResponse) {
      console.error('No text response from Gemini:', JSON.stringify(geminiData))
      throw new Error('No response from Gemini')
    }

    console.log(`Gemini response: ${textResponse.substring(0, 200)}...`)

    // Parse the JSON response from Gemini
    // Remove any markdown code blocks if present
    const cleanedResponse = textResponse
      .replace(/```json\n?/g, '')
      .replace(/\n?```/g, '')
      .trim()
    
    // Extract JSON object if there's extra text
    const jsonMatch = cleanedResponse.match(/\{[\s\S]*\}/)
    if (!jsonMatch) {
      throw new Error('Could not parse Gemini response as JSON')
    }

    const parsedResponse = JSON.parse(jsonMatch[0])
    
    // Map to our response format (handle both snake_case and camelCase from Gemini)
    const rawItemType = parsedResponse.item_type || parsedResponse.itemType || 'unknown'
    let itemType = ['food', 'beverage'].includes(rawItemType.toLowerCase()) ? rawItemType.toLowerCase() : 'unknown'

    // Keyword-based fallback: if Gemini didn't return a clear item_type, infer from the dish name
    const dishNameForType = (parsedResponse.dish_name || parsedResponse.dishName || '').toLowerCase()
    if (itemType === 'unknown' && dishNameForType && dishNameForType !== 'unknown') {
      const beverageKeywords = [
        'coffee', 'tea', 'juice', 'beer', 'wine', 'cocktail', 'smoothie', 'shake',
        'milkshake', 'latte', 'cappuccino', 'espresso', 'chai', 'soda', 'cola',
        'water', 'drink', 'beverage', 'mojito', 'lemonade', 'cider', 'punch',
        'americano', 'macchiato', 'mocha', 'frappe', 'matcha', 'lassi', 'kombucha',
        'whiskey', 'vodka', 'rum', 'gin', 'ale', 'lager', 'sangria', 'liquor',
        'margarita', 'daiquiri', 'spritzer', 'tonic', 'fizz', 'brew', 'shot'
      ]
      itemType = beverageKeywords.some(kw => dishNameForType.includes(kw)) ? 'beverage' : 'food'
    }

    const dishAnalysis: DishAnalysisResponse = {
      dishName: parsedResponse.dish_name || parsedResponse.dishName || 'Unknown',
      cuisine: parsedResponse.cuisine || 'Unknown',
      confidence: parsedResponse.confidence || 0,
      alternatives: parsedResponse.alternatives || [],
      itemType,
      restaurantChain: (parsedResponse.restaurant_chain || parsedResponse.restaurantChain || '').toString().trim(),
      restaurantType: (parsedResponse.restaurant_type || parsedResponse.restaurantType || '').toString().trim().toLowerCase()
    }

    console.log(`Detected: ${dishAnalysis.dishName} (type=${dishAnalysis.itemType}, confidence=${dishAnalysis.confidence})`)

    return new Response(
      JSON.stringify(dishAnalysis),
      { 
        headers: { 
          ...corsHeaders,
          'Content-Type': 'application/json' 
        } 
      }
    )

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    console.error('Error analyzing dish:', errorMessage)
    
    return new Response(
      JSON.stringify({
        error: errorMessage,
        dishName: 'Unknown',
        cuisine: '',
        confidence: 0,
        alternatives: [],
        itemType: 'unknown',
        restaurantChain: '',
        restaurantType: ''
      }),
      { 
        status: 400,
        headers: { 
          ...corsHeaders,
          'Content-Type': 'application/json' 
        } 
      }
    )
  }
})

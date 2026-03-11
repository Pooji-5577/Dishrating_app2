// Supabase Edge Function for AI Dish Analysis using Gemini
import "@supabase/functions-js/edge-runtime.d.ts"

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
  description: string
  ingredients: string[]
  itemType: string  // "food", "beverage", or "unknown"
  error?: string
}

const DETECTION_PROMPT = `You are a food and beverage identification expert. Look at this image and identify what is shown — it could be a food dish OR a beverage (drink).

IMPORTANT: You MUST respond with ONLY a JSON object, no other text before or after.

Response format for FOOD (JSON only):
{"dish_name":"Pizza Margherita","cuisine":"Italian","confidence":0.9,"item_type":"food","alternatives":["Cheese Pizza","Flatbread"],"description":"Classic Italian pizza with tomato sauce, mozzarella, and basil","ingredients":["tomato sauce","mozzarella","basil","olive oil"]}

Response format for BEVERAGE (JSON only):
{"dish_name":"Cappuccino","cuisine":"Italian","confidence":0.95,"item_type":"beverage","alternatives":["Latte","Coffee"],"description":"Italian espresso-based coffee drink with steamed milk foam","ingredients":["espresso","steamed milk","milk foam"]}

Guidelines:
1. dish_name: The most common English name for the item (be specific, e.g., "Chicken Tikka Masala" not just "Curry", "Mango Lassi" not just "Drink")
2. cuisine: The type of cuisine or origin (Italian, Indian, Mexican, Chinese, American, Japanese, Thai, etc.)
3. confidence: A number between 0.0 and 1.0 indicating how certain you are
4. item_type: Use "food" for solid food dishes/snacks/desserts, "beverage" for any drink (coffee, tea, juice, cocktail, milkshake, wine, beer, smoothie, soda, water, etc.), or "unknown" only if the image clearly shows neither food nor a drink
5. alternatives: 1-3 other possible names if you're not 100% sure
6. description: A brief 1-2 sentence description
7. ingredients: List of visible or likely ingredients

If the image clearly shows neither food nor a beverage, respond with:
{"dish_name":"Unknown","cuisine":"Unknown","confidence":0.0,"item_type":"unknown","alternatives":[],"description":"Unable to identify food or beverage","ingredients":[]}

Remember: Output ONLY the JSON, nothing else.`

Deno.serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
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

    // Call Gemini API with vision capabilities (using gemini-3.1-flash-lite-preview for best results)
    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=${GEMINI_API_KEY}`,
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
            temperature: 0.4,
            topK: 32,
            topP: 1,
            maxOutputTokens: 1024,
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
      description: parsedResponse.description || '',
      ingredients: parsedResponse.ingredients || [],
      itemType
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
        description: '',
        ingredients: [],
        itemType: 'unknown'
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

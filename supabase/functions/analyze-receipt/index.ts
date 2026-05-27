import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from 'npm:@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface ReceiptAnalysisRequest {
  imageBase64: string
  mimeType?: string
  dishNames: string[]
}

const promptFor = (dishNames: string[]) => `You are reading a restaurant, cafe, theater, or food delivery receipt image.
First OCR every visible purchased food or drink line item with its price, then match line-item prices to these dishes: ${dishNames.join(', ')}.
Return ONLY compact JSON:
{"matches":[{"dishName":"","price":0,"confidence":0}],"receiptItems":[],"summary":""}
Rules:
- matches must use dishName values from the provided list exactly when possible.
- receiptItems must include visible purchasable line items like "AVOCADO BURGER 14.99", "SM POPCORN 8.50", or "PESTO PASTA 12.00".
- Ignore subtotal, tax, tip, service charge, discounts, balance, change, payment, card, approval, and total lines.
- price is the item price before tax/service when visible.
- confidence is 0-1.
- receiptItems is a short list of visible line items like "Paneer Tikka 240".
- If there is no exact name match, match the closest food category: any burger line can match an uploaded burger dish, pasta can match pasta, biryani can match rice/biryani, fries can match fries.
- If only one uploaded dish name is provided and exactly one plausible food line item price is visible, return that price for the dish with confidence 0.45 even if the names differ.
- If a dish cannot be matched and no plausible item price is visible, omit it from matches.
- No markdown or prose.`

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Missing authorization header', matches: [], receiptItems: [] }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const supabase = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_ANON_KEY')!)
    const { data: { user }, error: authError } = await supabase.auth.getUser(authHeader.replace('Bearer ', ''))
    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Invalid or expired token', matches: [], receiptItems: [] }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      })
    }

    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY')
    if (!GEMINI_API_KEY) throw new Error('GEMINI_API_KEY not configured on server')

    const { imageBase64, mimeType = 'image/jpeg', dishNames }: ReceiptAnalysisRequest = await req.json()
    if (!imageBase64) throw new Error('imageBase64 is required')
    if (!Array.isArray(dishNames) || dishNames.length === 0) throw new Error('dishNames are required')

    const geminiModel = Deno.env.get('GEMINI_MODEL') ?? 'gemini-2.5-flash-lite'
    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${GEMINI_API_KEY}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [
              { text: promptFor(dishNames) },
              {
                inline_data: { mime_type: mimeType, data: imageBase64 },
                media_resolution: { level: 'MEDIA_RESOLUTION_HIGH' },
              },
            ],
          }],
          generationConfig: {
            temperature: 0.1,
            maxOutputTokens: 512,
            responseMimeType: 'application/json',
          },
        }),
      }
    )

    if (!geminiResponse.ok) {
      const errorText = await geminiResponse.text()
      throw new Error(`Gemini API error: ${geminiResponse.status} ${errorText}`)
    }

    const geminiData = await geminiResponse.json()
    const text = geminiData.candidates?.[0]?.content?.parts?.[0]?.text ?? '{}'
    const cleaned = text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim()
    let parsed
    try {
      parsed = JSON.parse(cleaned)
    } catch {
      parsed = { matches: [], receiptItems: [], summary: '' }
    }

    return new Response(JSON.stringify({
      matches: Array.isArray(parsed.matches) ? parsed.matches : [],
      receiptItems: Array.isArray(parsed.receiptItems) ? parsed.receiptItems : [],
      summary: typeof parsed.summary === 'string' ? parsed.summary : '',
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    console.error('Receipt analysis error:', error)
    return new Response(JSON.stringify({ matches: [], receiptItems: [], summary: '', error: String(error?.message ?? error) }), {
      status: 200,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }
})

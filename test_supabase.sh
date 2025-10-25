#!/bin/bash

# Test Supabase connection
echo "Testing Supabase connection..."

# Your Supabase URL and API key
SUPABASE_URL="https://ffdvhjxvkxouwxcqjbzr.supabase.co"
SUPABASE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmZHZoanh2a3hvdXd4Y3FqYnpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI3MTAxOTEsImV4cCI6MjA0ODI4NjE5MX0.9G9vABtzioKGyd5OhX1CjE5uGtcfXvXWzFssXSpHSP0"

# Test fetching from Live Brands table
echo "Fetching from Live Brands table..."
curl -X GET \
  "${SUPABASE_URL}/rest/v1/Live%20Brands?select=*" \
  -H "apikey: ${SUPABASE_KEY}" \
  -H "Authorization: Bearer ${SUPABASE_KEY}" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  | python3 -m json.tool

echo ""
echo "Test complete!"

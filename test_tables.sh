#!/bin/bash

# Test to get schema information
echo "Testing Supabase tables..."

SUPABASE_URL="https://ffdvhjxvkxouwxcqjbzr.supabase.co"
SUPABASE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmZHZoanh2a3hvdXd4Y3FqYnpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI3MTAxOTEsImV4cCI6MjA0ODI4NjE5MX0.9G9vABtzioKGyd5OhX1CjE5uGtcfXvXWzFssXSpHSP0"

# Try different table name formats
echo "1. Testing 'Live Brands' with space:"
curl -s -o /dev/null -w "%{http_code}\n" \
  "${SUPABASE_URL}/rest/v1/Live%20Brands?select=*&limit=1" \
  -H "apikey: ${SUPABASE_KEY}"

echo "2. Testing 'Live_Brands' with underscore:"
curl -s -o /dev/null -w "%{http_code}\n" \
  "${SUPABASE_URL}/rest/v1/Live_Brands?select=*&limit=1" \
  -H "apikey: ${SUPABASE_KEY}"

echo "3. Testing 'live_brands' lowercase:"
curl -s -o /dev/null -w "%{http_code}\n" \
  "${SUPABASE_URL}/rest/v1/live_brands?select=*&limit=1" \
  -H "apikey: ${SUPABASE_KEY}"

echo "4. Testing 'LiveBrands' no space:"
curl -s -o /dev/null -w "%{http_code}\n" \
  "${SUPABASE_URL}/rest/v1/LiveBrands?select=*&limit=1" \
  -H "apikey: ${SUPABASE_KEY}"

echo ""
echo "HTTP Status Codes:"
echo "200 = Table exists and accessible"
echo "404 = Table not found"
echo "401 = Authentication error"

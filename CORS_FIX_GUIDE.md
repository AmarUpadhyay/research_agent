# CORS Configuration & Troubleshooting Guide

**Last Updated**: April 3, 2026  
**Status**: ✅ CORS Fixed

---

## 🔧 What Was Done to Fix CORS

### 1. Created CORS Configuration Class
**File**: `src/main/java/com/researchagent/config/CorsConfig.java`

This configuration class:
- ✅ Allows cross-origin requests to `/api/**` endpoints
- ✅ Supports multiple frontend origins (localhost:8000, 3000, 5173, etc.)
- ✅ Allows all HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
- ✅ Allows all headers and credentials
- ✅ Caches preflight responses for 1 hour

### 2. Updated AgentController
**File**: `src/main/java/com/researchagent/controller/AgentController.java`

Added `@CrossOrigin` annotation with:
- ✅ Multiple allowed origins
- ✅ Allowed methods specified
- ✅ Credentials support enabled

---

## ✅ Allowed Origins

The following origins are now allowed to make requests:

| Origin | Purpose |
|--------|---------|
| `http://localhost:8000` | Python HTTP server |
| `http://localhost:3000` | React dev server |
| `http://localhost:5173` | Vite dev server |
| `http://127.0.0.1:*` | Local machine on any port |
| `file://*` | File protocol (opening HTML directly) |

### To Add More Origins

Edit `CorsConfig.java`:
```java
.allowedOrigins(
    "http://localhost:8000",
    "http://example.com",     // Add your domain
    "https://app.example.com" // Add your app domain
)
```

---

## 🚀 How to Use

### Step 1: Rebuild the Application

```bash
cd /Users/amarupadhyaya/code/projects/research_agent
mvn clean package -DskipTests
```

Or just run with Maven:
```bash
mvn spring-boot:run
```

### Step 2: Open Frontend

The following methods should now work without CORS errors:

**Method A - Direct File (Python Server)**
```bash
cd /Users/amarupadhyaya/code/projects/research_agent
python3 -m http.server 8000

# Then open: http://localhost:8000/ai-chatbot-landing.html
```

**Method B - Node.js Live Server**
```bash
npx http-server . -p 8000
```

**Method C - VS Code Live Server**
- Install "Live Server" extension
- Right-click HTML file → "Open with Live Server"

### Step 3: Test API Calls

The landing page should now:
- ✅ Call `GET /api/agent/health` without CORS error
- ✅ Call `POST /api/agent/tasks` without CORS error
- ✅ Poll `GET /api/agent/tasks/{id}` without CORS error
- ✅ Show real-time agent execution

---

## 🔍 Troubleshooting

### Problem: Still Getting CORS Error

**Solution 1 - Check Browser Console**
```
F12 → Console tab → Look for exact error message
```

**Solution 2 - Verify Agent is Running**
```
curl http://localhost:8080/api/agent/health
→ Should return: {"status":"up"}
```

**Solution 3 - Check Your Frontend Origin**
- In browser console: `console.log(window.location.origin)`
- Make sure it's in the allowed origins list
- If not, add it to `CorsConfig.java`

**Solution 4 - Clear Browser Cache**
```
Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows/Linux)
```

### Problem: `OPTIONS` Request Failing

This is the preflight request. The CORS config now handles it automatically.

If still failing:
1. Rebuild: `mvn clean package -DskipTests`
2. Restart: `mvn spring-boot:run`
3. Clear browser cache: Cmd+Shift+R

### Problem: Credentials Not Sent

The configuration has `allowCredentials = true`, so cookies and auth headers will be sent automatically.

### Problem: Specific Origin Still Blocked

Edit `CorsConfig.java` and add your origin:
```java
.allowedOrigins(
    "http://localhost:8000",
    "http://your-domain.com"  // Add here
)
```

Then rebuild and restart.

---

## 📋 CORS Configuration Details

### What Each Setting Does

| Setting | Value | Purpose |
|---------|-------|---------|
| `addMapping` | `/api/**` | Which endpoints allow CORS |
| `allowedOrigins` | Multiple | Which domains can access |
| `allowedMethods` | GET, POST, etc | Which HTTP methods allowed |
| `allowedHeaders` | `*` | All headers allowed |
| `allowCredentials` | `true` | Allow cookies/auth headers |
| `maxAge` | `3600` | Cache preflight for 1 hour |

### Environment-Specific Configuration

For production, modify `CorsConfig.java`:

```java
@Value("${cors.allowed-origins}")
private String[] allowedOrigins;

// Then use the property in application.yml
```

Then add to `application.yml`:
```yaml
cors:
  allowed-origins:
    - https://myapp.com
    - https://www.myapp.com
```

---

## 🎯 Common Scenarios

### Scenario 1: Local Development (This Setup)
```
Frontend: http://localhost:8000
Backend: http://localhost:8080
Status: ✅ Works (localhost is allowed)
```

### Scenario 2: React App
```
Frontend: http://localhost:3000
Backend: http://localhost:8080
Status: ✅ Works (3000 is allowed)
```

### Scenario 3: Vite App
```
Frontend: http://localhost:5173
Backend: http://localhost:8080
Status: ✅ Works (5173 is allowed)
```

### Scenario 4: Production
```
Frontend: https://myapp.com
Backend: https://api.myapp.com
Status: ⚠️ Needs configuration update
```

For production, update:
```java
.allowedOrigins("https://myapp.com")
```

### Scenario 5: Any Port on Localhost
```
Frontend: http://127.0.0.1:7000 (any port)
Backend: http://localhost:8080
Status: ✅ Works (127.0.0.1:* is allowed)
```

---

## 🧪 Testing CORS

### Test with cURL (No CORS Issue)
```bash
curl -X POST http://localhost:8080/api/agent/tasks \
  -H "Content-Type: application/json" \
  -d '{"task":"Find pending items"}'
```

### Test with Browser Console
```javascript
// Open browser console (F12) and run:
fetch('http://localhost:8080/api/agent/health')
    .then(r => r.json())
    .then(d => console.log(d))
    .catch(e => console.error('CORS Error:', e))
```

If you see the result logged, CORS is working! ✅

---

## 📊 CORS Response Headers

When CORS is working, you'll see these headers:

```
Access-Control-Allow-Origin: http://localhost:8000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

Check in: DevTools → Network tab → Click any request → Response Headers

---

## ✅ Verification Checklist

After applying these changes:

- [ ] CORS config file created: `CorsConfig.java`
- [ ] Controller updated with `@CrossOrigin` annotation
- [ ] Application rebuilt: `mvn clean package`
- [ ] Agent running: `mvn spring-boot:run`
- [ ] Frontend served on allowed port
- [ ] No CORS errors in browser console
- [ ] API calls successful from frontend
- [ ] Real-time updates working

---

## 🔗 Related Files

| File | Purpose |
|------|---------|
| `CorsConfig.java` | CORS configuration (NEW) |
| `AgentController.java` | REST endpoints (UPDATED) |
| `ai-chatbot-landing.html` | Frontend calling APIs |

---

## 💡 Pro Tips

1. **Always rebuild after changing CORS config**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Use exact origins in production**
   - Development: `*` (all origins)
   - Production: Specific domains only

3. **Browser console shows CORS errors clearly**
   - F12 → Console tab
   - Look for "Access to XMLHttpRequest blocked by CORS policy"

4. **Preflight requests are automatic**
   - Browser sends OPTIONS before POST
   - You don't need to handle them manually
   - Configuration handles it

5. **Credentials need explicit permission**
   - Already enabled: `allowCredentials = true`
   - Allows sending cookies/auth headers

---

## 🚀 Next Steps

1. **Rebuild application**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start agent**
   ```bash
   mvn spring-boot:run
   ```

3. **Open landing page**
   - File: `ai-chatbot-landing.html`
   - Or: `http://localhost:8000/ai-chatbot-landing.html`

4. **Test the integration**
   - Click "Check Health"
   - Click "Execute Agent"
   - No CORS errors should appear! ✅

---

## 📞 Support

If you still have CORS issues:

1. Check browser console for exact error
2. Verify agent is running on port 8080
3. Verify frontend origin is in allowed list
4. Clear cache and reload
5. Try from different port (test with Python server on 8000)
6. Check that application rebuilt successfully

---

**Status**: CORS configuration complete and ready to use! ✅


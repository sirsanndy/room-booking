# Frontend Production Deployment Guide

## Quick Start Options

### Option 1: Using npm serve (Simplest)
```bash
cd frontend
npm run build
npm run serve:prod
```
Or manually:
```bash
npx serve -s dist -l 5173
```

### Option 2: Using Vite Preview
```bash
cd frontend
npm run build
npm run preview
```
This uses Vite's built-in preview server (not for production use, only for testing the build).

### Option 3: Using Python HTTP Server
```bash
cd frontend
npm run build
cd dist
python3 -m http.server 5173
```

### Option 4: Using Docker + Nginx (Recommended for Production)
```bash
cd frontend

# Build the Docker image
npm run docker:build

# Run the container
npm run docker:run

# Or manually:
docker build -f Dockerfile.prod -t meeting-room-booking-frontend:prod .
docker run -p 80:80 meeting-room-booking-frontend:prod
```

Access at: http://localhost

## Full Production Deployment

### Using Docker Compose (Recommended)

The entire application stack (frontend, backend, database, Redis, monitoring) can be deployed with a single command:

```bash
# Quick start with automated script
./start-production.sh

# Or manually
docker-compose up -d --build
```

**Services included:**
- 📱 **Frontend (Nginx)**: http://localhost
- 🔧 **Backend (Spring Boot)**: http://localhost:8080  
- 🗄️ **PostgreSQL**: localhost:5432
- 📦 **Redis**: localhost:6379
- 📊 **Prometheus**: http://localhost:9090
- 📈 **Grafana**: http://localhost:3001 (admin/admin)
- 📉 **Postgres Exporter**: localhost:9187
- 📉 **Redis Exporter**: localhost:9121

**Management commands:**
```bash
# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f frontend
docker-compose logs -f backend

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Restart services
docker-compose restart

# Rebuild and restart specific service
docker-compose up -d --build frontend
```

### Manual Frontend-Only Deployment

If you want to deploy only the frontend:

### 1. Update docker-compose.yml for Production

The docker-compose.yml is already configured with all services:

```yaml
services:
  postgres:      # PostgreSQL database
  redis:         # Redis cache
  prometheus:    # Metrics collection
  grafana:       # Monitoring dashboard
  postgres-exporter: # PostgreSQL metrics
  redis-exporter:    # Redis metrics
  backend:       # Spring Boot API (NEW)
  frontend:      # Vue.js + Nginx (NEW)
```

The frontend Nginx is configured to proxy `/api` requests to the backend service automatically.

### 2. Deploy with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f frontend

# Stop all services
docker-compose down
```

Access the application at: http://localhost

## Environment Configuration

### Development vs Production

The frontend build uses environment variables. Update your backend API URL if needed:

**For local production testing:**
- API URL: `http://localhost:8080`

**For Docker deployment:**
- API URL: `http://backend:8080` (internal Docker network)
- Nginx will proxy `/api` requests to the backend

### Update API URL (if needed)

If you need to change the API endpoint, create/update `.env.production`:

```bash
cd frontend
echo "VITE_API_URL=http://your-backend-url:8080" > .env.production
```

Then rebuild:
```bash
npm run build
```

## Production Checklist

- [ ] Run `npm run build` successfully
- [ ] Test the build locally with `npm run preview` or `npx serve -s dist`
- [ ] Verify all API calls work (login, booking, etc.)
- [ ] Check browser console for errors
- [ ] Test on different browsers (Chrome, Firefox, Safari)
- [ ] Verify mobile responsiveness
- [ ] Check that routing works (refresh on any page should work)
- [ ] Test production Docker image
- [ ] Configure proper domain and SSL certificate (for real production)

## Performance Optimization

The production build includes:

✅ **Minification** - JavaScript and CSS minified
✅ **Tree-shaking** - Unused code removed
✅ **Code splitting** - Lazy loading for routes
✅ **Asset optimization** - Images and fonts optimized
✅ **Gzip compression** - Enabled in Nginx config
✅ **Cache headers** - Static assets cached for 1 year

## Nginx Features

The `nginx.conf` includes:

- **SPA routing** - All routes redirect to `index.html`
- **API proxy** - `/api` requests forwarded to backend
- **Gzip compression** - Reduced file sizes
- **Cache control** - Optimized caching for static assets
- **Security headers** - Basic security configurations

## Troubleshooting

### Issue: "Cannot GET /some-route" on refresh

**Solution:** This is already handled by the Nginx config with `try_files`. If using Python HTTP server, you'll need a proper server.

### Issue: API calls failing (CORS errors)

**Solution:** Make sure the backend allows requests from the frontend origin, or use the Nginx proxy configuration.

### Issue: 404 on assets

**Solution:** Check that the base URL in `vite.config.ts` is set correctly (should be `/` for root deployment).

### Issue: Blank page after deployment

**Solution:** 
1. Check browser console for errors
2. Verify the build created files in `dist/` folder
3. Check that `index.html` exists in the served directory
4. Verify API endpoint is correct

## Monitoring

After deployment, monitor:

```bash
# Docker logs
docker-compose logs -f frontend

# Nginx access logs (inside container)
docker exec -it booking-frontend tail -f /var/log/nginx/access.log

# Nginx error logs
docker exec -it booking-frontend tail -f /var/log/nginx/error.log
```

## Scaling for Production

For high-traffic production environments:

1. **Use CDN** - Serve static assets from CDN
2. **Load balancer** - Use multiple frontend instances behind a load balancer
3. **SSL/TLS** - Configure HTTPS with Let's Encrypt
4. **Monitoring** - Add application monitoring (e.g., Sentry, New Relic)
5. **CI/CD** - Automate build and deployment process

## Quick Commands Reference

```bash
# Development
npm run dev

# Build for production
npm run build

# Preview production build locally
npm run preview

# Serve production build
npm run serve:prod

# Build Docker image
npm run docker:build

# Run Docker container
npm run docker:run

# Full stack with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f frontend

# Rebuild and restart
docker-compose up -d --build frontend
```

---

**Ready for Production!** 🚀

Choose the deployment method that best fits your infrastructure:
- **Option 1** for quick testing
- **Option 4** for production deployments

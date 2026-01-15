# Grafana Configuration

Create a `.env` file in the frontend directory with your Grafana URL:

```env
VITE_GRAFANA_URL=http://localhost:3000
```

For production, update this to your actual Grafana server URL:

```env
VITE_GRAFANA_URL=https://grafana.your-domain.com
```

## Grafana Dashboard Setup

The Analytics & Monitoring page embeds the following Grafana dashboards:

1. **Transaction Overview** (`/d/transaction-overview`)
2. **AML Risk** (`/d/aml-risk`)
3. **Fraud Detection** (`/d/fraud-detection`)
4. **Compliance** (`/d/compliance`)
5. **Model Performance** (`/d/model-performance`)
6. **Screening** (`/d/screening`)
7. **System Performance** (`/d/system-performance`)
8. **Infrastructure Resources** (`/d/infrastructure-resources`)
9. **Thread Pools & Throughput** (`/d/thread-pools-throughput`)
10. **Circuit Breaker & Resilience** (`/d/circuit-breaker-resilience`)

## Kiosk Mode

Dashboards are embedded in kiosk mode (`?kiosk`) which hides the Grafana UI chrome for a cleaner embedded experience.

## Authentication

If your Grafana instance requires authentication, you have two options:

1. **Anonymous Access**: Configure Grafana to allow anonymous access for embedded dashboards
2. **Proxy Authentication**: Set up a proxy that handles authentication before forwarding to Grafana

See the Grafana documentation for more details on embedding dashboards.

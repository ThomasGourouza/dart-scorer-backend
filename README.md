# Dart Scorer Backend

Spring Boot API for persisting completed dart games.

## Local run without Docker

```bash
mvn spring-boot:run
```

Environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`

## Docker image

Build:

```bash
docker build -t dart-scorer-backend:local .
```

Run:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://<host>/<db>?sslmode=require&channel_binding=require' \
  -e SPRING_DATASOURCE_USERNAME='<user>' \
  -e SPRING_DATASOURCE_PASSWORD='<password>' \
  -e APP_CORS_ALLOWED_ORIGINS='https://<your-frontend-domain>' \
  dart-scorer-backend:local
```

## Render deployment

This repo contains `render.yaml` configured for Docker runtime.

## Fullstack docs

For production and one-command local fullstack setup, use:

- `../dart-scorer-infra/README.md`

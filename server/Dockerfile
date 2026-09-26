# Compilacion
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
# CGO apagado y binario estatico: es lo que permite la imagen distroless.
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /bin/api ./cmd/api

# Ejecucion
FROM gcr.io/distroless/static-debian12:nonroot
# La zona horaria hace falta: el cuadre agrupa por el dia de America/La_Paz, no
# por el dia UTC, y la imagen distroless no trae la base de zonas.
COPY --from=build /usr/local/go/lib/time/zoneinfo.zip /zoneinfo.zip
ENV ZONEINFO=/zoneinfo.zip
COPY --from=build /bin/api /api
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/api"]

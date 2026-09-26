# CheckQr — servicio

Backend del plan Básico. Su trabajo es chico y concreto:

1. **Reenviar los pagos** que captura el celular del dueño a los celulares de
   los cajeros que tienen la caja abierta.
2. **Servir las plantillas de banco firmadas**, que es lo que permite corregir
   un cambio de formato de un banco sin publicar una versión nueva de la app.
3. Sostener el **equipo** (invitaciones por QR, roles, turnos) y el **cuadre**.

Lo que **no** hace: no es la fuente de verdad de los pagos. Esa es la base local
de cada celular. Si el servicio se cae, la caja sigue funcionando: captura,
anuncia por voz y cuadra. Lo único que se pierde mientras tanto es que los
cajeros vean los pagos del dueño.

## Firma de plantillas

Se firman con Ed25519 crudo (`crypto/ed25519` de la librería estándar) sobre la
forma canónica del paquete, no sobre el JSON: el JSON depende del orden de las
claves y de los espacios, y la firma no puede depender de eso.

La forma canónica está definida en `TemplateBundle.canonico` del lado de Kotlin
y replicada en `internal/plantillas`. Hay un test de interoperabilidad a cada
lado con el mismo vector, porque si las dos implementaciones se separan el
síntoma sería que las plantillas dejan de actualizarse **en silencio**.

## Desarrollo

```bash
docker compose up -d      # Postgres 18
make migrar
make correr
```

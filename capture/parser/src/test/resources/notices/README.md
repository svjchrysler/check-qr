# Corpus de avisos

Cada archivo `.json` es un aviso de notificacion y el resultado que el parser
debe dar. Los tests de `CorpusDeAvisosTest` los corren todos contra las
plantillas empaquetadas.

Marcar `"real": true` solo cuando el aviso salio de un celular con la app del
banco instalada y una transferencia de verdad. Los casos `"real": false` son
sinteticos: sirven para probar el arnes, pero **no** prueban que la plantilla
funcione contra el banco.

Para conseguir avisos reales: instalar la variante `debug`, hacerse un pago de
Bs 1 con cada billetera, y exportar el JSON desde la pantalla de captura.

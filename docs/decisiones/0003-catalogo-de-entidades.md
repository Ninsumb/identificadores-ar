# 0003 — El nombre de entidad se resuelve en un catálogo reemplazable

- **Estado:** Aceptada
- **Fecha:** 2026-09-08

## Contexto

Los primeros tres dígitos de un CBU identifican a la entidad emisora, y en el
CVU los dígitos 4 a 7 identifican al PSP. Poder traducir esos códigos a un
nombre legible es útil, pero la nómina del BCRA cambia: entidades que se
fusionan, PSP nuevos que se autorizan, licencias que se revocan.

La pregunta era si esa tabla se embebe en la librería, se delega al consumidor,
o alguna combinación.

## Decisión

Separar dos hechos con estatus epistémico distinto:

- El **código de entidad** (`011`, `000`) se deriva de los dígitos. Es
  estructural, no puede desactualizarse, y va como propiedad del value object.
- El **nombre de la entidad** (`Banco de la Nación Argentina`) sale de una
  nómina mutable. Se resuelve **fuera** del value object, detrás de una interfaz.

```
interface CatalogoEntidades {
    fun nombre(codigo: String): String?

    companion object {
        val EMBEBIDO: CatalogoEntidades
    }
}
```

El value object **no expone** `nombreEntidad`.

### Alternativas descartadas

**Embeber la tabla en el value object.** Contaminaría con datos perecederos algo
que se promete inmutable y siempre correcto. Peor todavía: una tabla vencida no
falla de forma ruidosa, devuelve un nombre incorrecto con total confianza. Un
error silencioso y plausible es más caro que una excepción.

**Delegar la tabla enteramente al consumidor.** Cada persona que use la librería
tendría que armar la misma tabla, y la mayoría no lo haría. La funcionalidad
quedaría de adorno.

## Consecuencias

- El núcleo de la librería sigue siendo puro: sin I/O, sin dependencias, sin
  fecha de vencimiento.
- Hay una implementación embebida para que funcione sin configuración alguna.
- Quien necesite datos frescos inyecta su propia implementación —contra su base
  o contra una API— sin esperar una release.
- Un código no encontrado devuelve `null`. Es el estado de escape que todo dato
  tabular necesita, por el principio del [ADR 0002](0002-prefijo-cuit-desconocido.md).
- La tabla embebida declara su **fecha de vigencia** en el KDoc y en el README.
  Un consumidor puede evaluar si le sirve.
- Actualizar la tabla es un cambio MINOR. El mismo mecanismo cubre los códigos
  de PSP para CVU.

## Nota de implementación

La fecha de vigencia no es decorativa: es la forma de que un dato mutable
declare su propia caducidad. Sin ella, el consumidor no tiene manera de saber
si el nombre que recibió es confiable.

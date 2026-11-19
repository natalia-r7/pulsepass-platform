# Pruebas E2E manuales — PulsePass Fase 5

Pruebas HTTP de caja negra, ejecutadas a mano contra la aplicación real conectada a su base PostgreSQL
exclusiva de E2E (PRD E2E, pp. 112–137). Este documento recoge, por ahora, las **desviaciones formales**
respecto al PRD. La guía completa de preparación, secuencia, copia de variables, comparación de respuestas y
Definition of Done (§24) se completará en P5-08.

## Entregables

| Archivo | Contenido |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección oficial, Postman Collection v2.1: 124 peticiones, 22 rutas, sin scripts |
| `postman/PulsePass-local.postman_environment.json` | Plantilla del ambiente: 53 variables, sin secretos |
| `fixtures/README.md` y `fixtures/seed-e2e.sql` | Base E2E en Docker, seed y restauración |
| `checklist-e2e-manual.md` | Secuencia oficial de ejecución E0–E7 (173 acciones) |
| `registro-ejecucion-manual.md` | Bitácora de resultados (todo empieza en NOT RUN) |

## Desviaciones respecto al PRD

### D-01 — Cliente de ejecución: Bruno en lugar de Postman

**Qué dice el PRD.** La ejecución manual se hace «desde Postman con botón Send» (§1, Decisión MVP) y
«exclusivamente desde la interfaz de Postman» (§16.1).

**Qué se hace.** La colección y el ambiente oficiales siguen siendo los archivos **Postman Collection v2.1** de
`postman/` (FR-E2E-001, DoD §24), sin ningún cambio. Lo único que cambia es el **cliente** con el que se
importan y se envían las peticiones: **Bruno**, una aplicación de escritorio de código abierto. El procedimiento
no cambia: cada petición se envía a mano, en el orden del checklist, y se revisa visualmente. Siguen prohibidos
los scripts, `pm.test`, Newman y Postman CLI (§16.4); también el **Collection Runner de Bruno**.

**Por qué.** Las pruebas exigen que ningún secreto (contraseñas y tokens) salga del equipo (FR-E2E-013,
NFR-E2E-002) y la acción A-01 del checklist exige trabajar solo en local, sin sincronizar la colección ni el
ambiente con la nube:

- El cliente ligero de Postman, sin iniciar sesión, no permite importar colecciones ni usar ambientes y
  variables, de las que depende toda la colección (`{{baseUrl}}`, tokens y códigos).
- Con una cuenta de Postman, la colección, el ambiente y posiblemente el historial de peticiones (con
  contraseñas en los cuerpos y tokens en las cabeceras) se guardan en el espacio de trabajo en la nube, lo que
  no permite garantizar A-01.
- Bruno importa Postman Collection v2.1 y ambientes de Postman y funciona sin cuenta y sin conexión. Según su
  documentación, los valores secretos se guardan cifrados en un almacén local, fuera del archivo del ambiente;
  A-12 comprobará que no aparecen en el archivo del ambiente.

**Condiciones de uso** (checklist, A-01, A-02 y A-12):

1. Bruno lo instala la persona que ejecuta; se anota su versión en la bitácora.
2. La colección de Bruno se crea en una carpeta **fuera del repositorio**; nunca se abre `tests/e2e/` como
   colección de Bruno. Tras importar, `git status` debe seguir limpio.
3. Antes de ejecutar se comprueba la importación: 10 carpetas y 124 peticiones en orden, autenticación,
   cuerpos (incluido el JSON truncado de TC-HTTP-003), descripciones y ausencia de scripts. Si algo no
   coincide, se registra BLOCKED.
4. Las 20 variables `secret` se marcan como secretas en Bruno antes de introducir ningún valor, y se verifica
   que el archivo del ambiente no contiene los valores.

**Impacto.** Ninguno sobre el backend (F5-H), la colección, el ambiente ni `seed-e2e.sql`; solo se actualiza la
redacción de `fixtures/README.md`. La bitácora registra el cliente en su cabecera. Si la importación en Bruno
no reproduce la colección fielmente, los casos afectados quedan BLOCKED y la desviación se revisa antes de
continuar.

**Aprobación.** Aprobada en la revisión del proyecto el 19 de noviembre de 2026, antes de la ejecución
oficial.

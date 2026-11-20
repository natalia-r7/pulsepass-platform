/**
 * Traducción al español de los mensajes conocidos del backend (la API responde en inglés).
 * Se compara por prefijo; el detalle variable (código de ticket, evento…) se conserva entre paréntesis.
 * Un mensaje desconocido se muestra tal cual, para no perder información.
 */
const TRADUCCIONES: ReadonlyArray<readonly [prefijo: string, texto: string]> = [
  ['Only PAID tickets can be used', 'Este ticket no se puede usar: ya fue utilizado o no está pagado'],
  ['Only PAID tickets can be cancelled', 'Solo se pueden cancelar tickets pagados'],
  ['Only DRAFT events can be published', 'Solo se pueden publicar eventos en borrador'],
  ['Artist already associated with event', 'El artista ya está asociado a este evento'],
  ['Artists cannot be added to', 'No se pueden asociar artistas a este evento por su estado'],
  ['Tickets can only be purchased for PUBLISHED events', 'Solo se pueden comprar entradas de eventos publicados'],
  ['User does not meet minimum age', 'No cumples la edad mínima requerida para este evento'],
  ['Event has no available capacity', 'No quedan entradas disponibles para este evento'],
  ['Event already exists', 'Ya existe un evento con ese código'],
  ['Event date must be in the future', 'La fecha del evento debe ser futura'],
  ['Minimum age must be greater than or equal to 0', 'La edad mínima debe ser mayor o igual a 0'],
  ['Birth date cannot be in the future', 'La fecha de nacimiento no puede ser futura'],
  ['Venue is not active', 'La sede no está activa'],
  ['Venue not found', 'La sede no existe'],
  ['Event not found', 'El evento no existe'],
  ['Ticket not found', 'El ticket no existe'],
  ['Artist not found', 'El artista no existe'],
  ['User not found', 'El usuario no existe'],
  ['User has no profile', 'El usuario no tiene perfil'],
  ['User is not active', 'La cuenta de usuario no está activa'],
  ['Email already exists', 'Ya existe una cuenta con ese correo'],
  ['Username already exists', 'Ya existe una cuenta con ese nombre de usuario'],
  ['Invalid credentials', 'Credenciales inválidas'],
  ['Authentication is required', 'Debes iniciar sesión para continuar'],
  ['Access denied', 'No tienes permisos para realizar esta acción'],
  ['Validation failed', 'Revisa los datos del formulario'],
  ['Malformed JSON request', 'La solicitud no tiene un formato válido'],
]

export function traducirMensaje(mensaje: string): string {
  for (const [prefijo, texto] of TRADUCCIONES) {
    if (mensaje.startsWith(prefijo)) {
      const resto = mensaje.slice(prefijo.length).replace(/^[\s:.]+/, '').trim()
      return resto ? `${texto} (${resto}).` : `${texto}.`
    }
  }
  return mensaje
}

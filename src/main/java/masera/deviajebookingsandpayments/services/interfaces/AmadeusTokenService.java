package masera.deviajebookingsandpayments.services.interfaces;

import org.springframework.stereotype.Service;

/**
 * Interfaz para el servicio de autenticación de Amadeus.
 */
@Service
public interface AmadeusTokenService {

  /**
   * Obtiene el token de acceso de Amadeus.
   * Si el actual es válido, lo devuelve.
   * Si no es válido o no existe, lo solicita de nuevo.
   *
   * @return el token de acceso para la API de Amadeus.
   */
  String getToken();
}

package masera.deviajebookingsandpayments.services.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.clients.AmadeusAuthClient;
import masera.deviajebookingsandpayments.dtos.AmadeusTokenResponse;
import masera.deviajebookingsandpayments.services.interfaces.AmadeusTokenService;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de autenticación de Amadeus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmadeusTokenServiceImpl implements AmadeusTokenService {

  private final AmadeusAuthClient amadeusAuthClient;

  private String currentToken;

  private LocalDateTime tokenExpiration;

  /**
   * Obtiene el token de autenticación de Amadeus.
   * Si el token actual ha expirado, se solicita uno nuevo.
   *
   * @return el token de autenticación.
   */
  // synchronized se usa aquí para evitar que múltiples hilos
  // intenten obtener un nuevo token al mismo tiempo
  @Override
  public synchronized String getToken() {

    if (isTokenValid()) {
      return currentToken;
    }

    log.info("Solicitando nuevo token de Amadeus");

    try {
      // se coloca block() para esperar la respuesta
      AmadeusTokenResponse response = amadeusAuthClient.getAmadeusToken().block();

      if (response != null && response.getAccessToken() != null) {
        currentToken = response.getAccessToken();

        // Se le resta 60 segundos a la expiración para evitar que expire mientras se usa
        tokenExpiration = LocalDateTime.now().plusSeconds(response.getExpiresIn() - 60);

        log.info("Token de Amadeus obtenido, válido hasta: {}", tokenExpiration);
        return currentToken;
      } else {
        throw new RuntimeException("No se pudo obtener el token de Amadeus");
      }

    } catch (Exception e) {

      log.error("Error al obtener el token de Amadeus: {}", e.getMessage());
      throw new RuntimeException("Error al obtener el token de Amadeus", e);
    }
  }

  private boolean isTokenValid() {
    return currentToken != null && tokenExpiration != null
            && LocalDateTime.now().isBefore(tokenExpiration);
  }
}

package masera.deviajebookingsandpayments.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.dtos.additional.UserBasicInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente para consumir el microservicio de users and auth.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserClient {

  private final WebClient webClient;

  @Value("${services.users.url:http://localhost:8080}")
  private String usersServiceUrl;

  /**
   * Obtiene información básica de un usuario por su ID.
   *
   * @param userId ID del usuario
   * @return información básica del usuario
   */
  public Mono<UserBasicInfoDto> getUserBasicInfo(Integer userId) {
    if (userId == null) {
      return Mono.empty();
    }

    log.info("Obteniendo información del usuario con ID: {}", userId);

    return webClient
            .get()
            .uri(usersServiceUrl + "/api/users/" + userId)
            .retrieve()
            .bodyToMono(UserBasicInfoDto.class)
            .doOnSuccess(user -> {
              if (user != null) {
                log.info("Usuario obtenido exitosamente: {}", user.getUserName());
              }
            })
            .doOnError(error -> log.error("Error al obtener usuario con ID {}: {}",
                    userId, error.getMessage()))
            .onErrorResume(error -> {
              log.warn("No se pudo obtener información del usuario {}, retornando vacío", userId);
              return Mono.empty();
            });
  }
}
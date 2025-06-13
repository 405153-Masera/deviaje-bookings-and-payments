package masera.deviajebookingsandpayments.services.impl;


import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.PaymentRefund;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mercadopago.resources.payment.PaymentTransactionDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.PagoConfig;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementación del servicio de pagos con Mercado Pago.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PagoConfig pagoConfig;

  /**
   * Inicializa la configuración de Mercado Pago.
   */
  private void initMercadoPagoConfig() {
    MercadoPagoConfig.setAccessToken(pagoConfig.getAccessToken());
  }

  @Override
  @Transactional
  public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest) {
    log.info("Procesando pago por {} {}", paymentRequest.getAmount(), paymentRequest.getCurrency());

    if (paymentRequest.getPaymentToken() == null || paymentRequest.getPaymentToken().isEmpty()) {
      return PaymentResponseDto.rejected(
              "MISSING_TOKEN",
              "El token de pago es obligatorio"
      );
    }

    try {
      initMercadoPagoConfig();
      return processDirectPayment(paymentRequest);

    } catch (MPApiException e) {
      log.error("Error específico de Mercado Pago API", e);
      return PaymentResponseDto.rejected(
              "MP_API_ERROR",
              "Error de Mercado Pago: " + e.getMessage() + " (Código: " + e.getStatusCode() + ")"
      );
    } catch (Exception e) {
      log.error("Error general al procesar pago", e);
      return PaymentResponseDto.rejected(
              "PROCESSING_ERROR",
              "Error al procesar el pago: " + e.getMessage()
      );
    }
  }

  /**
   * Procesa un pago directo con token.
   */
  private PaymentResponseDto processDirectPayment(PaymentRequestDto paymentRequest)
                                              throws MPException, MPApiException {

    PaymentClient paymentClient = new PaymentClient();

    // Crear el builder para PaymentPayerRequest si tenemos datos del pagador
    PaymentPayerRequest.PaymentPayerRequestBuilder payerBuilder = null;
    log.info("PAYER DATOS: {}", paymentRequest.getPayer());
    if (paymentRequest.getPayer() != null && paymentRequest.getPayer().getEmail() != null) {
      payerBuilder = PaymentPayerRequest.builder()
              .email(paymentRequest.getPayer().getEmail());

      log.info("DNI: {}", paymentRequest.getPayer().getIdentification());
      // SOLO AGREGAR DNI SI ESTÁ DISPONIBLE
      if (paymentRequest.getPayer().getIdentification() != null
              && paymentRequest.getPayer().getIdentificationType() != null) {
        IdentificationRequest identification = IdentificationRequest.builder()
                .type(paymentRequest.getPayer().getIdentificationType())
                .number(paymentRequest.getPayer().getIdentification())
                .build();

        payerBuilder.identification(identification);
      }
    }

    // Crear el request de pago usando el builder pattern
    PaymentCreateRequest.PaymentCreateRequestBuilder paymentBuilder = PaymentCreateRequest.builder()
            .transactionAmount(paymentRequest.getAmount())
            .token(paymentRequest.getPaymentToken())
            .description(paymentRequest.getDescription() != null
                    ? paymentRequest.getDescription() : "Reserva en DeViaje")
            .installments(paymentRequest.getInstallments())
            .paymentMethodId(paymentRequest.getPaymentMethod());

    if (payerBuilder != null) {
      paymentBuilder.payer(payerBuilder.build());
    }
    PaymentCreateRequest paymentCreateRequest = paymentBuilder.build();

    log.info("Enviando request a Mercado Pago: {}", paymentCreateRequest);

    // Procesar el pago
    try {
      com.mercadopago.resources.payment.Payment createdPayment =
              paymentClient.create(paymentCreateRequest);

      // Log de respuesta exitosa
      log.info("Pago procesado exitosamente: ID={}, Status={}",
              createdPayment.getId(), createdPayment.getStatus());

      // Guardar en nuestra base de datos
      masera.deviajebookingsandpayments.entities.Payment paymentEntity =
              masera.deviajebookingsandpayments.entities.Payment.builder()
                      .amount(paymentRequest.getAmount())
                      .currency(paymentRequest.getCurrency())
                      .method(paymentRequest.getPaymentMethod())
                      .paymentProvider("MERCADO_PAGO")
                      .externalPaymentId(createdPayment.getId().toString())
                      .status(mapMercadoPagoStatus(createdPayment.getStatus()))
                      .date(LocalDateTime.now())
                      .build();

      masera.deviajebookingsandpayments.entities.Payment savedPayment =
              paymentRepository.save(paymentEntity);

      if ("approved".equals(createdPayment.getStatus())) {
        return PaymentResponseDto.approved(
                savedPayment.getId(),
                createdPayment.getId().toString(),
                paymentRequest.getAmount(),
                paymentRequest.getCurrency()
        );
      } else {
        return PaymentResponseDto.builder()
                .id(savedPayment.getId())
                .externalPaymentId(createdPayment.getId().toString())
                .amount(paymentRequest.getAmount())
                .currency(paymentRequest.getCurrency())
                .status(createdPayment.getStatus().toUpperCase())
                .errorCode(createdPayment.getStatusDetail())
                .errorMessage("Estado del pago: " + createdPayment.getStatus())
                .date(LocalDateTime.now())
                .build();
      }
    } catch (MPApiException e) {
      // Log detallado del error de Mercado Pago
      log.error("Error de API de Mercado Pago:");
      log.error("Status Code: {}", e.getStatusCode());
      log.error("Error: {}", e.getApiResponse().getContent());
      log.error("Message: {}", e.getMessage());
      log.error("Cause: {}", e.getCause());

      // Re-lanzar la excepción para que sea manejada por el método padre
      throw e;
    }
  }

  @Override
  @Transactional
  public PaymentResponseDto refundPayment(Long paymentId) {
    log.info("Procesando reembolso para pago ID: {}", paymentId);

    Optional<masera.deviajebookingsandpayments.entities.Payment> paymentOpt =
            paymentRepository.findById(paymentId);

    if (paymentOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }

    masera.deviajebookingsandpayments.entities.Payment payment = paymentOpt.get();

    // Verificar si ya está reembolsado
    if (masera.deviajebookingsandpayments.entities
            .Payment.PaymentStatus.REFUNDED.equals(payment.getStatus())) {
      return PaymentResponseDto.builder()
              .id(payment.getId())
              .externalPaymentId(payment.getExternalPaymentId())
              .amount(payment.getAmount())
              .currency(payment.getCurrency())
              .status("REFUNDED")
              .date(payment.getDate())
              .errorMessage("El pago ya había sido reembolsado")
              .build();
    }

    try {
      initMercadoPagoConfig();
      PaymentRefundClient refundClient = new PaymentRefundClient();

      // Convertir ID externo a long
      Long mpPaymentId = Long.parseLong(payment.getExternalPaymentId());

      // Procesar reembolso total o parcial
      PaymentRefund refund = refundClient.refund(mpPaymentId, payment.getAmount());

      // Actualizar estado en BD
      payment.setStatus(masera.deviajebookingsandpayments.entities.Payment.PaymentStatus.REFUNDED);
      paymentRepository.save(payment);

      return PaymentResponseDto.refunded(
              payment.getId(),
              payment.getExternalPaymentId(),
              payment.getAmount()
      );
    } catch (MPException | MPApiException e) {
      log.error("Error al procesar reembolso con Mercado Pago", e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al procesar reembolso: " + e.getMessage()
      );
    }
  }

  @Override
  @Transactional
  public PaymentResponseDto processRefundForBooking(Long bookingId) {
    log.info("Procesando reembolso para reserva ID: {}", bookingId);

    // Buscar todos los pagos de la reserva
    List<masera.deviajebookingsandpayments.entities.Payment> payments =
            paymentRepository.findByBookingId(bookingId);

    if (payments.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
              "No se encontraron pagos para la reserva");
    }

    // Procesar reembolso del último pago aprobado
    for (masera.deviajebookingsandpayments.entities.Payment payment : payments) {
      if (masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.APPROVED.equals(payment.getStatus())) {

        return refundPayment(payment.getId());
      }
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No hay pagos aprobados para reembolsar");
  }

  @Override
  public PaymentResponseDto checkPaymentStatus(Long paymentId) {
    log.info("Verificando estado de pago ID: {}", paymentId);

    Optional<masera.deviajebookingsandpayments.entities.Payment> paymentOpt =
            paymentRepository.findById(paymentId);

    if (paymentOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }

    masera.deviajebookingsandpayments.entities.Payment payment = paymentOpt.get();

    try {
      // Si el pago está pendiente, verificamos su estado actual en Mercado Pago
      if (masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.PENDING.equals(payment.getStatus())) {

        return checkExternalPaymentStatus(payment.getExternalPaymentId());
      }

      // Si ya está en un estado final, simplemente devolvemos la información
      return convertToPaymentResponseDto(payment);
    } catch (Exception e) {
      log.error("Error al verificar estado de pago", e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al verificar estado de pago: " + e.getMessage()
      );
    }
  }

  @Override
  public PaymentResponseDto checkExternalPaymentStatus(String externalPaymentId) {
    log.info("Verificando estado de pago externo ID: {}", externalPaymentId);

    try {
      initMercadoPagoConfig();
      PaymentClient paymentClient = new PaymentClient();

      Long mpPaymentId = Long.parseLong(externalPaymentId);
      com.mercadopago.resources.payment.Payment mpPayment = paymentClient.get(mpPaymentId);

      // Buscar el pago en nuestra BD
      Optional<masera.deviajebookingsandpayments.entities.Payment> paymentOpt =
              paymentRepository.findByExternalPaymentId(externalPaymentId);

      if (paymentOpt.isPresent()) {
        masera.deviajebookingsandpayments.entities.Payment payment = paymentOpt.get();

        // Actualizar estado si ha cambiado
        masera.deviajebookingsandpayments.entities.Payment.PaymentStatus newStatus =
                mapMercadoPagoStatus(mpPayment.getStatus());

        if (!payment.getStatus().equals(newStatus)) {
          payment.setStatus(newStatus);
          paymentRepository.save(payment);
        }

        return convertToPaymentResponseDto(payment);
      } else {
        // Si no lo tenemos en nuestra BD, devolver información básica
        return PaymentResponseDto.builder()
                .externalPaymentId(externalPaymentId)
                .status(mpPayment.getStatus().toUpperCase())
                .build();
      }
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "ID de pago inválido"
      );
    } catch (MPException | MPApiException e) {
      log.error("Error al verificar pago con Mercado Pago", e);
      throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              "Error al verificar estado de pago: " + e.getMessage()
      );
    }
  }

  /**
   * Mapea el estado de Mercado Pago a nuestro enum de estados.
   */
  private masera.deviajebookingsandpayments.entities
          .Payment.PaymentStatus mapMercadoPagoStatus(String mpStatus) {

    if (mpStatus == null) {
      return masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.PENDING;
    }

    return switch (mpStatus.toLowerCase()) {
      case "approved" -> masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.APPROVED;
      case "rejected" -> masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.REJECTED;
      case "cancelled" -> masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.CANCELLED;
      case "refunded" -> masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.REFUNDED;
      default -> masera.deviajebookingsandpayments.entities
              .Payment.PaymentStatus.PENDING;
    };
  }

  /**
   * Convierte una entidad Payment a un DTO PaymentResponseDto.
   */
  private PaymentResponseDto convertToPaymentResponseDto(
          masera.deviajebookingsandpayments.entities.Payment payment) {

    return PaymentResponseDto.builder()
            .id(payment.getId())
            .externalPaymentId(payment.getExternalPaymentId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .method(payment.getMethod())
            .paymentProvider(payment.getPaymentProvider())
            .date(payment.getDate())
            .build();
  }
}

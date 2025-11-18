package masera.deviajebookingsandpayments.services.impl;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import masera.deviajebookingsandpayments.configs.PagoConfig;
import masera.deviajebookingsandpayments.dtos.payments.PaymentRequestDto;
import masera.deviajebookingsandpayments.dtos.responses.PaymentResponseDto;
import masera.deviajebookingsandpayments.entities.PaymentEntity;
import masera.deviajebookingsandpayments.exceptions.MercadoPagoException;
import masera.deviajebookingsandpayments.repositories.PaymentRepository;
import masera.deviajebookingsandpayments.services.interfaces.PaymentService;
import masera.deviajebookingsandpayments.utils.ErrorHandler;
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

  private final ErrorHandler errorHandler;

  /**
   * Inicializa la configuración de Mercado Pago.
   */
  private void initMercadoPagoConfig() {
    MercadoPagoConfig.setAccessToken(pagoConfig.getAccessToken());
  }

  @Override
  public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest) {
    log.info("Procesando pago por {} {}", paymentRequest.getAmount(), paymentRequest.getCurrency());

    try {
      initMercadoPagoConfig();
      return processDirectPayment(paymentRequest);

    } catch (MPApiException e) {
      log.error("Error específico de Mercado Pago API", e);
      throw errorHandler.handleMercadoPagoError(e);
    }  catch (MPException e) {
      log.error("Error general de Mercado Pago", e);
      throw errorHandler.handleMercadoPagoError(e);

    } catch (MercadoPagoException e) {
      throw e;

    } catch (Exception e) {
      log.error("Error inesperado al procesar pago", e);
      throw new MercadoPagoException(
              "— 500 INTERNAL SERVER ERROR - Error inesperado al procesar el pago",
              500,
              e
      );
    }
  }

  /**
   * Procesa un pago directo con token.
   *
   * @param paymentRequest datos del pago
   * @return PaymentResponseDto con el resultado
   * @throws MPApiException si hay error de API
   * @throws MPException cuando hay error general
   */
  private PaymentResponseDto processDirectPayment(PaymentRequestDto paymentRequest)
                                              throws MPException, MPApiException {

    PaymentPayerRequest.PaymentPayerRequestBuilder payerBuilder = null;
    if (paymentRequest.getPayer() != null && paymentRequest.getPayer().getEmail() != null) {
      payerBuilder = PaymentPayerRequest.builder()
              .email(paymentRequest.getPayer().getEmail());

      if (paymentRequest.getPayer().getIdentification() != null
              && paymentRequest.getPayer().getIdentificationType() != null) {
        IdentificationRequest identification = IdentificationRequest.builder()
                .type(paymentRequest.getPayer().getIdentificationType())
                .number(paymentRequest.getPayer().getIdentification())
                .build();

        payerBuilder.identification(identification);
      }
    }

    PaymentCreateRequest.PaymentCreateRequestBuilder paymentBuilder = PaymentCreateRequest.builder()
            .transactionAmount(paymentRequest.getAmount())
            .token(paymentRequest.getPaymentToken())
            .description(paymentRequest.getDescription() != null
                    ? paymentRequest.getDescription() : "Reserva en DeViaje")
            .installments(paymentRequest.getInstallments())
            .paymentMethodId(paymentRequest.getPaymentMethod())
            .externalReference(paymentRequest.getBookingId() != null
                    ? paymentRequest.getBookingId().toString()
                    : null);

    if (payerBuilder != null) {
      paymentBuilder.payer(payerBuilder.build());
    }
    PaymentCreateRequest paymentCreateRequest = paymentBuilder.build();

    PaymentClient paymentClient = new PaymentClient();
    log.info("Enviando request a Mercado Pago: {}", paymentCreateRequest);
    Payment createdPayment =
              paymentClient.create(paymentCreateRequest);

    log.info("Pago procesado exitosamente: ID={}, Status={}",
            createdPayment.getId(), createdPayment.getStatus());

    PaymentEntity paymentEntity =
            PaymentEntity.builder()
                    .amount(paymentRequest.getAmount())
                    .currency(paymentRequest.getCurrency())
                    .method(paymentRequest.getPaymentMethod())
                    .paymentProvider("MERCADO_PAGO")
                    .type(paymentRequest.getType())
                    .externalPaymentId(createdPayment.getId().toString())
                    .status(mapMercadoPagoStatus(createdPayment.getStatus()))
                    .date(LocalDateTime.now())
                    .build();

    PaymentEntity savedPaymentEntity =
            paymentRepository.save(paymentEntity);

    if ("approved".equals(createdPayment.getStatus())) {
      return PaymentResponseDto.approved(
              savedPaymentEntity.getId(),
              createdPayment.getId().toString(),
              paymentRequest.getAmount(),
              paymentRequest.getCurrency()
      );
    } else {
      String statusDetail = createdPayment.getStatusDetail();
      String errorMessage = getPaymentErrorMessage(statusDetail);

      int statusCode = mapPaymentStatusToHttpCode(createdPayment.getStatus());

      throw new MercadoPagoException(
              String.format("— %d %s - MercadoPago Error [%s]: %s",
                      statusCode,
                      getHttpStatusText(statusCode),
                      statusDetail != null ? statusDetail : "PAYMENT_REJECTED",
                      errorMessage),
              statusCode,
              statusDetail
      );
    }
  }

  @Override
  @Transactional
  public PaymentResponseDto refundPayment(Long paymentId) {
    log.info("Procesando reembolso para pago ID: {}", paymentId);

    Optional<PaymentEntity> paymentOpt =
            paymentRepository.findById(paymentId);

    if (paymentOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }

    PaymentEntity paymentEntity = paymentOpt.get();

    // Verificar si ya está reembolsado
    if (PaymentEntity.PaymentStatus.REFUNDED.equals(paymentEntity.getStatus())) {
      return PaymentResponseDto.builder()
              .id(paymentEntity.getId())
              .externalPaymentId(paymentEntity.getExternalPaymentId())
              .amount(paymentEntity.getAmount())
              .currency(paymentEntity.getCurrency())
              .status("REFUNDED")
              .date(paymentEntity.getDate())
              .errorMessage("El pago ya había sido reembolsado")
              .build();
    }

    try {
      initMercadoPagoConfig();
      PaymentRefundClient refundClient = new PaymentRefundClient();

      // Convertir ID externo a long
      Long mpPaymentId = Long.parseLong(paymentEntity.getExternalPaymentId());

      // Procesar reembolso
      refundClient.refund(mpPaymentId, paymentEntity.getAmount());

      // Actualizar estado en BD
      paymentEntity.setStatus(PaymentEntity.PaymentStatus.REFUNDED);
      paymentRepository.save(paymentEntity);

      return PaymentResponseDto.refunded(
              paymentEntity.getId(),
              paymentEntity.getExternalPaymentId(),
              paymentEntity.getAmount()
      );

    } catch (MPApiException e) {
      log.error("Error de API al procesar reembolso con Mercado Pago", e);
      throw errorHandler.handleMercadoPagoError(e);

    } catch (MPException e) {
      log.error("Error general al procesar reembolso con Mercado Pago", e);
      throw errorHandler.handleMercadoPagoError(e);

    } catch (NumberFormatException e) {
      throw new MercadoPagoException(
              "— 400 BAD REQUEST - ID de pago externo inválido",
              400,
              "INVALID_PAYMENT_ID"
      );

    } catch (Exception e) {
      log.error("Error inesperado al procesar reembolso", e);
      throw new MercadoPagoException(
              "— 500 INTERNAL SERVER ERROR - Error al procesar reembolso",
              500,
              e
      );
    }
  }

  @Override
  @Transactional
  public PaymentResponseDto processRefundForBooking(Long bookingId) {
    log.info("Procesando reembolso para reserva ID: {}", bookingId);

    // Buscar todos los pagos de la reserva
    List<PaymentEntity> paymentEntities =
            paymentRepository.findByBookingEntityId(bookingId);

    if (paymentEntities.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
              "No se encontraron pagos para la reserva");
    }

    // Procesar reembolso del último pago aprobado
    for (PaymentEntity paymentEntity : paymentEntities) {
      if (PaymentEntity.PaymentStatus.APPROVED.equals(paymentEntity.getStatus())) {

        return refundPayment(paymentEntity.getId());
      }
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "No hay pagos aprobados para reembolsar");
  }

  @Override
  public PaymentResponseDto checkPaymentStatus(Long paymentId) {
    log.info("Verificando estado de pago ID: {}", paymentId);

    Optional<PaymentEntity> paymentOpt =
            paymentRepository.findById(paymentId);

    if (paymentOpt.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado");
    }

    PaymentEntity paymentEntity = paymentOpt.get();

    try {
      // Si el pago está pendiente, verificamos su estado actual en Mercado Pago
      if (PaymentEntity.PaymentStatus.PENDING.equals(paymentEntity.getStatus())) {

        return checkExternalPaymentStatus(paymentEntity.getExternalPaymentId());
      }

      return convertToPaymentResponseDto(paymentEntity);
    } catch (MercadoPagoException e) {
      throw e;

    } catch (Exception e) {
      log.error("Error al verificar estado de pago", e);
      throw new MercadoPagoException(
              "— 500 INTERNAL SERVER ERROR - Error al verificar estado de pago",
              500,
              e
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

      Optional<PaymentEntity> paymentOpt =
              paymentRepository.findByExternalPaymentId(externalPaymentId);

      if (paymentOpt.isPresent()) {
        PaymentEntity paymentEntity = paymentOpt.get();

        PaymentEntity.PaymentStatus newStatus =
                mapMercadoPagoStatus(mpPayment.getStatus());

        if (!paymentEntity.getStatus().equals(newStatus)) {
          paymentEntity.setStatus(newStatus);
          paymentRepository.save(paymentEntity);
        }

        return convertToPaymentResponseDto(paymentEntity);
      } else {

        return PaymentResponseDto.builder()
                .externalPaymentId(externalPaymentId)
                .status(mpPayment.getStatus().toUpperCase())
                .build();
      }
    } catch (NumberFormatException e) {
      throw new MercadoPagoException(
              "— 400 BAD REQUEST - ID de pago externo inválido",
              400,
              "INVALID_PAYMENT_ID"
      );

    } catch (MPApiException e) {
      log.error("Error de API al verificar pago con Mercado Pago", e);
      throw errorHandler.handleMercadoPagoError(e);

    } catch (MPException e) {
      log.error("Error general al verificar pago con Mercado Pago", e);
      throw errorHandler.handleMercadoPagoError(e);

    } catch (Exception e) {
      log.error("Error inesperado al verificar pago", e);
      throw new MercadoPagoException(
              "— 500 INTERNAL SERVER ERROR - Error al verificar estado de pago",
              500,
              e
      );
    }
  }

  /**
   * Mapea el estado de Mercado Pago a nuestro enum de estados.
   */
  private PaymentEntity.PaymentStatus mapMercadoPagoStatus(String mpStatus) {

    if (mpStatus == null) {
      return PaymentEntity.PaymentStatus.PENDING;
    }

    return switch (mpStatus.toLowerCase()) {
      case "approved" -> PaymentEntity.PaymentStatus.APPROVED;
      case "rejected" -> PaymentEntity.PaymentStatus.REJECTED;
      case "cancelled" -> PaymentEntity.PaymentStatus.CANCELLED;
      case "refunded" -> PaymentEntity.PaymentStatus.REFUNDED;
      default -> PaymentEntity.PaymentStatus.PENDING;
    };
  }

  /**
   * Convierte una entidad Payment a un DTO PaymentResponseDto.
   */
  private PaymentResponseDto convertToPaymentResponseDto(
          PaymentEntity paymentEntity) {

    return PaymentResponseDto.builder()
            .id(paymentEntity.getId())
            .externalPaymentId(paymentEntity.getExternalPaymentId())
            .amount(paymentEntity.getAmount())
            .currency(paymentEntity.getCurrency())
            .status(paymentEntity.getStatus().name())
            .method(paymentEntity.getMethod())
            .paymentProvider(paymentEntity.getPaymentProvider())
            .date(paymentEntity.getDate())
            .build();
  }

  /**
   * Mapea el status de pago de MP a código HTTP.
   */
  private int mapPaymentStatusToHttpCode(String paymentStatus) {
    if (paymentStatus == null) {
      return 402;
    }

    return switch (paymentStatus.toLowerCase()) {
      case "rejected" -> 402; // Payment Required
      case "cancelled" -> 400; // Bad Request
      case "pending" -> 202;   // Accepted (procesando)
      default -> 402;
    };
  }

  /**
   * Obtiene mensaje amigable según el status detail de MP.
   */
  private String getPaymentErrorMessage(String statusDetail) {
    if (statusDetail == null) {
      return "Pago rechazado";
    }

    return switch (statusDetail) {
      case "cc_rejected_insufficient_amount" ->
              "Fondos insuficientes en la tarjeta";
      case "cc_rejected_bad_filled_security_code" ->
              "Código de seguridad inválido";
      case "cc_rejected_bad_filled_date" ->
              "Fecha de vencimiento inválida";
      case "cc_rejected_bad_filled_other" ->
              "Datos de tarjeta incorrectos";
      case "cc_rejected_call_for_authorize" ->
              "Debe autorizar el pago con su banco";
      case "cc_rejected_card_disabled" ->
              "Tarjeta deshabilitada";
      case "cc_rejected_duplicated_payment" ->
              "Pago duplicado";
      case "cc_rejected_high_risk" ->
              "Pago rechazado por seguridad";
      case "cc_rejected_invalid_installments" ->
              "Cantidad de cuotas inválida";
      case "cc_rejected_max_attempts" ->
              "Se excedió el número de intentos permitidos";
      default -> "Pago rechazado: " + statusDetail;
    };
  }

  /**
   * Obtiene el texto del status HTTP.
   */
  private String getHttpStatusText(int statusCode) {
    return switch (statusCode) {
      case 400 -> "BAD REQUEST";
      case 402 -> "PAYMENT REQUIRED";
      case 202 -> "ACCEPTED";
      default -> "ERROR";
    };
  }
}
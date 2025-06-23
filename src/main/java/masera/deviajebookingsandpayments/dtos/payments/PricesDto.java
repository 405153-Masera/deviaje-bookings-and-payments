package masera.deviajebookingsandpayments.dtos.payments;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa una solicitud de búsqueda de vuelos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricesDto {


  private BigDecimal totalAmount; // Suma de precio de apis + commission + taxes - discount

  private BigDecimal grandTotal = BigDecimal.ZERO; // Precio de la API de Amadeus.
  private BigDecimal net = BigDecimal.ZERO; // Precio de la API de hoteles

  private BigDecimal commission = BigDecimal.ZERO; // Comisión aplicada
  private BigDecimal discount = BigDecimal.ZERO; // Descuento aplicado

  private BigDecimal taxesFlight = BigDecimal.ZERO; // Impuestos aplicados para vuelos
  private BigDecimal taxesHotel = BigDecimal.ZERO; // Impuestos aplicados para hoteles

  private String currency = "ARS"; // Moneda por defecto
}

-- Base de Datos para Microservicio de Reservas y Transacciones

CREATE DATABASE IF NOT EXISTS deviaje_bookings_payments;

USE deviaje_bookings_payments;

-- Tabla principal de reservas unificada
CREATE TABLE bookings (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          client_id INT,
                          agent_id INT,
                          status VARCHAR(20) NOT NULL,   -- PENDING, CONFIRMED, CANCELLED, COMPLETED
                          type VARCHAR(20) NOT NULL,     -- FLIGHT, HOTEL, PACKAGE
                          total_amount DECIMAL(10,2) NOT NULL, -- SUMA de precio de api + commission + taxes - discount
                          commission DECIMAL(10,2) DEFAULT 0,
                          discount DECIMAL(10,2) DEFAULT 0,
                          taxes DECIMAL(10,2) DEFAULT 0,
                          currency VARCHAR(3) DEFAULT 'ARS',
                          phone VARCHAR(20),
                          email VARCHAR(100),
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT
);

-- Tabla de reservas de vuelos (datos mínimos)
CREATE TABLE flights_bookings (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  booking_id BIGINT NOT NULL,
                                  external_id VARCHAR(50), -- ID de Amadeus
                                  origin VARCHAR(3),
                                  destination VARCHAR(3),
                                  departure_date VARCHAR(25),
                                  return_date VARCHAR(25), -- Para vuelos de ida y vuelta
                                  carrier VARCHAR(2), -- Aerolínea principal (código IATA)
                                  adults INT,
                                  children INT,
                                  infants INT,
                                  total_price DECIMAL(10,2), -- Grand total de Amadeus
                                  taxes DECIMAL(10,2),
                                  currency VARCHAR(3),
                                  cancellation_from DATE,
                                  cancellation_amount DECIMAL(10,2) DEFAULT 0,
                                  created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  created_user INT,
                                  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de reservas de hoteles (datos mínimos)
CREATE TABLE hotels_bookings (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 booking_id BIGINT NOT NULL,
                                 external_id VARCHAR(50), -- Reference de HotelBeds
                                 hotel_name VARCHAR(100),
                                 destination_name VARCHAR(50),
                                 check_in_date DATE,
                                 check_out_date DATE,
                                 number_of_nights INT,
                                 number_of_rooms INT,
                                 adults INT,
                                 children INT,
                                 total_price DECIMAL(10,2), -- net de hotelsbeds
                                 taxes DECIMAL(10,2),
                                 currency VARCHAR(3),
                                 cancellation_from DATE,
                                 cancellation_amount DECIMAL(10,2) DEFAULT 0,
                                 created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 created_user INT,
                                 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de pagos
CREATE TABLE payments (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          booking_id BIGINT,
                          amount DECIMAL(10,2),
                          currency VARCHAR(3) DEFAULT 'ARS',
                          method VARCHAR(50), -- CREDIT_CARD, MERCADO_PAGO, TRANSFER, etc.
                          payment_provider VARCHAR(50), -- MERCADO_PAGO, STRIPE, etc.
                          external_payment_id VARCHAR(100), -- ID del proveedor de pago
                          status VARCHAR(20), -- PENDING, APPROVED, REJECTED, CANCELLED
                          date DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de Comisiones - DeViaje
CREATE TABLE commissions (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             type ENUM('FLIGHT', 'HOTEL', 'PACKAGE') NOT NULL,
                             percentage DECIMAL(5,2) NOT NULL
);
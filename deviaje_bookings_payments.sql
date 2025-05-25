-- Base de Datos para Microservicio de Reservas y Transacciones

CREATE DATABASE IF NOT EXISTS deviaje_bookings_payments;

USE deviaje_bookings_payments;

-- Tabla principal de reservas
CREATE TABLE bookings (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          client_id INT NOT NULL,
                          agent_id INT,
                          branch_id INT,
                          status VARCHAR(20) NOT NULL,   -- PENDING, CONFIRMED, CANCELLED, COMPLETED
                          type VARCHAR(20) NOT NULL,     -- FLIGHT, HOTEL, PACKAGE
                          total_amount DECIMAL(10,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
                          discount DECIMAL(10,2) DEFAULT 0,
                          taxes DECIMAL(10,2) DEFAULT 0,
                          notes TEXT,
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT
);

-- Tabla de reservas de vuelos (datos mínimos)
CREATE TABLE flights_bookings (
                                  id INT PRIMARY KEY AUTO_INCREMENT,
                                  booking_id INT NOT NULL,
                                  external_id VARCHAR(50), -- ID de Amadeus
                                  origin VARCHAR(3) NOT NULL,
                                  destination VARCHAR(3) NOT NULL,
                                  departure_date DATETIME NOT NULL,
                                  return_date DATETIME, -- Para vuelos de ida y vuelta
                                  carrier VARCHAR(2) NOT NULL, -- Aerolínea principal (código IATA)
                                  base_price DECIMAL(10,2) NOT NULL,
                                  taxes DECIMAL(10,2) NOT NULL,
                                  discounts DECIMAL(10,2) DEFAULT 0,
                                  total_price DECIMAL(10,2) NOT NULL,
                                  currency VARCHAR(3) NOT NULL,
                                  status VARCHAR(20) NOT NULL,
                                  created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  created_user INT,
                                  last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  last_updated_user INT,
                                  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de reservas de hoteles (datos mínimos)
CREATE TABLE hotels_bookings (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 booking_id INT NOT NULL,
                                 external_id VARCHAR(50), -- Reference de HotelBeds
                                 hotel_code VARCHAR(10) NOT NULL,
                                 hotel_name VARCHAR(100) NOT NULL,
                                 destination_code VARCHAR(10) NOT NULL,
                                 destination_name VARCHAR(50) NOT NULL,
                                 check_in_date DATE NOT NULL,
                                 check_out_date DATE NOT NULL,
                                 number_of_nights INT NOT NULL,
                                 number_of_rooms INT NOT NULL,
                                 adults INT NOT NULL,
                                 children INT NOT NULL,
                                 base_price DECIMAL(10,2) NOT NULL,
                                 taxes DECIMAL(10,2) NOT NULL,
                                 discounts DECIMAL(10,2) DEFAULT 0,
                                 total_price DECIMAL(10,2) NOT NULL,
                                 currency VARCHAR(3) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 created_user INT,
                                 last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 last_updated_user INT,
                                 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de pagos (sin card_payment_details)
CREATE TABLE payments (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          booking_id INT NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
                          method VARCHAR(50) NOT NULL, -- CREDIT_CARD, MERCADO_PAGO, TRANSFER, etc.
                          payment_provider VARCHAR(50), -- MERCADO_PAGO, STRIPE, etc.
                          external_payment_id VARCHAR(100), -- ID del proveedor de pago
                          status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, REJECTED, CANCELLED
                          date DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT,
                          FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de facturas
CREATE TABLE invoices (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          booking_id INT NOT NULL,
                          invoice_number VARCHAR(20) NOT NULL UNIQUE,
                          date DATETIME DEFAULT CURRENT_TIMESTAMP,
                          subtotal DECIMAL(10,2) NOT NULL,
                          taxes DECIMAL(10,2) NOT NULL,
                          discounts DECIMAL(10,2) DEFAULT 0,
                          total DECIMAL(10,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
                          status VARCHAR(20) NOT NULL, -- DRAFT, SENT, PAID, CANCELLED
                          pdf_path VARCHAR(255),
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT,
                          FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
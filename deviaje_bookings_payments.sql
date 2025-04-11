-- Base de Datos para Microservicio de Reservas y Transacciones

CREATE DATABASE IF NOT EXISTS deviaje_bookings_payments;

USE deviaje_bookings_payments;

-- Tabla principal de reservas (bookings)
CREATE TABLE bookings (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          booking_code VARCHAR(10) NOT NULL UNIQUE,
                          client_id INT NOT NULL,
                          agent_id INT,
                          branch_id INT,
                          status VARCHAR(20) NOT NULL,   -- PENDING, CONFIRMED, CANCELLED, COMPLETED
                          total_amount DECIMAL(10,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                          discount DECIMAL(10,2) DEFAULT 0,
                          taxes DECIMAL(10,2) DEFAULT 0,
                          notes TEXT,
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT
);

-- Tabla de reservas de vuelos
CREATE TABLE flights_bookings (
                                  id INT PRIMARY KEY AUTO_INCREMENT,
                                  booking_id INT NOT NULL,
                                  amadeus_id VARCHAR(50),
                                  origin VARCHAR(3) NOT NULL,
                                  destination VARCHAR(3) NOT NULL,
                                  departure_date DATETIME NOT NULL,
                                  arrival_date DATETIME NOT NULL,
                                  airline VARCHAR(50) NOT NULL,
                                  flight_number VARCHAR(10) NOT NULL,
                                  class VARCHAR(20) NOT NULL,
                                  base_price DECIMAL(10,2) NOT NULL,
                                  taxes DECIMAL(10,2) NOT NULL,
                                  total_price DECIMAL(10,2) NOT NULL,
                                  currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                                  stops INT DEFAULT 0,
                                  duration VARCHAR(20),
                                  created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                  created_user INT,
                                  last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  last_updated_user INT,
                                  FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de segmentos de vuelo (para escalas)
CREATE TABLE flight_segments (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 flight_booking_id INT NOT NULL,
                                 origin VARCHAR(3) NOT NULL,
                                 destination VARCHAR(3) NOT NULL,
                                 departure_terminal VARCHAR(10),
                                 arrival_terminal VARCHAR(10),
                                 departure_date DATETIME NOT NULL,
                                 arrival_date DATETIME NOT NULL,
                                 airline VARCHAR(50) NOT NULL,
                                 flight_number VARCHAR(10) NOT NULL,
                                 aircraft VARCHAR(10),
                                 duration VARCHAR(20),
                                 created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 created_user INT,
                                 last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 last_updated_user INT,
                                 FOREIGN KEY (flight_booking_id) REFERENCES flights_bookings(id)
);

-- Tabla de reservas de hoteles
CREATE TABLE hotels_bookings (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 booking_id INT NOT NULL,
                                 amadeus_id VARCHAR(50),
                                 hotel_name VARCHAR(100) NOT NULL,
                                 hotel_chain VARCHAR(50),
                                 address VARCHAR(255),
                                 city VARCHAR(100) NOT NULL,
                                 country VARCHAR(100) NOT NULL,
                                 stars INT,
                                 latitude DECIMAL(10,8),
                                 longitude DECIMAL(11,8),
                                 check_in_date DATE NOT NULL,
                                 check_out_date DATE NOT NULL,
                                 nights INT NOT NULL,
                                 rooms INT NOT NULL,
                                 room_type VARCHAR(50) NOT NULL,
                                 board_basis VARCHAR(50),
                                 price_per_night DECIMAL(10,2) NOT NULL,
                                 total_price DECIMAL(10,2) NOT NULL,
                                 currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                                 cancellation_policy TEXT,
                                 created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 created_user INT,
                                 last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 last_updated_user INT,
                                 FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de pasajeros
CREATE TABLE passengers (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            booking_id INT NOT NULL,
                            first_name VARCHAR(100) NOT NULL,
                            last_name VARCHAR(100) NOT NULL,
                            type VARCHAR(20) NOT NULL,
                            dni VARCHAR(20),
                            created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                            created_user INT,
                            last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            last_updated_user INT,
                            FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de pagos
CREATE TABLE payments (
                          id INT PRIMARY KEY AUTO_INCREMENT,
                          booking_id INT NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                          method VARCHAR(50) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          date DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT,
                          FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de detalles de pago con tarjeta
CREATE TABLE card_payment_details (
                                      id INT PRIMARY KEY AUTO_INCREMENT,
                                      payment_id INT NOT NULL,
                                      card_holder VARCHAR(100) NOT NULL,
                                      masked_number VARCHAR(19) NOT NULL,
                                      card_type VARCHAR(20) NOT NULL,
                                      expiry_month VARCHAR(2) NOT NULL,
                                      expiry_year VARCHAR(4) NOT NULL,
                                      auth_code VARCHAR(20),
                                      created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      created_user INT,
                                      last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      last_updated_user INT,
                                      FOREIGN KEY (payment_id) REFERENCES payments(id)
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
                          currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                          status VARCHAR(20) NOT NULL,
                          pdf_path VARCHAR(255),
                          created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          created_user INT,
                          last_updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          last_updated_user INT,
                          FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Tabla de Auditoría para bookings
CREATE TABLE bookings_audit (
                                version_id INT AUTO_INCREMENT PRIMARY KEY,
                                id INT,
                                version INT,
                                booking_code VARCHAR(10),
                                client_id INT,
                                agent_id INT,
                                branch_id INT,
                                status VARCHAR(20),
                                total_amount DECIMAL(10,2),
                                currency VARCHAR(3),
                                discount DECIMAL(10,2),
                                taxes DECIMAL(10,2),
                                notes TEXT,
                                created_datetime DATETIME,
                                created_user INT,
                                last_updated_datetime DATETIME,
                                last_updated_user INT
);

-- Tabla de Auditoría para flights_bookings
CREATE TABLE flights_bookings_audit (
                                        version_id INT AUTO_INCREMENT PRIMARY KEY,
                                        id INT,
                                        version INT,
                                        booking_id INT,
                                        amadeus_id VARCHAR(50),
                                        origin VARCHAR(3),
                                        destination VARCHAR(3),
                                        departure_date DATETIME,
                                        arrival_date DATETIME,
                                        airline VARCHAR(50),
                                        flight_number VARCHAR(10),
                                        class VARCHAR(20),
                                        base_price DECIMAL(10,2),
                                        taxes DECIMAL(10,2),
                                        total_price DECIMAL(10,2),
                                        currency VARCHAR(3),
                                        stops INT,
                                        duration VARCHAR(20),
                                        created_datetime DATETIME,
                                        created_user INT,
                                        last_updated_datetime DATETIME,
                                        last_updated_user INT
);

-- Tabla de Auditoría para flight_segments
CREATE TABLE flight_segments_audit (
                                       version_id INT AUTO_INCREMENT PRIMARY KEY,
                                       id INT,
                                       version INT,
                                       flight_booking_id INT,
                                       origin VARCHAR(3),
                                       destination VARCHAR(3),
                                       departure_terminal VARCHAR(10),
                                       arrival_terminal VARCHAR(10),
                                       departure_date DATETIME,
                                       arrival_date DATETIME,
                                       airline VARCHAR(50),
                                       flight_number VARCHAR(10),
                                       aircraft VARCHAR(10),
                                       duration VARCHAR(20),
                                       created_datetime DATETIME,
                                       created_user INT,
                                       last_updated_datetime DATETIME,
                                       last_updated_user INT
);

-- Tabla de Auditoría para hotels_bookings
CREATE TABLE hotels_bookings_audit (
                                       version_id INT AUTO_INCREMENT PRIMARY KEY,
                                       id INT,
                                       version INT,
                                       booking_id INT,
                                       amadeus_id VARCHAR(50),
                                       hotel_name VARCHAR(100),
                                       hotel_chain VARCHAR(50),
                                       address VARCHAR(255),
                                       city VARCHAR(100),
                                       country VARCHAR(100),
                                       stars INT,
                                       latitude DECIMAL(10,8),
                                       longitude DECIMAL(11,8),
                                       check_in_date DATE,
                                       check_out_date DATE,
                                       nights INT,
                                       rooms INT,
                                       room_type VARCHAR(50),
                                       board_basis VARCHAR(50),
                                       price_per_night DECIMAL(10,2),
                                       total_price DECIMAL(10,2),
                                       currency VARCHAR(3),
                                       cancellation_policy TEXT,
                                       created_datetime DATETIME,
                                       created_user INT,
                                       last_updated_datetime DATETIME,
                                       last_updated_user INT
);

-- Tabla de Auditoría para passengers
CREATE TABLE passengers_audit (
                                  version_id INT AUTO_INCREMENT PRIMARY KEY,
                                  id INT,
                                  version INT,
                                  booking_id INT,
                                  first_name VARCHAR(100),
                                  last_name VARCHAR(100),
                                  type VARCHAR(20),
                                  dni VARCHAR(20),
                                  created_datetime DATETIME,
                                  created_user INT,
                                  last_updated_datetime DATETIME,
                                  last_updated_user INT
);

-- Tabla de Auditoría para payments
CREATE TABLE payments_audit (
                                version_id INT AUTO_INCREMENT PRIMARY KEY,
                                id INT,
                                version INT,
                                booking_id INT,
                                amount DECIMAL(10,2),
                                currency VARCHAR(3),
                                method VARCHAR(50),
                                status VARCHAR(20),
                                date DATETIME,
                                created_datetime DATETIME,
                                created_user INT,
                                last_updated_datetime DATETIME,
                                last_updated_user INT
);

-- Tabla de Auditoría para card_payment_details
CREATE TABLE card_payment_details_audit (
                                            version_id INT AUTO_INCREMENT PRIMARY KEY,
                                            id INT,
                                            version INT,
                                            payment_id INT,
                                            card_holder VARCHAR(100),
                                            masked_number VARCHAR(19),
                                            card_type VARCHAR(20),
                                            expiry_month VARCHAR(2),
                                            expiry_year VARCHAR(4),
                                            auth_code VARCHAR(20),
                                            created_datetime DATETIME,
                                            created_user INT,
                                            last_updated_datetime DATETIME,
                                            last_updated_user INT
);

-- Tabla de Auditoría para invoices
CREATE TABLE invoices_audit (
                                version_id INT AUTO_INCREMENT PRIMARY KEY,
                                id INT,
                                version INT,
                                booking_id INT,
                                invoice_number VARCHAR(20),
                                date DATETIME,
                                subtotal DECIMAL(10,2),
                                taxes DECIMAL(10,2),
                                discounts DECIMAL(10,2),
                                total DECIMAL(10,2),
                                currency VARCHAR(3),
                                status VARCHAR(20),
                                pdf_path VARCHAR(255),
                                created_datetime DATETIME,
                                created_user INT,
                                last_updated_datetime DATETIME,
                                last_updated_user INT
);

-- Ahora creamos los triggers de auditoría
DELIMITER $$

-- Triggers para bookings
CREATE TRIGGER trg_bookings_insert
    AFTER INSERT ON bookings
    FOR EACH ROW
BEGIN
    INSERT INTO bookings_audit (id, version, booking_code, client_id, agent_id, branch_id, status, total_amount, currency, discount, taxes, notes, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_code, NEW.client_id, NEW.agent_id, NEW.branch_id, NEW.status, NEW.total_amount, NEW.currency, NEW.discount, NEW.taxes, NEW.notes, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_bookings_update
    AFTER UPDATE ON bookings
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM bookings_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO bookings_audit (id, version, booking_code, client_id, agent_id, branch_id, status, total_amount, currency, discount, taxes, notes, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_code, NEW.client_id, NEW.agent_id, NEW.branch_id, NEW.status, NEW.total_amount, NEW.currency, NEW.discount, NEW.taxes, NEW.notes, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para flights_bookings
CREATE TRIGGER trg_flights_bookings_insert
    AFTER INSERT ON flights_bookings
    FOR EACH ROW
BEGIN
    INSERT INTO flights_bookings_audit (id, version, booking_id, amadeus_id, origin, destination, departure_date, arrival_date, airline, flight_number, class, base_price, taxes, total_price, currency, stops, duration, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_id, NEW.amadeus_id, NEW.origin, NEW.destination, NEW.departure_date, NEW.arrival_date, NEW.airline, NEW.flight_number, NEW.class, NEW.base_price, NEW.taxes, NEW.total_price, NEW.currency, NEW.stops, NEW.duration, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_flights_bookings_update
    AFTER UPDATE ON flights_bookings
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM flights_bookings_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO flights_bookings_audit (id, version, booking_id, amadeus_id, origin, destination, departure_date, arrival_date, airline, flight_number, class, base_price, taxes, total_price, currency, stops, duration, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_id, NEW.amadeus_id, NEW.origin, NEW.destination, NEW.departure_date, NEW.arrival_date, NEW.airline, NEW.flight_number, NEW.class, NEW.base_price, NEW.taxes, NEW.total_price, NEW.currency, NEW.stops, NEW.duration, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para flight_segments
CREATE TRIGGER trg_flight_segments_insert
    AFTER INSERT ON flight_segments
    FOR EACH ROW
BEGIN
    INSERT INTO flight_segments_audit (id, version, flight_booking_id, origin, destination, departure_terminal, arrival_terminal, departure_date, arrival_date, airline, flight_number, aircraft, duration, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.flight_booking_id, NEW.origin, NEW.destination, NEW.departure_terminal, NEW.arrival_terminal, NEW.departure_date, NEW.arrival_date, NEW.airline, NEW.flight_number, NEW.aircraft, NEW.duration, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_flight_segments_update
    AFTER UPDATE ON flight_segments
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM flight_segments_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO flight_segments_audit (id, version, flight_booking_id, origin, destination, departure_terminal, arrival_terminal, departure_date, arrival_date, airline, flight_number, aircraft, duration, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.flight_booking_id, NEW.origin, NEW.destination, NEW.departure_terminal, NEW.arrival_terminal, NEW.departure_date, NEW.arrival_date, NEW.airline, NEW.flight_number, NEW.aircraft, NEW.duration, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para hotels_bookings
CREATE TRIGGER trg_hotels_bookings_insert
    AFTER INSERT ON hotels_bookings
    FOR EACH ROW
BEGIN
    INSERT INTO hotels_bookings_audit (id, version, booking_id, amadeus_id, hotel_name, hotel_chain, address, city, country, stars, latitude, longitude, check_in_date, check_out_date, nights, rooms, room_type, board_basis, price_per_night, total_price, currency, cancellation_policy, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_id, NEW.amadeus_id, NEW.hotel_name, NEW.hotel_chain, NEW.address, NEW.city, NEW.country, NEW.stars, NEW.latitude, NEW.longitude, NEW.check_in_date, NEW.check_out_date, NEW.nights, NEW.rooms, NEW.room_type, NEW.board_basis, NEW.price_per_night, NEW.total_price, NEW.currency, NEW.cancellation_policy, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_hotels_bookings_update
    AFTER UPDATE ON hotels_bookings
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM hotels_bookings_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO hotels_bookings_audit (id, version, booking_id, amadeus_id, hotel_name, hotel_chain, address, city, country, stars, latitude, longitude, check_in_date, check_out_date, nights, rooms, room_type, board_basis, price_per_night, total_price, currency, cancellation_policy, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_id, NEW.amadeus_id, NEW.hotel_name, NEW.hotel_chain, NEW.address, NEW.city, NEW.country, NEW.stars, NEW.latitude, NEW.longitude, NEW.check_in_date, NEW.check_out_date, NEW.nights, NEW.rooms, NEW.room_type, NEW.board_basis, NEW.price_per_night, NEW.total_price, NEW.currency, NEW.cancellation_policy, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para passengers
CREATE TRIGGER trg_passengers_insert
    AFTER INSERT ON passengers
    FOR EACH ROW
BEGIN
    INSERT INTO passengers_audit (id, version, booking_id, first_name, last_name, type, dni, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_id, NEW.first_name, NEW.last_name, NEW.type, NEW.dni, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_passengers_update
    AFTER UPDATE ON passengers
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM passengers_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO passengers_audit (id, version, booking_id, first_name, last_name, type, dni, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_id, NEW.first_name, NEW.last_name, NEW.type, NEW.dni, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para payments
CREATE TRIGGER trg_payments_insert
    AFTER INSERT ON payments
    FOR EACH ROW
BEGIN
    INSERT INTO payments_audit (id, version, booking_id, amount, currency, method, status, date, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_id, NEW.amount, NEW.currency, NEW.method, NEW.status, NEW.date, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_payments_update
    AFTER UPDATE ON payments
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM payments_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO payments_audit (id, version, booking_id, amount, currency, method, status, date, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_id, NEW.amount, NEW.currency, NEW.method, NEW.status, NEW.date, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

DELIMITER ;

DELIMITER $$

-- Triggers para card_payment_details
CREATE TRIGGER trg_card_payment_details_insert
    AFTER INSERT ON card_payment_details
    FOR EACH ROW
BEGIN
    INSERT INTO card_payment_details_audit (id, version, payment_id, card_holder, masked_number, card_type, expiry_month, expiry_year, auth_code, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.payment_id, NEW.card_holder, NEW.masked_number, NEW.card_type, NEW.expiry_month, NEW.expiry_year, NEW.auth_code, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_card_payment_details_update
    AFTER UPDATE ON card_payment_details
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM card_payment_details_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO card_payment_details_audit (id, version, payment_id, card_holder, masked_number, card_type, expiry_month, expiry_year, auth_code, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.payment_id, NEW.card_holder, NEW.masked_number, NEW.card_type, NEW.expiry_month, NEW.expiry_year, NEW.auth_code, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

-- Triggers para invoices
CREATE TRIGGER trg_invoices_insert
    AFTER INSERT ON invoices
    FOR EACH ROW
BEGIN
    INSERT INTO invoices_audit (id, version, booking_id, invoice_number, date, subtotal, taxes, discounts, total, currency, status, pdf_path, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, 1, NEW.booking_id, NEW.invoice_number, NEW.date, NEW.subtotal, NEW.taxes, NEW.discounts, NEW.total, NEW.currency, NEW.status, NEW.pdf_path, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

CREATE TRIGGER trg_invoices_update
    AFTER UPDATE ON invoices
    FOR EACH ROW
BEGIN
    DECLARE latest_version INT;
    SELECT MAX(version) INTO latest_version FROM invoices_audit WHERE id = NEW.id;
    SET latest_version = IFNULL(latest_version, 0) + 1;

    INSERT INTO invoices_audit (id, version, booking_id, invoice_number, date, subtotal, taxes, discounts, total, currency, status, pdf_path, created_datetime, created_user, last_updated_datetime, last_updated_user)
    VALUES (NEW.id, latest_version, NEW.booking_id, NEW.invoice_number, NEW.date, NEW.subtotal, NEW.taxes, NEW.discounts, NEW.total, NEW.currency, NEW.status, NEW.pdf_path, NEW.created_datetime, NEW.created_user, NEW.last_updated_datetime, NEW.last_updated_user);
END $$

DELIMITER ;
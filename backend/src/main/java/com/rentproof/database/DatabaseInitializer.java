package com.rentproof.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the RentProof database tables in foreign-key dependency order.
 */
public class DatabaseInitializer {

    private static final String CREATE_DATABASE_SQL =
            "CREATE DATABASE IF NOT EXISTS rentproof "
            + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

    private static final String[] CREATE_TABLE_SQL = {
        "CREATE TABLE IF NOT EXISTS landlords ("
                + "landlord_id INT NOT NULL AUTO_INCREMENT,"
                + "full_name VARCHAR(100) NOT NULL,"
                + "email VARCHAR(100) NOT NULL,"
                + "phone VARCHAR(20) NOT NULL,"
                + "password_hash VARCHAR(255) NOT NULL,"
                + "password_salt VARCHAR(255) NULL,"
                + "password_iterations INT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (landlord_id),"
                + "CONSTRAINT uq_landlord_email UNIQUE (email)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        "CREATE TABLE IF NOT EXISTS properties ("
                + "property_id INT NOT NULL AUTO_INCREMENT,"
                + "landlord_id INT NOT NULL,"
                + "property_name VARCHAR(100) NOT NULL,"
                + "property_type VARCHAR(50) NOT NULL,"
                + "address VARCHAR(255) NOT NULL,"
                + "city VARCHAR(50) NOT NULL,"
                + "state VARCHAR(50) NOT NULL,"
                + "pincode VARCHAR(10) NOT NULL,"
                + "total_rooms INT NOT NULL DEFAULT 1,"
                + "monthly_rent DECIMAL(10,2) NOT NULL DEFAULT 0.00,"
                + "description TEXT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (property_id),"
                + "KEY idx_properties_landlord (landlord_id),"
                + "KEY idx_properties_city (city),"
                + "KEY idx_properties_type (property_type),"
                + "CONSTRAINT fk_properties_landlord FOREIGN KEY (landlord_id) "
                + "REFERENCES landlords(landlord_id) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        "CREATE TABLE IF NOT EXISTS rooms ("
                + "room_id INT NOT NULL AUTO_INCREMENT,"
                + "property_id INT NOT NULL,"
                + "room_number VARCHAR(20) NOT NULL,"
                + "monthly_rent DECIMAL(10,2) NOT NULL DEFAULT 0.00,"
                + "room_status VARCHAR(20) NOT NULL DEFAULT 'Vacant',"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (room_id),"
                + "CONSTRAINT uq_property_room UNIQUE (property_id, room_number),"
                + "KEY idx_rooms_property (property_id),"
                + "KEY idx_rooms_status (room_status),"
                + "CONSTRAINT fk_rooms_property FOREIGN KEY (property_id) "
                + "REFERENCES properties(property_id) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        "CREATE TABLE IF NOT EXISTS tenants ("
                + "tenant_id INT NOT NULL AUTO_INCREMENT,"
                + "property_id INT NOT NULL,"
                + "room_id INT NOT NULL,"
                + "full_name VARCHAR(100) NOT NULL,"
                + "email VARCHAR(100) NOT NULL,"
                + "phone VARCHAR(20) NOT NULL,"
                + "emergency_contact VARCHAR(20) NULL,"
                + "id_proof_number VARCHAR(50) NULL,"
                + "move_in_date DATE NOT NULL,"
                + "move_out_date DATE NULL,"
                + "security_deposit DECIMAL(10,2) DEFAULT 0.00,"
                + "tenant_status VARCHAR(20) NOT NULL DEFAULT 'Active',"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (tenant_id),"
                + "KEY idx_tenants_property (property_id),"
                + "KEY idx_tenants_room (room_id),"
                + "KEY idx_tenants_status (tenant_status),"
                + "CONSTRAINT fk_tenants_property FOREIGN KEY (property_id) "
                + "REFERENCES properties(property_id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                + "CONSTRAINT fk_tenants_room FOREIGN KEY (room_id) "
                + "REFERENCES rooms(room_id) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        "CREATE TABLE IF NOT EXISTS payments ("
                + "payment_id INT NOT NULL AUTO_INCREMENT,"
                + "tenant_id INT NOT NULL,"
                + "property_id INT NOT NULL,"
                + "room_id INT NOT NULL,"
                + "amount DECIMAL(10,2) NOT NULL,"
                + "payment_date DATE NOT NULL,"
                + "payment_month VARCHAR(20) NOT NULL,"
                + "payment_mode VARCHAR(30) NOT NULL,"
                + "transaction_ref VARCHAR(100) NULL,"
                + "payment_status VARCHAR(20) NOT NULL DEFAULT 'Paid',"
                + "remarks TEXT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (payment_id),"
                + "KEY idx_payments_tenant (tenant_id),"
                + "KEY idx_payments_property (property_id),"
                + "KEY idx_payments_status (payment_status),"
                + "CONSTRAINT fk_payments_tenant FOREIGN KEY (tenant_id) "
                + "REFERENCES tenants(tenant_id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                + "CONSTRAINT fk_payments_property FOREIGN KEY (property_id) "
                + "REFERENCES properties(property_id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                + "CONSTRAINT fk_payments_room FOREIGN KEY (room_id) "
                + "REFERENCES rooms(room_id) ON DELETE RESTRICT ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",

        "CREATE TABLE IF NOT EXISTS reminders ("
                + "reminder_id INT NOT NULL AUTO_INCREMENT,"
                + "landlord_id INT NOT NULL,"
                + "tenant_id INT NULL,"
                + "title VARCHAR(150) NOT NULL,"
                + "message TEXT NOT NULL,"
                + "due_date DATE NOT NULL,"
                + "reminder_type VARCHAR(50) NOT NULL,"
                + "reminder_status VARCHAR(20) NOT NULL DEFAULT 'Pending',"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (reminder_id),"
                + "KEY idx_reminders_landlord (landlord_id),"
                + "KEY idx_reminders_tenant (tenant_id),"
                + "KEY idx_reminders_status (reminder_status),"
                + "CONSTRAINT fk_reminders_landlord FOREIGN KEY (landlord_id) "
                + "REFERENCES landlords(landlord_id) ON DELETE RESTRICT ON UPDATE CASCADE,"
                + "CONSTRAINT fk_reminders_tenant FOREIGN KEY (tenant_id) "
                + "REFERENCES tenants(tenant_id) ON DELETE SET NULL ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    };

    private static final String[] TABLE_NAMES = {
        "landlords", "properties", "rooms", "tenants", "payments", "reminders"
    };

    private DatabaseInitializer() {
    }

    /**
     * Creates the database if needed and ensures each required table exists.
     *
     * @throws SQLException if the database or any table cannot be created
     */
    public static void initializeDatabase() throws SQLException {
        try (Connection connection = DBConnection.getServerConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_DATABASE_SQL);
            statement.execute("USE rentproof");

            statement.executeUpdate(CREATE_TABLE_SQL[0]);
            System.out.println("Table ready: " + TABLE_NAMES[0]);

            DatabaseMetaData meta = connection.getMetaData();
            ensureLandlordColumn(meta, statement, "password_salt", "VARCHAR(255) NULL");
            ensureLandlordColumn(meta, statement, "password_iterations", "INT NULL");

            for (int i = 1; i < CREATE_TABLE_SQL.length; i++) {
                statement.executeUpdate(CREATE_TABLE_SQL[i]);
                System.out.println("Table ready: " + TABLE_NAMES[i]);
            }
        }
    }

    private static void ensureLandlordColumn(DatabaseMetaData meta, Statement statement,
                                             String columnName, String columnDefinition)
            throws SQLException {
        try (ResultSet column = meta.getColumns("rentproof", null, "landlords", columnName)) {
            if (!column.next()) {
                statement.executeUpdate("ALTER TABLE landlords ADD COLUMN "
                        + columnName + " " + columnDefinition);
            }
        }
    }

    public static void main(String[] args) {
        try {
            initializeDatabase();
            System.out.println("RentProof database initialization completed successfully.");
        } catch (SQLException e) {
            System.err.println("RentProof database initialization failed: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState() + ", error code: " + e.getErrorCode());
        }
    }
}

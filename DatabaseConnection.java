package com.parking;

import java.sql.*;

public class DatabaseConnection {
    private static Connection con;

    public static Connection getConnection() throws Exception {
        if (con == null || con.isClosed()) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/vehicle_parking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", // <-- change to your DB user
                "moneca@2005"  // <-- change to your DB password
            );
            // Ensure the bookings table has the vehicle_type column (safe runtime migration)
            ensureVehicleTypeColumn(con);
        }
        return con;
    }

    // Checks INFORMATION_SCHEMA for the 'vehicle_type' column and adds it if missing.
    private static void ensureVehicleTypeColumn(Connection connection) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bookings' AND column_name = 'vehicle_type'"
            );
            rs = ps.executeQuery();
            boolean missing = true;
            if (rs.next()) {
                missing = rs.getInt(1) == 0;
            }
            if (missing) {
                Statement st = null;
                try {
                    st = connection.createStatement();
                    // Use a straightforward ALTER to add the column with a sensible default.
                    st.executeUpdate("ALTER TABLE bookings ADD COLUMN vehicle_type VARCHAR(20) DEFAULT 'Unknown'");
                } finally {
                    if (st != null) try { st.close(); } catch (SQLException ignored) {}
                }
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        }
    }
}

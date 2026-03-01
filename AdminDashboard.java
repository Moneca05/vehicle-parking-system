
package com.parking;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    private JButton addSlot, updateSlot, removeSlot, monitorStatus, viewReports, logout;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(480, 380);
        setLayout(new GridLayout(6,1,8,8));
        setLocationRelativeTo(null);

        addSlot = new JButton("Add Slot");
        updateSlot = new JButton("Update Slot Status");
        removeSlot = new JButton("Remove Slot");
        monitorStatus = new JButton("Monitor Slot Status");
        viewReports = new JButton("View Reports");
        logout = new JButton("Logout");

        add(addSlot);
        add(updateSlot);
        add(removeSlot);
        add(monitorStatus);
        add(viewReports);
        add(logout);

        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        addSlot.addActionListener(e -> addNewSlot());
        updateSlot.addActionListener(e -> changeSlotStatus());
        removeSlot.addActionListener(e -> removeSlot());
        monitorStatus.addActionListener(e -> monitor());
        viewReports.addActionListener(e -> viewReports());
        logout.addActionListener(e -> {
            new LoginFrame();
            dispose();
        });
    }

    private void addNewSlot() {
        String slot = JOptionPane.showInputDialog(this, "Enter slot no (eg A3):");
        if (slot == null || slot.trim().isEmpty()) return;

        String location = JOptionPane.showInputDialog(this, "Enter slot location (eg: Ground Floor - East Wing):");
        if (location == null || location.trim().isEmpty()) location = "Unknown";

        String[] types = {"2-Wheeler", "4-Wheeler"};
        String vehicleType = (String) JOptionPane.showInputDialog(
                this,
                "Select vehicle type:",
                "Vehicle Type",
                JOptionPane.PLAIN_MESSAGE,
                null,
                types,
                types[0]
        );

        if (vehicleType == null) return;

        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO slots(slot_no, status, location, vehicle_type) VALUES(?, 'Available', ?, ?)"
            );
            pst.setString(1, slot.trim());
            pst.setString(2, location.trim());
            pst.setString(3, vehicleType);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Slot added successfully!");
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "Slot already exists!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    private void changeSlotStatus() {
        String slot = JOptionPane.showInputDialog(this, "Enter slot no to update:");
        if (slot == null || slot.trim().isEmpty()) return;

        String status = JOptionPane.showInputDialog(this, "Enter new status (Available/Booked/Occupied):");
        if (status == null || status.trim().isEmpty()) return;

        String location = JOptionPane.showInputDialog(this, "Enter new location (leave blank to keep current):");

        String[] types = {"No Change", "2-Wheeler", "4-Wheeler"};
        String vehicleType = (String) JOptionPane.showInputDialog(
                this,
                "Update Vehicle Type:",
                "Vehicle Type Update",
                JOptionPane.PLAIN_MESSAGE,
                null,
                types,
                types[0]
        );

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement pst;

            if (!"No Change".equals(vehicleType)) {
                pst = con.prepareStatement("UPDATE slots SET status=?, location=IF(?='', location, ?), vehicle_type=? WHERE slot_no=?");
                pst.setString(1, status.trim());
                pst.setString(2, location == null ? "" : location.trim());
                pst.setString(3, location == null ? "" : location.trim());
                pst.setString(4, vehicleType);
                pst.setString(5, slot.trim());
            } else {
                pst = con.prepareStatement("UPDATE slots SET status=?, location=IF(?='', location, ?) WHERE slot_no=?");
                pst.setString(1, status.trim());
                pst.setString(2, location == null ? "" : location.trim());
                pst.setString(3, location == null ? "" : location.trim());
                pst.setString(4, slot.trim());
            }

            int rows = pst.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Slot updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Slot not found!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    private void removeSlot() {
        String slot = JOptionPane.showInputDialog(this, "Enter slot no to remove:");
        if (slot == null || slot.trim().isEmpty()) return;

        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("DELETE FROM slots WHERE slot_no=?");
            pst.setString(1, slot.trim());
            int r = pst.executeUpdate();
            if (r > 0) JOptionPane.showMessageDialog(this, "Slot removed!");
            else JOptionPane.showMessageDialog(this, "Slot not found!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    private void monitor() {
        try (Connection con = DatabaseConnection.getConnection()) {

            String query = "SELECT slot_no, status, location, vehicle_type FROM slots ORDER BY slot_no";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            String[] columnNames = {"Slot No", "Status", "Location", "Vehicle Type"};

            java.util.List<String[]> rows = new java.util.ArrayList<>();

            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("slot_no"),
                        rs.getString("status"),
                        rs.getString("location"),
                        rs.getString("vehicle_type")
                });
            }

            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No slot records available.");
                return;
            }

            String[][] data = rows.toArray(new String[0][]);

            JTable table = new JTable(data, columnNames);
            table.setEnabled(false);
            table.setRowHeight(25);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(450, 250));

            JOptionPane.showMessageDialog(
                    this,
                    scroll,
                    "Slot Status (Tabular View)",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    private void viewReports() {
        try (Connection con = DatabaseConnection.getConnection()) {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT username, slot_no, payment_status FROM bookings ORDER BY id DESC");

            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getString("username"))
                  .append(" - ").append(rs.getString("slot_no"))
                  .append(" - ").append(rs.getString("payment_status"))
                  .append("\n");
            }

            if (sb.length() == 0) sb.append("No bookings found");

            JOptionPane.showMessageDialog(this, sb.toString(),
                    "Bookings Report", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }
}

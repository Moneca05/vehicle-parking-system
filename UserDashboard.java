package com.parking;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserDashboard extends JFrame {

    private String currentUser;
    private JTable slotTable;
    private DefaultTableModel slotModel;

    private JComboBox<String> locationDropdown;

    public UserDashboard(String username) {
        this.currentUser = username;

        setTitle("User Dashboard - Welcome " + username);
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);

        JLabel title = new JLabel("User Dashboard");
        title.setBounds(350, 15, 400, 40);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        add(title);

        // LOCATION DROPDOWN
        JLabel locLabel = new JLabel("Select Location:");
        locLabel.setBounds(20, 70, 120, 30);
        add(locLabel);

        locationDropdown = new JComboBox<>();
        locationDropdown.setBounds(140, 70, 200, 30);
        locationDropdown.addActionListener(e -> loadSlotsByLocation());
        add(locationDropdown);

        loadLocations();

        // SLOT TABLE
        slotModel = new DefaultTableModel(
                new Object[]{"Slot No", "Status", "Vehicle Category"}, 0
        );
        slotTable = new JTable(slotModel);

        JScrollPane scroll = new JScrollPane(slotTable);
        scroll.setBounds(20, 120, 550, 350);
        add(scroll);

        // REFRESH BUTTON
        JButton refreshBtn = new JButton("Refresh Slots");
        refreshBtn.setBounds(600, 120, 200, 40);
        refreshBtn.addActionListener(e -> loadSlotsByLocation());
        add(refreshBtn);

        // BOOK SLOT
        JButton bookBtn = new JButton("Book Slot");
        bookBtn.setBounds(600, 180, 200, 40);
        bookBtn.addActionListener(e -> bookSelectedSlot());
        add(bookBtn);

        // CANCEL BOOKING
        JButton cancelBtn = new JButton("Cancel Booking");
        cancelBtn.setBounds(600, 240, 200, 40);
        cancelBtn.addActionListener(e -> new CancelBookingFrame(currentUser));
        add(cancelBtn);

        // VIEW BOOKINGS
        JButton viewBookingsBtn = new JButton("My Bookings");
        viewBookingsBtn.setBounds(600, 300, 200, 40);
        viewBookingsBtn.addActionListener(e -> showUserBookings());
        add(viewBookingsBtn);

        // LOGOUT
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBounds(600, 360, 200, 40);
        logoutBtn.addActionListener(e -> logout());
        add(logoutBtn);

        setVisible(true);
    }

    // --------------------------------------
    // LOAD ALL LOCATIONS INTO DROPDOWN
    // --------------------------------------
    private void loadLocations() {
        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement pst = con.prepareStatement(
                    "SELECT DISTINCT location FROM slots ORDER BY location"
            );

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                locationDropdown.addItem(rs.getString("location"));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading locations: " + ex.getMessage());
        }
    }
    private void loadSlotsByLocation() {
        slotModel.setRowCount(0);

        String selectedLocation = (String) locationDropdown.getSelectedItem();
        if (selectedLocation == null) return;

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement pst = con.prepareStatement(
                    "SELECT slot_no, status, vehicle_category FROM slots WHERE location=? ORDER BY slot_no"
            );
            pst.setString(1, selectedLocation);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                slotModel.addRow(new Object[]{
                        rs.getString("slot_no"),
                        rs.getString("status"),
                        rs.getString("vehicle_category")
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading slots: " + ex.getMessage());
        }
    }

    private void bookSelectedSlot() {
        int row = slotTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a slot!");
            return;
        }

        String slotNo = (String) slotModel.getValueAt(row, 0);
        String status = (String) slotModel.getValueAt(row, 1);

        if (!status.equalsIgnoreCase("Available")) {
            JOptionPane.showMessageDialog(this, "This slot is not available!");
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement upd = con.prepareStatement(
                    "UPDATE slots SET status='Booked' WHERE slot_no=?"
            );
            upd.setString(1, slotNo);
            upd.executeUpdate();

            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO bookings (username, slot_no, payment_status) VALUES (?, ?, 'Pending')",
                    Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, currentUser);
            pst.setString(2, slotNo);
            pst.executeUpdate();

            int bookingId = 0;
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) bookingId = keys.getInt(1);
            }

            JOptionPane.showMessageDialog(this, "Slot Added! Proceed to Payment");

            new PaymentGateway(currentUser, slotNo, null, bookingId);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Booking failed: " + ex.getMessage());
        }
    }

    // VIEW USER BOOKINGS
    private void showUserBookings() {
        JFrame frame = new JFrame("My Bookings");
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(this);

        DefaultTableModel userModel = new DefaultTableModel(
                new Object[]{"Slot No", "Start Time", "End Time", "Payment"}, 0
        );
        JTable userTable = new JTable(userModel);

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement pst = con.prepareStatement(
                    "SELECT slot_no, start_time, end_time, payment_status " +
                            "FROM bookings WHERE username=? ORDER BY id DESC"
            );
            pst.setString(1, currentUser);

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                userModel.addRow(new Object[]{
                        rs.getString("slot_no"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("payment_status")
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading bookings: " + ex.getMessage());
        }

        frame.add(new JScrollPane(userTable));
        frame.setVisible(true);
    }

    // LOGOUT
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame();
        }
    }
}

package com.parking;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CancelBookingFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private final String username;

    public CancelBookingFrame(String username) {
        this.username = username;

        setTitle("Cancel Booking");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        model = new DefaultTableModel(
                new String[]{"Booking ID", "Slot No", "Start Time", "End Time", "Status"}, 0);

        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        loadUserBookings();

        JButton cancelBtn = new JButton("Cancel Selected Booking");
        cancelBtn.addActionListener(e -> cancelSelected());

        add(scroll, BorderLayout.CENTER);
        add(cancelBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadUserBookings() {
        model.setRowCount(0);

        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement(
                "SELECT id, slot_no, start_time, end_time, payment_status " +
                "FROM bookings WHERE username=? AND payment_status='Success'"
            );
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
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
    }

    private void cancelSelected() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel");
            return;
        }

        int bookingId = (int) model.getValueAt(row, 0);
        String slotNo = (String) model.getValueAt(row, 1);

        try (Connection con = DatabaseConnection.getConnection()) {

            // 1. Cancel booking
            PreparedStatement pst = con.prepareStatement(
                "UPDATE bookings SET payment_status='Cancelled' WHERE id=?"
            );
            pst.setInt(1, bookingId);
            pst.executeUpdate();

            // 2. Free slot
            PreparedStatement upd = con.prepareStatement(
                "UPDATE slots SET status='Available' WHERE slot_no=?"
            );
            upd.setString(1, slotNo);
            upd.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Booking cancelled successfully!");

            loadUserBookings();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cancelling booking: " + ex.getMessage());
        }
    }
}


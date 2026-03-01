package com.parking;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class PaymentGateway extends JFrame {

    private static final double HOURLY_RATE = 50.0;

    private final String username;
    private final String slotNo;
    private final int bookingId; // Track booking ID

    private JComboBox<String> vehicleTypeBox;
    private JSpinner dateSpinner;
    private JSpinner timeSpinner;
    private JSpinner durationSpinner;
    private JLabel amountLabel;
    private JComboBox<String> paymentMethodBox;

    // CLEANED CONSTRUCTOR – remove old vehicleType param
    public PaymentGateway(String user, String slot, int bookingId) {
        this.username = user;
        this.slotNo = slot;
        this.bookingId = bookingId;

        setTitle("Payment Gateway - Slot " + slot);
        setSize(480, 420);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Font labelFont = new Font("Arial", Font.PLAIN, 15);

        // ---------- VEHICLE TYPE ----------
        JLabel vtLbl = new JLabel("Vehicle Type:");
        vtLbl.setBounds(40, 40, 150, 30);
        vtLbl.setFont(labelFont);
        add(vtLbl);

        vehicleTypeBox = new JComboBox<>(new String[]{
                "Two Wheeler", "Four Wheeler"
        });
        vehicleTypeBox.setBounds(200, 40, 200, 30);
        add(vehicleTypeBox);

        // ---------- PARKING DATE ----------
        JLabel dateLbl = new JLabel("Parking Date:");
        dateLbl.setBounds(40, 90, 150, 30);
        dateLbl.setFont(labelFont);
        add(dateLbl);

        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setBounds(200, 90, 200, 30);
        add(dateSpinner);

        // ---------- START TIME ----------
        JLabel timeLbl = new JLabel("Start Time (HH:mm):");
        timeLbl.setBounds(40, 140, 150, 30);
        timeLbl.setFont(labelFont);
        add(timeLbl);

        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setBounds(200, 140, 200, 30);
        add(timeSpinner);

        // ---------- DURATION ----------
        JLabel durLbl = new JLabel("Parking Duration (hours):");
        durLbl.setBounds(40, 190, 200, 30);
        durLbl.setFont(labelFont);
        add(durLbl);

        durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 48, 1));
        durationSpinner.setBounds(250, 190, 60, 30);
        durationSpinner.addChangeListener(e -> updateAmount());
        add(durationSpinner);

        // ---------- AMOUNT ----------
        JLabel amtLbl = new JLabel("Amount to Pay:");
        amtLbl.setBounds(40, 240, 200, 30);
        amtLbl.setFont(labelFont);
        add(amtLbl);

        amountLabel = new JLabel("Rs. 50.00");
        amountLabel.setBounds(200, 240, 200, 30);
        amountLabel.setFont(new Font("Arial", Font.BOLD, 15));
        add(amountLabel);

        // ---------- PAYMENT METHOD ----------
        JLabel methodLbl = new JLabel("Payment Method:");
        methodLbl.setBounds(40, 290, 200, 30);
        methodLbl.setFont(labelFont);
        add(methodLbl);

        paymentMethodBox = new JComboBox<>(new String[]{
                "Select Method", "UPI", "Credit Card", "Debit Card", "Net Banking", "Cash"
        });
        paymentMethodBox.setBounds(200, 290, 200, 30);
        add(paymentMethodBox);

        // ---------- PAY BUTTON ----------
        JButton payBtn = new JButton("Proceed to Pay");
        payBtn.setBounds(140, 340, 200, 40);
        payBtn.addActionListener(e -> processPayment());
        add(payBtn);

        updateAmount();
        setVisible(true);
    }

    private void updateAmount() {
        int hrs = (int) durationSpinner.getValue();
        double amount = hrs * HOURLY_RATE;
        amountLabel.setText("Rs. " + String.format("%.2f", amount));
    }

    private void processPayment() {
        if (paymentMethodBox.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Select a payment method");
            return;
        }

        String vehicleType = (String) vehicleTypeBox.getSelectedItem();
        String method = (String) paymentMethodBox.getSelectedItem();
        int hrs = (int) durationSpinner.getValue();
        double amount = hrs * HOURLY_RATE;

        // Extract Date
        LocalDate date = LocalDate.parse(
                new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .format(dateSpinner.getValue())
        );

        // Extract Time
        LocalTime time = LocalTime.parse(
                new java.text.SimpleDateFormat("HH:mm")
                        .format(timeSpinner.getValue())
        );

        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusHours(hrs);

        try (Connection con = DatabaseConnection.getConnection()) {

            // Update booking entry (correct)
            PreparedStatement pst = con.prepareStatement(
                    "UPDATE bookings SET vehicle_type=?, payment_status='Success', " +
                            "start_time=?, end_time=? WHERE id=?"
            );

            pst.setString(1, vehicleType);
            pst.setTimestamp(2, Timestamp.valueOf(start));
            pst.setTimestamp(3, Timestamp.valueOf(end));
            pst.setInt(4, bookingId);

            int updated = pst.executeUpdate();

            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "Payment failed: booking not found.");
                return;
            }

            // Slot remains Booked (correct)
            PreparedStatement pst2 = con.prepareStatement(
                    "UPDATE slots SET status='Booked' WHERE slot_no=?"
            );
            pst2.setString(1, slotNo);
            pst2.executeUpdate();

            // Generate Bill
            new BillFrame(username, slotNo, vehicleType, hrs, amount, method, start, end);

            JOptionPane.showMessageDialog(this,
                    "Payment Successful!\nStart: " + start + "\nEnd: " + end);

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment Error: " + ex.getMessage());
        }
    }
}

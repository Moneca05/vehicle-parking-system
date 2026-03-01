// package com.parking;

// import javax.swing.*;
// import java.awt.*;
// import java.sql.*;

// public class BookSlotFrame extends JFrame {
//     private JComboBox<String> slotBox;
//     private JComboBox<String> vehicleTypeBox;
//     private JButton bookBtn;
//     private String username;

//     public BookSlotFrame(String user) {
//         this.username = user;
//         setTitle("Book Slot");
//         setSize(420, 300);
//         setLayout(null);
//         setLocationRelativeTo(null);

//         JLabel lbl = new JLabel("Select Slot:");
//         lbl.setBounds(40, 60, 100, 30);
//         add(lbl);

//         slotBox = new JComboBox<>();
//         slotBox.setBounds(150, 60, 200, 30);
//         add(slotBox);

//         JLabel vLbl = new JLabel("Vehicle Type:");
//         vLbl.setBounds(40, 100, 100, 30);
//         add(vLbl);

//         vehicleTypeBox = new JComboBox<>(new String[]{"2 Wheeler", "4 Wheeler"});
//         vehicleTypeBox.setBounds(150, 100, 200, 30);
//         add(vehicleTypeBox);

//         bookBtn = new JButton("Book");
//         bookBtn.setBounds(150, 160, 100, 30);
//         add(bookBtn);

//         setVisible(true);
//         setDefaultCloseOperation(DISPOSE_ON_CLOSE);

//         loadAvailableSlots();

//         bookBtn.addActionListener(e -> bookSlot());
//     }

//     private void loadAvailableSlots() {
//         try (Connection con = DatabaseConnection.getConnection()) {
//             Statement st = con.createStatement();
//             ResultSet rs = st.executeQuery("SELECT slot_no, location FROM slots WHERE status='Available'");

//             while (rs.next()) {
//                 String slotView = rs.getString("slot_no") + " (" + rs.getString("location") + ")";
//                 slotBox.addItem(slotView);
//             }
//             if (slotBox.getItemCount() == 0) {
//                 slotBox.addItem("No Available Slots");
//                 bookBtn.setEnabled(false);
//             }
//         } catch (Exception ex) {
//             ex.printStackTrace();
//             JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
//         }
//     }

//     private void bookSlot() {
//         String selected = (String) slotBox.getSelectedItem();
//         if (selected == null || selected.equals("No Available Slots")) return;

//         String slot = selected.split(" ")[0];  // Extract A1 from "A1 (location)"

//         try (Connection con = DatabaseConnection.getConnection()) {

//             // double-check availability
//             PreparedStatement check = con.prepareStatement("SELECT status FROM slots WHERE slot_no=? FOR UPDATE");
//             check.setString(1, slot);
//             ResultSet rs = check.executeQuery();

//             if (rs.next() && rs.getString("status").equalsIgnoreCase("Available")) {

//                 // mark slot as Booked (reserve)
//                 PreparedStatement upd = con.prepareStatement("UPDATE slots SET status='Booked' WHERE slot_no=?");
//                 upd.setString(1, slot);
//                 upd.executeUpdate();

//                 String vehicleType = (String) vehicleTypeBox.getSelectedItem();

//                 // Insert booking WITHOUT start_time/end_time; payment will set times.
//                 PreparedStatement ins = con.prepareStatement(
//                         "INSERT INTO bookings(username, slot_no, vehicle_type, payment_status) " +
//                                 "VALUES(?, ?, ?, 'Pending')",
//                         Statement.RETURN_GENERATED_KEYS
//                 );
//                 ins.setString(1, username);
//                 ins.setString(2, slot);
//                 ins.setString(3, vehicleType);
//                 ins.executeUpdate();

//                 // get generated booking id
//                 int bookingId = 0;
//                 try (ResultSet keys = ins.getGeneratedKeys()) {
//                     if (keys.next()) {
//                         bookingId = keys.getInt(1);
//                     }
//                 }

//                 JOptionPane.showMessageDialog(this, "Slot booked! Proceed to payment.");

//                 // Pass bookingId to PaymentGateway so it updates exactly this row
//                 new PaymentGateway(username, slot, vehicleType, bookingId);
//                 dispose();

//             } else {
//                 JOptionPane.showMessageDialog(this, "Slot no longer available!");
//             }

//         } catch (Exception ex) {
//             ex.printStackTrace();
//             JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
//         }
//     }
// }
package com.parking;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class BookSlotFrame extends JFrame {

    private JLabel slotLabel;
    private JComboBox<String> vehicleTypeBox;
    private JButton bookBtn;

    private String username;
    private String slotNo;

    public BookSlotFrame(String username, String slotNo) {
        this.username = username;
        this.slotNo = slotNo;

        setTitle("Book Slot - " + slotNo);
        setSize(400, 250);
        setLocationRelativeTo(null);
        setLayout(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel lbl = new JLabel("Selected Slot:");
        lbl.setBounds(40, 40, 100, 30);
        add(lbl);

        slotLabel = new JLabel(slotNo);
        slotLabel.setBounds(150, 40, 200, 30);
        add(slotLabel);

        JLabel vLbl = new JLabel("Vehicle Type:");
        vLbl.setBounds(40, 80, 100, 30);
        add(vLbl);

        vehicleTypeBox = new JComboBox<>(new String[]{"2 Wheeler", "4 Wheeler"});
        vehicleTypeBox.setBounds(150, 80, 180, 30);
        add(vehicleTypeBox);

        bookBtn = new JButton("Book");
        bookBtn.setBounds(150, 130, 120, 30);
        add(bookBtn);

        bookBtn.addActionListener(e -> bookSlot());

        setVisible(true);
    }

    private void bookSlot() {
        String vehicleType = (String) vehicleTypeBox.getSelectedItem();

        try (Connection con = DatabaseConnection.getConnection()) {

            // Checking availability
            PreparedStatement check = con.prepareStatement(
                    "SELECT status FROM slots WHERE slot_no=? FOR UPDATE"
            );
            check.setString(1, slotNo);
            ResultSet rs = check.executeQuery();

            if (rs.next() && rs.getString("status").equalsIgnoreCase("Available")) {

                PreparedStatement upd = con.prepareStatement(
                        "UPDATE slots SET status='Booked' WHERE slot_no=?"
                );
                upd.setString(1, slotNo);
                upd.executeUpdate();

                PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO bookings(username, slot_no, vehicle_type, payment_status) VALUES (?, ?, ?, 'Pending')",
                        Statement.RETURN_GENERATED_KEYS
                );
                ins.setString(1, username);
                ins.setString(2, slotNo);
                ins.setString(3, vehicleType);
                ins.executeUpdate();

                int bookingId = 0;
                ResultSet keys = ins.getGeneratedKeys();
                if (keys.next()) bookingId = keys.getInt(1);

                JOptionPane.showMessageDialog(this, "Slot booked! Proceed to payment.");

                new PaymentGateway(username, slotNo, vehicleType, bookingId);
                dispose();

            } else {
                JOptionPane.showMessageDialog(this, "Slot is no longer available!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }
}

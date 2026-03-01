// package com.parking;

// import javax.swing.*;
// import java.awt.*;
// import java.sql.*;

// public class RegisterFrame extends JFrame {
//     private JTextField username;
//     private JPasswordField password;
//     private JButton registerBtn, backBtn;

//     public RegisterFrame() {
//         setTitle("User Registration");
//         setSize(420, 320);
//         setLayout(null);
//         setLocationRelativeTo(null);

//         JLabel userLabel = new JLabel("Username:");
//         userLabel.setBounds(50, 80, 100, 30);
//         add(userLabel);

//         username = new JTextField();
//         username.setBounds(160, 80, 180, 30);
//         add(username);

//         JLabel passLabel = new JLabel("Password:");
//         passLabel.setBounds(50, 120, 100, 30);
//         add(passLabel);

//         password = new JPasswordField();
//         password.setBounds(160, 120, 180, 30);
//         add(password);

//         registerBtn = new JButton("Register");
//         registerBtn.setBounds(80, 190, 100, 30);
//         add(registerBtn);

//         backBtn = new JButton("Back");
//         backBtn.setBounds(220, 190, 100, 30);
//         add(backBtn);

//         setDefaultCloseOperation(EXIT_ON_CLOSE);
//         setVisible(true);

//         registerBtn.addActionListener(e -> registerUser());
//         backBtn.addActionListener(e -> {
//             new LoginFrame();
//             dispose();
//         });
//     }

//     private void registerUser() {
//         String user = username.getText().trim();
//         String pass = new String(password.getPassword());

//         if (user.isEmpty() || pass.isEmpty()) {
//             JOptionPane.showMessageDialog(this, "Fill all fields");
//             return;
//         }

//         try (Connection con = DatabaseConnection.getConnection()) {
//             PreparedStatement pst = con.prepareStatement("INSERT INTO users(username, password) VALUES(?, ?)"); 
//             pst.setString(1, user);
//             pst.setString(2, pass);
//             pst.executeUpdate();
//             JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
//             new LoginFrame();
//             dispose();
//         } catch (SQLIntegrityConstraintViolationException ex) {
//             JOptionPane.showMessageDialog(this, "Username already exists!");
//         } catch (Exception ex) {
//             ex.printStackTrace();
//             JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
//         }
//     }
// }

package com.parking;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.security.MessageDigest;

public class RegisterFrame extends JFrame {

    private JTextField username;
    private JPasswordField password;
    private JButton registerBtn, backBtn;

    public RegisterFrame() {

        setTitle("User Registration");
        setSize(420, 320);
        setLayout(null);
        setLocationRelativeTo(null);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 80, 100, 30);
        add(userLabel);

        username = new JTextField();
        username.setBounds(160, 80, 180, 30);
        add(username);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 120, 100, 30);
        add(passLabel);

        password = new JPasswordField();
        password.setBounds(160, 120, 180, 30);
        add(password);

        registerBtn = new JButton("Register");
        registerBtn.setBounds(80, 190, 100, 30);
        add(registerBtn);

        backBtn = new JButton("Back");
        backBtn.setBounds(220, 190, 100, 30);
        add(backBtn);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        registerBtn.addActionListener(e -> registerUser());

        backBtn.addActionListener(e -> {
            new LoginFrame();
            dispose();
        });
    }

    // ------------------------------
    // REGISTER USER WITH SHA-256 HASH
    // ------------------------------
    private void registerUser() {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields");
            return;
        }

        String hashedPass = hashPassword(pass);

        try (Connection con = DatabaseConnection.getConnection()) {

            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO users(username, password) VALUES(?, ?)"
            );
            pst.setString(1, user);
            pst.setString(2, hashedPass);

            pst.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Registration successful! Please login.");

            new LoginFrame();
            dispose();

        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this,
                    "Username already exists!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage());
        }
    }

    // ------------------------------
    // SHA-256 PASSWORD HASHING
    // ------------------------------
    private String hashPassword(String password) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

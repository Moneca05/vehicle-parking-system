package com.parking;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BillFrame extends JFrame {

    private final String billText;

    public BillFrame(String username, String slotNo, String vehicleType,
                     int hours, double amount, String method,
                     LocalDateTime start, LocalDateTime end) {

        setTitle("Parking Receipt");
        setSize(450, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("----------- PARKING RECEIPT -----------\n");
        sb.append("User          : ").append(username).append("\n");
        sb.append("Slot Number   : ").append(slotNo).append("\n");
        sb.append("Vehicle Type  : ").append(vehicleType).append("\n");
        sb.append("Start Time    : ").append(start.format(dtf)).append("\n");
        sb.append("End Time      : ").append(end.format(dtf)).append("\n");
        sb.append("Duration      : ").append(hours).append(" hour(s)\n");
        sb.append("Rate per Hour : Rs. 50.00\n");
        sb.append("Total Amount  : Rs. ").append(String.format("%.2f", amount)).append("\n");
        sb.append("Payment Mode  : ").append(method).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Thank you for using the Parking System!\n");

        billText = sb.toString();

        JTextArea area = new JTextArea(billText);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Receipt");
        JButton closeBtn = new JButton("Close");
        bottom.add(saveBtn);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        saveBtn.addActionListener((ActionEvent e) -> saveReceipt());
        closeBtn.addActionListener((ActionEvent e) -> dispose());

        // Auto-save receipt
        try {
            autoSave();
        } catch (IOException ignored) {}

        setVisible(true);
    }

    private void autoSave() throws IOException {
        String dir = System.getProperty("user.dir");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        File file = new File(dir, "receipt_" + tf.format(LocalDateTime.now()) + ".txt");

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(billText);
        }
    }

    private void saveReceipt() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Receipt");
        int ret = chooser.showSaveDialog(this);

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try {
                FileWriter fw = new FileWriter(file);
                fw.write(billText);
                fw.close();

                JOptionPane.showMessageDialog(this,
                        "Receipt saved to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not save: " + ex.getMessage());
            }
        }
    }
}

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddCustomer extends JFrame {
    private JTextField nameField, emailField, phoneField;
    private JTextArea addressArea;

    public AddCustomer() {
        setTitle("Add New Customer");
        setSize(500, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Add New Customer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(nameLabel, gbc);

        nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(emailLabel, gbc);

        emailField = new JTextField(20);
        emailField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(phoneLabel, gbc);

        phoneField = new JTextField(20);
        phoneField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(phoneField, gbc);

        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(addressLabel, gbc);

        addressArea = new JTextArea(4, 20);
        addressArea.setFont(new Font("Arial", Font.PLAIN, 14));
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(addressArea);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        JButton addButton = new JButton("Add Customer");
        addButton.setFont(new Font("Arial", Font.BOLD, 16));
        addButton.setBackground(new Color(0, 153, 204));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addButton, gbc);

        addButton.addActionListener(e -> addCustomer());

        add(panel);
        setVisible(true);
    }

    private void addCustomer() {
        try {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressArea.getText().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Phone are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection conn = DBConnection.getConnection();
            String sql = "INSERT INTO customers (name, email, phone, address) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, email.isEmpty() ? null : email);
            stmt.setString(3, phone);
            stmt.setString(4, address);

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            JOptionPane.showMessageDialog(this, "Customer added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            nameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            addressArea.setText("");

        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "Email already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

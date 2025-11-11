import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddPet extends JFrame {
    private JTextField nameField, speciesField, breedField, ageField, priceField, quantityField;

    public AddPet() {
        setTitle("Add New Pet");
        setSize(500, 500);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Add New Pet");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        String[] labels = {"Name:", "Species:", "Breed:", "Age:", "Price:", "Quantity:"};
        JTextField[] fields = new JTextField[6];

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 16));
            panel.add(label, gbc);

            gbc.gridx = 1;
            fields[i] = new JTextField(20);
            fields[i].setFont(new Font("Arial", Font.PLAIN, 14));
            panel.add(fields[i], gbc);
        }

        nameField = fields[0];
        speciesField = fields[1];
        breedField = fields[2];
        ageField = fields[3];
        priceField = fields[4];
        quantityField = fields[5];

        JButton addButton = new JButton("Add Pet");
        addButton.setFont(new Font("Arial", Font.BOLD, 16));
        addButton.setBackground(new Color(0, 153, 204));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        panel.add(addButton, gbc);

        addButton.addActionListener(e -> addPet());

        add(panel);
        setVisible(true);
    }

    private void addPet() {
        try {
            String name = nameField.getText().trim();
            String species = speciesField.getText().trim();
            String breed = breedField.getText().trim();
            int age = Integer.parseInt(ageField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());

            if (name.isEmpty() || species.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Species are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection conn = DBConnection.getConnection();
            String sql = "INSERT INTO pets (name, species, breed, age, price, quantity) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, species);
            stmt.setString(3, breed);
            stmt.setInt(4, age);
            stmt.setDouble(5, price);
            stmt.setInt(6, quantity);

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            JOptionPane.showMessageDialog(this, "Pet added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            nameField.setText("");
            speciesField.setText("");
            breedField.setText("");
            ageField.setText("");
            priceField.setText("");
            quantityField.setText("");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for Age, Price, and Quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class MakeSale extends JFrame {
    private JComboBox<String> petComboBox, customerComboBox;
    private JTextField quantityField;
    private JLabel totalLabel;

    public MakeSale() {
        setTitle("Make Sale");
        setSize(500, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Make Sale");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        JLabel petLabel = new JLabel("Select Pet:");
        petLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(petLabel, gbc);

        petComboBox = new JComboBox<>();
        petComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(petComboBox, gbc);

        JLabel customerLabel = new JLabel("Select Customer:");
        customerLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(customerLabel, gbc);

        customerComboBox = new JComboBox<>();
        customerComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(customerComboBox, gbc);

        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(quantityLabel, gbc);

        quantityField = new JTextField(20);
        quantityField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(quantityField, gbc);

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        totalLabel.setForeground(new Color(0, 153, 0));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(totalLabel, gbc);

        JButton calculateButton = new JButton("Calculate Total");
        calculateButton.setFont(new Font("Arial", Font.PLAIN, 14));
        calculateButton.setBackground(new Color(102, 178, 255));
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setFocusPainted(false);
        calculateButton.addActionListener(e -> calculateTotal());
        gbc.gridy = 5;
        panel.add(calculateButton, gbc);

        JButton saleButton = new JButton("Make Sale");
        saleButton.setFont(new Font("Arial", Font.BOLD, 16));
        saleButton.setBackground(new Color(0, 153, 204));
        saleButton.setForeground(Color.WHITE);
        saleButton.setFocusPainted(false);
        saleButton.addActionListener(e -> makeSale());
        gbc.gridy = 6;
        panel.add(saleButton, gbc);

        add(panel);
        loadPets();
        loadCustomers();
        setVisible(true);
    }

    private void loadPets() {
        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT id, name, species, price, quantity FROM pets WHERE quantity > 0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String item = rs.getInt("id") + " - " + rs.getString("name") +
                        " (" + rs.getString("species") + ") - $" + rs.getDouble("price") +
                        " - Stock: " + rs.getInt("quantity");
                petComboBox.addItem(item);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading pets: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCustomers() {
        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT id, name, phone FROM customers";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String item = rs.getInt("id") + " - " + rs.getString("name") + " (" + rs.getString("phone") + ")";
                customerComboBox.addItem(item);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateTotal() {
        try {
            String petSelection = (String) petComboBox.getSelectedItem();
            if (petSelection == null) return;

            int petId = Integer.parseInt(petSelection.split(" - ")[0]);
            int quantity = Integer.parseInt(quantityField.getText().trim());

            Connection conn = DBConnection.getConnection();
            String sql = "SELECT price FROM pets WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, petId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double price = rs.getDouble("price");
                double total = price * quantity;
                totalLabel.setText(String.format("Total: $%.2f", total));
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void makeSale() {
        try {
            String petSelection = (String) petComboBox.getSelectedItem();
            String customerSelection = (String) customerComboBox.getSelectedItem();

            if (petSelection == null || customerSelection == null) {
                JOptionPane.showMessageDialog(this, "Please select both pet and customer!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int petId = Integer.parseInt(petSelection.split(" - ")[0]);
            int customerId = Integer.parseInt(customerSelection.split(" - ")[0]);
            int quantity = Integer.parseInt(quantityField.getText().trim());

            Connection conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT price, quantity FROM pets WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, petId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int availableQuantity = rs.getInt("quantity");
                double price = rs.getDouble("price");

                if (quantity > availableQuantity) {
                    JOptionPane.showMessageDialog(this, "Not enough stock! Available: " + availableQuantity, "Error", JOptionPane.ERROR_MESSAGE);
                    conn.rollback();
                    conn.close();
                    return;
                }

                double totalPrice = price * quantity;

                String saleSql = "INSERT INTO sales (pet_id, customer_id, quantity, total_price) VALUES (?, ?, ?, ?)";
                PreparedStatement saleStmt = conn.prepareStatement(saleSql);
                saleStmt.setInt(1, petId);
                saleStmt.setInt(2, customerId);
                saleStmt.setInt(3, quantity);
                saleStmt.setDouble(4, totalPrice);
                saleStmt.executeUpdate();

                String updateSql = "UPDATE pets SET quantity = quantity - ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, quantity);
                updateStmt.setInt(2, petId);
                updateStmt.executeUpdate();

                conn.commit();

                JOptionPane.showMessageDialog(this, String.format("Sale completed! Total: $%.2f", totalPrice), "Success", JOptionPane.INFORMATION_MESSAGE);

                quantityField.setText("");
                totalLabel.setText("Total: $0.00");
                petComboBox.removeAllItems();
                loadPets();

                saleStmt.close();
                updateStmt.close();
            }

            rs.close();
            checkStmt.close();
            conn.close();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

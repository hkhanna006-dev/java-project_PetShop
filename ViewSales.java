import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ViewSales extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;

    public ViewSales() {
        setTitle("View Sales History");
        setSize(1000, 500);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Sales History");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        String[] columns = {"Sale ID", "Pet Name", "Customer Name", "Quantity", "Total Price", "Sale Date"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(0, 153, 204));
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 16));
        refreshButton.setBackground(new Color(0, 153, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadSales());
        panel.add(refreshButton, BorderLayout.SOUTH);

        add(panel);
        loadSales();
        setVisible(true);
    }

    private void loadSales() {
        tableModel.setRowCount(0);

        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT s.id, p.name AS pet_name, c.name AS customer_name, " +
                    "s.quantity, s.total_price, s.sale_date " +
                    "FROM sales s " +
                    "JOIN pets p ON s.pet_id = p.id " +
                    "JOIN customers c ON s.customer_id = c.id " +
                    "ORDER BY s.sale_date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("pet_name"),
                        rs.getString("customer_name"),
                        rs.getInt("quantity"),
                        String.format("$%.2f", rs.getDouble("total_price")),
                        rs.getTimestamp("sale_date").toString()
                };
                tableModel.addRow(row);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

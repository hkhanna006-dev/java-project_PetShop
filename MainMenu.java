import javax.swing.*;
import java.awt.*;

public class MainMenu extends JFrame {

    public MainMenu() {
        setTitle("Pet Shop Management System");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Pet Shop Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        String[] buttonLabels = {
                "Add Pet", "View Pets", "Add Customer",
                "Make Sale", "View Sales", "Exit"
        };

        gbc.gridwidth = 1;
        int row = 1;
        int col = 0;

        for (String label : buttonLabels) {
            JButton button = createStyledButton(label);
            gbc.gridx = col;
            gbc.gridy = row;
            mainPanel.add(button, gbc);

            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }

        add(mainPanel);
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setPreferredSize(new Dimension(200, 50));
        button.setBackground(new Color(0, 153, 204));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addActionListener(e -> {
            switch (text) {
                case "Add Pet":
                    new AddPet();
                    break;
                case "View Pets":
                    new ViewPets();
                    break;
                case "Add Customer":
                    new AddCustomer();
                    break;
                case "Make Sale":
                    new MakeSale();
                    break;
                case "View Sales":
                    new ViewSales();
                    break;
                case "Exit":
                    System.exit(0);
                    break;
            }
        });

        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu());
    }
}

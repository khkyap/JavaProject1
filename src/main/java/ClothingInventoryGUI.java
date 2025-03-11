import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ClothingInventoryGUI {
    private ClothingInventory inventory;
    private static final Font STATUS_ICON_FONT = new Font("Arial", Font.BOLD, 14); // Bold font for icons
    private JFrame frame;
    private JPanel panel;
    private JButton addItemButton;
    private JButton leftArrowButton, rightArrowButton, upArrowButton, downArrowButton;
    private JLabel[] categoryHeaders;
    private int categoryOffset = 0;
    private int itemOffset = 0;
    private static final int MAX_DISPLAY_CATEGORIES = 5;
    private static final int MAX_DISPLAY_ITEMS = 10;
    private static final String[] PRESET_CATEGORIES = {
            "", "",
            "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket", "Sweater", "Socks", "Shorts", "Scarf",
            "", ""
    };
    private JLabel totalLabel;
    private double totalPrice = 0.0;
    private Map<JCheckBox, ClothingItem> checkBoxItemMap; // Map checkboxes to ClothingItems
    private JButton confirmButton; // Confirm button for actions

    public ClothingInventoryGUI(ClothingInventory inventory) {
        this.inventory = inventory;
        this.categoryOffset = 2;
        this.checkBoxItemMap = new HashMap<>();
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600); // Increased width to accommodate checkboxes
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(54, 57, 63)); // Dark Gray Theme

        JPanel topPanel = new JPanel(new GridLayout(1, MAX_DISPLAY_CATEGORIES + 2));
        topPanel.setBackground(new Color(47, 49, 54));

        leftArrowButton = createArrowButton("<", e -> navigateCategories(-1));
        topPanel.add(leftArrowButton);

        categoryHeaders = new JLabel[MAX_DISPLAY_CATEGORIES];
        for (int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            categoryHeaders[i] = new JLabel("", SwingConstants.CENTER);
            categoryHeaders[i].setForeground(Color.WHITE);
            categoryHeaders[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            topPanel.add(categoryHeaders[i]);
        }

        rightArrowButton = createArrowButton(">", e -> navigateCategories(1));
        topPanel.add(rightArrowButton);

        panel = new JPanel(new GridLayout(MAX_DISPLAY_ITEMS, 1));
        panel.setBackground(new Color(54, 57, 63));
        refreshInventoryDisplay();

        JPanel navigationPanel = new JPanel(new BorderLayout());
        navigationPanel.setBackground(new Color(54, 57, 63));
        upArrowButton = createArrowButton("▲", e -> navigateItems(-1));
        downArrowButton = createArrowButton("▼", e -> navigateItems(1));

        navigationPanel.add(upArrowButton, BorderLayout.NORTH);
        navigationPanel.add(panel, BorderLayout.CENTER);
        navigationPanel.add(downArrowButton, BorderLayout.SOUTH);

        addItemButton = new JButton("Add Clothing Item");
        addItemButton.setBackground(new Color(88, 101, 242));
        addItemButton.setForeground(Color.WHITE);
        addItemButton.addActionListener(e -> addItemDialog(null, -1, -1));

        // Create the side panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(47, 49, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidePanel.add(totalLabel);

        JButton markSoldButton = new JButton("Mark Sold");
        markSoldButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        markSoldButton.addActionListener(e -> showCheckboxesForAction("Sold"));
        sidePanel.add(markSoldButton);

        JButton markCancelledButton = new JButton("Mark Cancelled");
        markCancelledButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        markCancelledButton.addActionListener(e -> showCheckboxesForAction("Cancelled"));
        sidePanel.add(markCancelledButton);

        JButton addToTotalButton = new JButton("Add to Total");
        addToTotalButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addToTotalButton.addActionListener(e -> addToTotal());
        sidePanel.add(addToTotalButton);

        JButton quickEditButton = new JButton("Quick Edit Price");
        quickEditButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        quickEditButton.addActionListener(e -> quickEditPrice());
        sidePanel.add(quickEditButton);

        confirmButton = new JButton("CONFIRM");
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmButton.setBackground(new Color(0, 128, 0)); // Green color
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmButton.setVisible(false); // Initially hidden
        confirmButton.addActionListener(e -> confirmAction());
        sidePanel.add(confirmButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(navigationPanel, BorderLayout.CENTER);
        frame.add(addItemButton, BorderLayout.SOUTH);
        frame.add(sidePanel, BorderLayout.EAST);
        frame.setVisible(true);
    }

    private void addItemDialog(ClothingItem existingItem, int row, int col) {
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        String currentCategory = PRESET_CATEGORIES[currentCategoryIndex];




        JTextField categoryField = new JTextField(existingItem != null ? existingItem.getCategory() : currentCategory, 20);
        JTextField brandField = new JTextField(existingItem != null ? existingItem.getBrand() : "", 20);
        JTextField nameField = new JTextField(existingItem != null ? existingItem.getName() : "", 20);
        JTextField colorField = new JTextField(existingItem != null ? existingItem.getColor1() : "", 20);
        JTextField sizeField = new JTextField(existingItem != null ? existingItem.getSize() : "", 20);
        JTextField conditionField = new JTextField(existingItem != null ? String.valueOf(existingItem.getCondition()) : "", 20);
        JTextField descriptionField = new JTextField(existingItem != null ? existingItem.getDescription() : "", 20);
        JTextField priceField = new JTextField(existingItem != null ? String.valueOf(existingItem.getPrice()) : "", 20);




        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(47, 49, 54));
        inputPanel.setBorder(BorderFactory.createLineBorder(new Color(68, 81, 222), 2));




        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;




        JLabel titleLabel = new JLabel(existingItem == null ? "Add New Clothing Item" : "Edit Clothing Item", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(titleLabel, gbc);




        gbc.gridwidth = 1;




        String[] labels = {"Category:", "Brand:", "Name:", "Color:", "Size:", "Condition (1-10):", "Description:", "Price:"};
        JTextField[] fields = {categoryField, brandField, nameField, colorField, sizeField, conditionField, descriptionField, priceField};




        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            JLabel label = new JLabel(labels[i]);
            label.setForeground(Color.WHITE);
            inputPanel.add(label, gbc);








            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            fields[i].setBackground(new Color(54, 57, 63));
            fields[i].setForeground(Color.WHITE);
            fields[i].setCaretColor(Color.WHITE);
            inputPanel.add(fields[i], gbc);
        }




        int result = JOptionPane.showConfirmDialog(frame, inputPanel, existingItem == null ? "Add Clothing Item" : "Edit Clothing Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);




        if (result == JOptionPane.OK_OPTION) {
            try {
                String category = categoryField.getText();
                String brand = brandField.getText();
                String name = nameField.getText();
                String color1 = colorField.getText();
                String size = sizeField.getText();
                int condition = Integer.parseInt(conditionField.getText());
                String description = descriptionField.getText();
                double price = Double.parseDouble(priceField.getText());




                if (existingItem == null) {
                    inventory.autoAddItem(category, brand, name, color1, size, condition, description, price);
                } else {
                    inventory.updateItem(row, col, category, brand, name, color1, size, condition, description, price);
                }
                refreshInventoryDisplay();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid input. Please enter valid data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createArrowButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(new Color(88, 101, 242));
        button.setForeground(Color.WHITE);
        button.addActionListener(action);
        return button;
    }




    private void refreshInventoryDisplay() {
        panel.removeAll();
        updateCategoryHeaders();

        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for (int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBackground(new Color(47, 49, 54));
                itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                itemPanel.setPreferredSize(new Dimension(100, 80));

                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, i);
                if (item != null) {
                    // Item name label
                    JLabel label = new JLabel(item.getName(), SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);

                    JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                    iconsPanel.setBackground(new Color(47, 49, 54));

                    // Sold icon ($)
                    JLabel soldIcon = new JLabel("$");
                    soldIcon.setFont(STATUS_ICON_FONT);
                    soldIcon.setForeground(Color.GREEN);
                    soldIcon.setVisible(item.isSold()); // Only show if item is sold

                    // Cancelled icon (X)
                    JLabel cancelledIcon = new JLabel("X");
                    cancelledIcon.setFont(STATUS_ICON_FONT);
                    cancelledIcon.setForeground(Color.RED);
                    cancelledIcon.setVisible(item.isCancelled()); // Only show if item is cancelled

                    // Add icons to the panel
                    iconsPanel.add(cancelledIcon);
                    iconsPanel.add(soldIcon);

                    // Add icons panel to the item panel
                    itemPanel.add(iconsPanel, BorderLayout.EAST);
                } else {
                    JLabel label = new JLabel("Empty", SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);
                }
                panel.add(itemPanel);
            }
        }

        panel.revalidate();
        panel.repaint();
    }
    private void updateCategoryHeaders() {
        for (int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            int categoryIndex = categoryOffset + i;
            if (categoryIndex < PRESET_CATEGORIES.length) {
                categoryHeaders[i].setText(PRESET_CATEGORIES[categoryIndex]);




                if (i == MAX_DISPLAY_CATEGORIES / 2) {
                    categoryHeaders[i].setForeground(Color.GREEN);
                    categoryHeaders[i].setFont(new Font("Arial", Font.BOLD, 14));
                } else {
                    categoryHeaders[i].setForeground(Color.WHITE);
                    categoryHeaders[i].setFont(new Font("Arial", Font.PLAIN, 12));
                }




                if (PRESET_CATEGORIES[categoryIndex].isEmpty()) {
                    categoryHeaders[i].setText("");
                }
            } else {
                categoryHeaders[i].setText("");
            }
        }
    }




    private void navigateCategories(int direction) {
        int newOffset = categoryOffset + direction;




        if (newOffset >= 0 && newOffset + MAX_DISPLAY_CATEGORIES <= PRESET_CATEGORIES.length) {
            categoryOffset = newOffset;
            refreshInventoryDisplay();
        }
    }




    private void navigateItems(int direction) {
        int newOffset = itemOffset + direction;
        if (newOffset >= 0 && newOffset + MAX_DISPLAY_ITEMS <= inventory.getCols()) {
            itemOffset = newOffset;
            refreshInventoryDisplay();
        }
    }


    private void markSoldDialog() {
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            JTextField priceSoldField = new JTextField(20);
            JPanel inputPanel = new JPanel();
            inputPanel.setBackground(new Color(47, 49, 54));
            inputPanel.add(new JLabel("Price Sold:"));
            inputPanel.add(priceSoldField);


            int result = JOptionPane.showConfirmDialog(frame, inputPanel, "Mark Sold", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


            if (result == JOptionPane.OK_OPTION) {
                try {
                    double priceSold = Double.parseDouble(priceSoldField.getText());
                    // Implement logic to mark the item as sold and store the sold price
                    // For now, we'll just print the sold price
                    System.out.println("Item sold for: $" + priceSold);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid price. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }


    private void markCancelled() {
        System.out.println("Item marked as cancelled.");
    }


    private void addToTotal() {
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for (int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, i);
                if (item != null) {
                    totalPrice += item.getPrice();
                }
            }
            totalLabel.setText("Total: $" + String.format("%.2f", totalPrice));
        }
    }


    private void quickEditPrice() {
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            JTextField newPriceField = new JTextField(20);
            JPanel inputPanel = new JPanel();
            inputPanel.setBackground(new Color(47, 49, 54));
            inputPanel.add(new JLabel("New Price:"));
            inputPanel.add(newPriceField);


            int result = JOptionPane.showConfirmDialog(frame, inputPanel, "Quick Edit Price", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


            if (result == JOptionPane.OK_OPTION) {
                try {
                    double newPrice = Double.parseDouble(newPriceField.getText());
                    System.out.println("Price updated to: $" + newPrice);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid price. Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showCheckboxesForAction(String action) {
        // Clear the checkbox map
        checkBoxItemMap.clear();

        // Add checkboxes next to each item
        panel.removeAll();
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for (int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBackground(new Color(47, 49, 54));
                itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, i);
                if (item != null) {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setBackground(new Color(47, 49, 54));
                    checkBox.setForeground(Color.WHITE);
                    itemPanel.add(checkBox, BorderLayout.WEST);

                    JLabel label = new JLabel(item.getName(), SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);

                    checkBoxItemMap.put(checkBox, item); // Map checkbox to item
                } else {
                    JLabel label = new JLabel("Empty", SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);
                }
                panel.add(itemPanel);
            }
        }

        // Show the confirm button
        confirmButton.setVisible(true);
        confirmButton.setActionCommand(action); // Set the action type
        panel.revalidate();
        panel.repaint();
    }

    private void confirmAction() {
        String action = confirmButton.getActionCommand();
        for (Map.Entry<JCheckBox, ClothingItem> entry : checkBoxItemMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                ClothingItem item = entry.getValue();
                if (action.equals("Sold")) {
                    markSold(item);
                } else if (action.equals("Cancelled")) {
                    markCancelled(item);
                }
            }
        }

        // Hide the confirm button and refresh the display
        confirmButton.setVisible(false);
        refreshInventoryDisplay();
    }

    private void markSold(ClothingItem item) {
        item.setSold(true); // Mark the item as sold
        System.out.println("Item sold: " + item.getName());
        refreshInventoryDisplay(); // Refresh the display to show the sold icon
    }

    private void markCancelled(ClothingItem item) {
        item.setCancelled(true); // Mark the item as cancelled
        System.out.println("Item cancelled: " + item.getName());
        refreshInventoryDisplay(); // Refresh the display to show the cancelled icon
    }

public static void main(String[] args) {
    ClothingInventory inventory = new ClothingInventory(10, 10);


    inventory.autoAddItem("Shirt", "Nike", "T-Shirt", "Black", "M", 8, "Cotton T-Shirt", 25.99);
    inventory.autoAddItem("Shirt", "Adidas", "Polo Shirt", "Blue", "L", 9, "Classic Polo", 35.50);
    inventory.autoAddItem("Pants", "Levi's", "Jeans", "Dark Blue", "32x32", 7, "Slim Fit Jeans", 59.99);
    inventory.autoAddItem("Pants", "H&M", "Chinos", "Beige", "34x30", 6, "Casual Chinos", 29.99);
    inventory.autoAddItem("Shoes", "Nike", "Air Max", "White", "10", 9, "Running Shoes", 120.00);
    inventory.autoAddItem("Shoes", "Vans", "Old Skool", "Black", "9", 8, "Skate Shoes", 55.00);
    inventory.autoAddItem("Bag", "Herschel", "Backpack", "Gray", "One Size", 10, "Laptop Backpack", 75.00);
    inventory.autoAddItem("Jacket", "The North Face", "Puffer Jacket", "Black", "L", 9, "Winter Jacket", 199.99);
    inventory.autoAddItem("Sweater", "Uniqlo", "Crewneck Sweater", "Navy", "M", 8, "Wool Blend Sweater", 45.00);
    inventory.autoAddItem("Socks", "Stance", "Ankle Socks", "Multicolor", "S", 7, "Breathable Socks", 12.00);


    new ClothingInventoryGUI(inventory);
}
}

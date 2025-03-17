import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ClothingInventoryGUI {
    private ClothingInventory inventory;
    private FinancialData financialData = new FinancialData();
    private JLabel netWorthLabel, cashBalanceLabel, inventoryValueLabel;
    private static final Font STATUS_ICON_FONT = new Font("Arial", Font.BOLD, 14);
    private JFrame frame;
    private JPanel topPanel;
    private JPanel panel;
    private JButton addItemButton;
    private JButton leftArrowButton, rightArrowButton, upArrowButton, downArrowButton;
    private JLabel[] categoryHeaders;
    private JButton categoryMenuButton;
    private DefaultListModel<String> categoryListModel;
    private int categoryOffset = 0;
    private int itemOffset = 0;
    private static final int MAX_DISPLAY_CATEGORIES = 5;
    private static final int MAX_DISPLAY_ITEMS = 10;
    private JButton newTradeButton;
    private static final String[] PRESET_CATEGORIES = {
            "", "", "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket",
            "Sweater", "Socks", "Shorts", "Scarf", "", ""
    };
    private JLabel totalLabel;
    private double totalPrice = 0.0;
    private Map<JCheckBox, ClothingItem> checkBoxItemMap;
    private JButton confirmButton;
    private Set<ClothingItem> addedItems = new HashSet<>(); // NEW: Track added items

    public ClothingInventoryGUI(ClothingInventory inventory) {
        this.inventory = inventory;
        this.categoryOffset = 2;
        this.checkBoxItemMap = new HashMap<>();
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(54, 57, 63));

        categoryMenuButton = new JButton("≡");
        categoryMenuButton.setFont(new Font("Arial", Font.BOLD, 14));
        categoryMenuButton.addActionListener(e -> showCategoryDialog());
        categoryMenuButton.setPreferredSize(new Dimension(40, 40));

        topPanel.setLayout(new BorderLayout());
        JPanel categoryNavPanel = new JPanel(new GridLayout(1, MAX_DISPLAY_CATEGORIES + 2));
        categoryNavPanel.setBackground(new Color(47, 49, 54));

        topPanel.add(categoryMenuButton, BorderLayout.WEST);
        topPanel.add(categoryNavPanel, BorderLayout.CENTER);

        categoryNavPanel.add(leftArrowButton);
        for(int i=0; i<MAX_DISPLAY_CATEGORIES; i++) {
        leftArrowButton = createArrowButton("<", e -> navigateCategories(-1));
        rightArrowButton = createArrowButton(">", e -> navigateCategories(1));
            categoryHeaders[i].addMouseListener(createCategoryHeaderListener());
        }
        categoryNavPanel.add(rightArrowButton);


        categoryHeaders = new JLabel[MAX_DISPLAY_CATEGORIES];
        for(int i=0; i<MAX_DISPLAY_CATEGORIES; i++) {
            categoryHeaders[i] = new JLabel("", SwingConstants.CENTER);
            categoryHeaders[i].setForeground(Color.WHITE);
            categoryHeaders[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            topPanel.add(categoryHeaders[i]);
        }
        topPanel.add(leftArrowButton);
        topPanel.add(rightArrowButton);


        // Main inventory panel
        panel = new JPanel(new GridLayout(MAX_DISPLAY_ITEMS, 1));
        panel.setBackground(new Color(54, 57, 63));
        refreshInventoryDisplay();

        // Navigation arrows
        JPanel navigationPanel = new JPanel(new BorderLayout());
        navigationPanel.setBackground(new Color(54, 57, 63));
        upArrowButton = createArrowButton("▲", e -> navigateItems(-1));
        downArrowButton = createArrowButton("▼", e -> navigateItems(1));

        navigationPanel.add(upArrowButton, BorderLayout.NORTH);
        navigationPanel.add(panel, BorderLayout.CENTER);
        navigationPanel.add(downArrowButton, BorderLayout.SOUTH);

        // Side panel components
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(47, 49, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Financial panel
        createFinancialPanel(sidePanel);

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setForeground(Color.WHITE);
        sidePanel.add(totalLabel);

        // Action buttons
        JButton markSoldButton = new JButton("Mark Sold");
        markSoldButton.addActionListener(e -> showCheckboxesForAction("Sold"));
        sidePanel.add(markSoldButton);

        JButton markCancelledButton = new JButton("Mark Cancelled");
        markCancelledButton.addActionListener(e -> showCheckboxesForAction("Cancelled"));
        sidePanel.add(markCancelledButton);

        JButton addToTotalButton = new JButton("Add to Total");
        addToTotalButton.addActionListener(e -> showCheckboxesForAction("AddToTotal")); // Modified
        sidePanel.add(addToTotalButton);

        JButton quickEditButton = new JButton("Quick Edit Price");
        quickEditButton.addActionListener(e -> quickEditPrice());
        sidePanel.add(quickEditButton);

        newTradeButton = new JButton("New Trade");
        newTradeButton.addActionListener(e -> new TradeWindow(frame, this).setVisible(true));
        sidePanel.add(newTradeButton);

        confirmButton = new JButton("CONFIRM");
        confirmButton.setBackground(new Color(0, 128, 0));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setVisible(false);
        confirmButton.addActionListener(e -> confirmAction());
        sidePanel.add(confirmButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(navigationPanel, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);
        frame.setVisible(true);
    }

    public ClothingInventory getInventory() {
        return inventory;
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

    private void createFinancialPanel(JPanel sidePanel) {
        JPanel financePanel = new JPanel();
        financePanel.setLayout(new BoxLayout(financePanel, BoxLayout.Y_AXIS));
        financePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(88, 101, 242)),
                        "Financial Summary"
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        financePanel.setBackground(new Color(47, 49, 54));

        netWorthLabel = createFinanceLabel("Net Worth: $0.00");
        cashBalanceLabel = createFinanceLabel("Cash Balance: $0.00");
        inventoryValueLabel = createFinanceLabel("Inventory Value: $0.00");

        JButton editFinanceButton = new JButton("Edit Finances");
        editFinanceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        editFinanceButton.setBackground(new Color(88, 101, 242));
        editFinanceButton.setForeground(Color.WHITE);
        editFinanceButton.addActionListener(e -> showFinanceDialog());

        financePanel.add(netWorthLabel);
        financePanel.add(cashBalanceLabel);
        financePanel.add(inventoryValueLabel);
        financePanel.add(Box.createVerticalStrut(10));
        financePanel.add(editFinanceButton);

        sidePanel.add(financePanel);
        sidePanel.add(Box.createVerticalStrut(20)); // Add spacing
        updateFinancialDisplay(); // Initialize values

    }

    private JLabel createFinanceLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }

    private void updateFinancialDisplay() {
        double inventoryValue = calculateInventoryValue();
        netWorthLabel.setText(String.format("Net Worth: $%.2f",
                financialData.getNetWorth() + inventoryValue));
        cashBalanceLabel.setText(String.format("Cash Balance: $%.2f",
                financialData.getTotalAssets()));
        inventoryValueLabel.setText(String.format("Inventory Value: $%.2f", inventoryValue));
    }

    private double calculateInventoryValue() {
        double total = 0;
        for(int row = 0; row < inventory.getRows(); row++) {
            for(int col = 0; col < inventory.getCols(); col++) {
                ClothingItem item = inventory.getItem(row, col);
                if(item != null && !item.isSold() && !item.isCancelled()) {
                    total += item.getPrice();
                }
            }
        }
        return total;
    }

    private JTextField createFinanceField(double value) {
        JTextField field = new JTextField(String.format("%.2f", value));
        field.setBackground(new Color(54, 57, 63));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(88, 101, 242)));
        return field;
    }
    private void showFinanceDialog() {
        JDialog dialog = new JDialog(frame, "Financial Details", true);
        dialog.setLayout(new GridLayout(0, 2, 10, 5));
        dialog.getContentPane().setBackground(new Color(47, 49, 54));

        // Assets
        JTextField boaField = createFinanceField(financialData.getBankOfAmerica());
        JTextField paypalField = createFinanceField(financialData.getPaypal());
        JTextField fidelityField = createFinanceField(financialData.getFidelity());
        JTextField cashField = createFinanceField(financialData.getPaperCash());

        // Debts
        JTextField bankDebtField = createFinanceField(financialData.getBankDebt());
        JTextField paypalDebtField = createFinanceField(financialData.getPaypalDebt());

        addField(dialog, "Bank of America:", boaField);
        addField(dialog, "PayPal:", paypalField);
        addField(dialog, "Fidelity:", fidelityField);
        addField(dialog, "Paper Cash:", cashField);
        addField(dialog, "Bank Debt:", bankDebtField);
        addField(dialog, "PayPal Debt:", paypalDebtField);

        JButton saveButton = new JButton("Save");
        saveButton.setBackground(new Color(88, 101, 242));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> {
            try {
                financialData.setBankOfAmerica(Double.parseDouble(boaField.getText()));
                financialData.setPaypal(Double.parseDouble(paypalField.getText()));
                financialData.setFidelity(Double.parseDouble(fidelityField.getText()));
                financialData.setPaperCash(Double.parseDouble(cashField.getText()));
                financialData.setBankDebt(Double.parseDouble(bankDebtField.getText()));
                financialData.setPaypalDebt(Double.parseDouble(paypalDebtField.getText()));
                updateFinancialDisplay();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Invalid input! Please enter numbers only.\nExample: 1500.00",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(new JLabel()); // Spacer
        dialog.add(saveButton);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void addField(JDialog dialog, String label, JTextField field) {
        JLabel jlabel = new JLabel(label);
        jlabel.setForeground(Color.WHITE);
        dialog.add(jlabel);
        dialog.add(field);
    }

    private void refreshInventoryDisplay() {
        panel.removeAll();
        updateCategoryHeaders();

        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if(currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for(int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                final int column = i;
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBackground(new Color(47, 49, 54));
                itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                itemPanel.setPreferredSize(new Dimension(100, 80));

                // Hover effects and click handler
                itemPanel.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        itemPanel.setBackground(new Color(64, 68, 75));
                    }
                    public void mouseExited(MouseEvent e) {
                        itemPanel.setBackground(new Color(47, 49, 54));
                    }
                    public void mouseClicked(MouseEvent e) {
                        ClothingItem item = inventory.getItem(currentCategoryIndex - 2, column);
                        if(item != null) {
                            addItemDialog(item, currentCategoryIndex - 2, column);
                        }
                    }
                });

                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, column);
                if(item != null) {
                    JLabel label = new JLabel(item.getName(), SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);

                    JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                    iconsPanel.setBackground(new Color(47, 49, 54));

                    JLabel soldIcon = new JLabel("$");
                    soldIcon.setFont(STATUS_ICON_FONT);
                    soldIcon.setForeground(Color.GREEN);
                    soldIcon.setVisible(item.isSold());

                    JLabel cancelledIcon = new JLabel("X");
                    cancelledIcon.setFont(STATUS_ICON_FONT);
                    cancelledIcon.setForeground(Color.RED);
                    cancelledIcon.setVisible(item.isCancelled());

                    iconsPanel.add(cancelledIcon);
                    iconsPanel.add(soldIcon);
                    itemPanel.add(iconsPanel, BorderLayout.EAST);
                } else {
                    JLabel label = new JLabel("---", SwingConstants.CENTER);
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
        int centerIndex = categoryOffset + MAX_DISPLAY_CATEGORIES/2;
        for (int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            int categoryIndex = categoryOffset + i;
            if (categoryIndex < PRESET_CATEGORIES.length) {
                categoryHeaders[i].setText(PRESET_CATEGORIES[categoryIndex]);

                // Highlight if center category or current selection
                boolean isCenter = (i == MAX_DISPLAY_CATEGORIES/2);
                categoryHeaders[i].setForeground(isCenter ? Color.GREEN : Color.WHITE);
                categoryHeaders[i].setFont(new Font("Arial", isCenter ? Font.BOLD : Font.PLAIN, isCenter ? 14 : 12));

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

    private void addToTotal(ClothingItem item) {
        if (!addedItems.contains(item)) {
            totalPrice += item.getPrice();
            addedItems.add(item);
            totalLabel.setText(String.format("Total: $%.2f", totalPrice));
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
        checkBoxItemMap.clear();
        panel.removeAll();

        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if(currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for(int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBackground(new Color(47, 49, 54));
                itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, i);
                if(item != null) {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setBackground(new Color(47, 49, 54));
                    checkBox.setForeground(Color.WHITE);
                    itemPanel.add(checkBox, BorderLayout.WEST);

                    JLabel label = new JLabel(item.getName(), SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);

                    checkBoxItemMap.put(checkBox, item);
                } else {
                    JLabel label = new JLabel("---", SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    itemPanel.add(label, BorderLayout.CENTER);
                }
                panel.add(itemPanel);
            }
        }
        confirmButton.setActionCommand(action);
        confirmButton.setVisible(true);
        panel.revalidate();
        panel.repaint();
    }

    private JButton createArrowButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(new Color(88, 101, 242));
        button.setForeground(Color.WHITE);
        button.addActionListener(action);
        return button;
    }
    private void confirmAction() {
        String action = confirmButton.getActionCommand();
        for(Map.Entry<JCheckBox, ClothingItem> entry : checkBoxItemMap.entrySet()) {
            if(entry.getKey().isSelected()) {
                ClothingItem item = entry.getValue();
                switch(action) {
                    case "Sold":
                        double newBalance = financialData.getBankOfAmerica() + item.getPrice();
                        financialData.setBankOfAmerica(newBalance);
                        break;
                    case "Cancelled":
                        item.setCancelled(true);
                        break;
                    case "AddToTotal":
                        if(!addedItems.contains(item)) {
                            totalPrice += item.getPrice();
                            addedItems.add(item);
                        }
                        break;
                }
            }
        }
        confirmButton.setVisible(false);
        refreshInventoryDisplay();
        updateFinancialDisplay();
        totalLabel.setText("Total: $" + String.format("%.2f", totalPrice));
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
    private MouseAdapter createCategoryHeaderListener() {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JLabel source = (JLabel) e.getSource();
                for(int i=0; i<categoryHeaders.length; i++) {
                    if(categoryHeaders[i] == source) {
                        int targetCategory = categoryOffset + i;
                        if(targetCategory >= 2 && targetCategory < PRESET_CATEGORIES.length - 2) {
                            categoryOffset = targetCategory - MAX_DISPLAY_CATEGORIES/2;
                            refreshInventoryDisplay();
                        }
                        break;
                    }
                }
            }

            public void mouseEntered(MouseEvent e) {
                ((JComponent)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                ((JComponent)e.getSource()).setCursor(Cursor.getDefaultCursor());
            }
        };
    }

    private void showCategoryDialog() {
        JDialog dialog = new JDialog(frame, "Select Category", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 400);

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterCategories(); }
            public void insertUpdate(DocumentEvent e) { filterCategories(); }
            public void removeUpdate(DocumentEvent e) { filterCategories(); }

            private void filterCategories() {
                String query = searchField.getText().toLowerCase();
                categoryListModel.clear();
                for(String category : PRESET_CATEGORIES) {
                    if(category != null && category.length() > 0) {
                        if(category.toLowerCase().contains(query)) {
                            categoryListModel.addElement(category);
                        }
                    }
                }
            }
        });

        searchPanel.add(new JLabel("Search:"), BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Category list
        categoryListModel = new DefaultListModel<>();
        JList<String> categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterCategories();

        // Select current category
        int currentIndex = categoryOffset + MAX_DISPLAY_CATEGORIES/2;
        categoryList.setSelectedValue(PRESET_CATEGORIES[currentIndex], true);

        categoryList.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                String selected = categoryList.getSelectedValue();
                for(int i=0; i<PRESET_CATEGORIES.length; i++) {
                    if(PRESET_CATEGORIES[i].equals(selected)) {
                        categoryOffset = i - MAX_DISPLAY_CATEGORIES/2;
                        refreshInventoryDisplay();
                        dialog.dispose();
                        break;
                    }
                }
            }
        });

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

public static void main(String[] args) {
    ClothingInventory inventory = new ClothingInventory(30, 30);

    inventory.autoAddItem("Shirt", "Chrome Hearts", "CH Plus Logo T-Shirt", "Black", "M", 9, "Oversized black cotton T-shirt with silver CH Plus logo", 350.00);
    inventory.autoAddItem("Shirt", "Enfants Riches Déprimés", "Destroyed Band Logo Tee", "White", "L", 7, "Heavyweight cotton with screen-printed logo and intentional distress", 280.00);
    inventory.autoAddItem("Shirt", "Undercover", "Scab Graphic T-Shirt", "White", "M", 8, "Distressed 'Scab' print with raw hem detailing", 320.00);
    inventory.autoAddItem("Shirt", "Number (N)ine", "Destroyed Flannel Shirt", "Red/Black", "L", 8, "Vintage-inspired flannel with ripped details", 600.00);
    inventory.autoAddItem("Shirt", "Raf Simons", "RS Logo Long Sleeve", "Navy", "M", 9, "Oversized fit with contrast logo taping", 450.00);

    // Pants
    inventory.autoAddItem("Pants", "Dior Homme", "Slim-Fit Biker Jeans", "Black", "32x32", 9, "Waxed denim with leather knee panels", 1200.00);
    inventory.autoAddItem("Pants", "Helmut Lang", "Bondage Pants", "Black", "34x30", 8, "Leather pants with strap detailing", 780.00);
    inventory.autoAddItem("Pants", "Yohji Yamamoto", "Wide-Leg Trousers", "Charcoal", "34x34", 9, "Wool blend trousers with dramatic drape", 850.00);
    inventory.autoAddItem("Pants", "Junya Watanabe", "Patchwork Denim", "Multi", "32x34", 7, "Distressed denim with patchwork and embroidery", 670.00);
    inventory.autoAddItem("Pants", "Issey Miyake", "Pleated Trousers", "Beige", "S", 9, "Tech fabric with signature pleats", 420.00);

    // Shoes
    inventory.autoAddItem("Shoes", "Rick Owens", "DRKSHDW Ramones", "Black", "10", 9, "High-top sneakers with crepe sole", 550.00);
    inventory.autoAddItem("Shoes", "Maison Margiela", "Replica Tabi Boots", "White", "9", 8, "Split-toe ankle boots in white leather", 890.00);
    inventory.autoAddItem("Shoes", "Visvim", "FBT Shaman Folk", "Brown", "10", 9, "Moccasin-style shoes with elk leather", 950.00);
    inventory.autoAddItem("Shoes", "Balenciaga", "Speed Trainer", "Black", "11", 9, "Stretch-knit sneakers with elastic cuff", 750.00);
    inventory.autoAddItem("Shoes", "Undercover", "Grace Lace-Up Boots", "White", "10", 8, "Chunky sole boots with lace-up closure", 720.00);

    // Jackets
    inventory.autoAddItem("Jacket", "Enfants Riches Déprimés", "Distressed Denim Jacket", "Blue", "L", 8, "Acid wash denim with heavy distressing", 850.00);
    inventory.autoAddItem("Jacket", "Raf Simons", "Consumed Parka", "Olive", "M", 9, "Longline parka with multiple pockets", 2200.00);
    inventory.autoAddItem("Jacket", "Yohji Yamamoto", "Asymmetric Drape Blazer", "Black", "L", 8, "Wool blazer with peak lapels", 1500.00);
    inventory.autoAddItem("Jacket", "Sacai", "Double-Layered Denim", "Blue/Black", "M", 8, "Deconstructed two-layer jacket", 890.00);
    inventory.autoAddItem("Jacket", "Number (N)ine", "A Closing Echo Leather", "Black", "M", 9, "Lambskin jacket with distressed finish", 2400.00);

    // Bags & Accessories
    inventory.autoAddItem("Bag", "Prada", "Nylon Belt Bag", "Black", "One Size", 9, "Re-edition 2000 nylon with logo plaque", 950.00);
    inventory.autoAddItem("Accessories", "Vivienne Westwood", "Orb Choker", "Gold", "One Size", 10, "Gold-plated choker with orb pendant", 320.00);
    inventory.autoAddItem("Accessories", "Alexander McQueen", "Skull Scarf", "Black/Silver", "One Size", 9, "Silk twill skull print scarf", 250.00);
    inventory.autoAddItem("Accessories", "Off-White", "Arrow Belt", "Yellow/Black", "120cm", 9, "Industrial yellow belt with metal details", 200.00);

    // Sweaters
    inventory.autoAddItem("Sweater", "Comme des Garçons", "PLAY Striped", "Navy/White", "M", 9, "Striped sweater with heart logo", 450.00);
    inventory.autoAddItem("Sweater", "Bape", "Shark Hoodie", "Camo", "L", 7, "Camouflage hoodie with shark face", 380.00);
    inventory.autoAddItem("Sweater", "Vetements", "Metal Logo Hoodie", "Gray", "XL", 8, "Oversized with metal lettering", 650.00);

    // Jewelry
    inventory.autoAddItem("Jewelry", "Chrome Hearts", "Cemetery Cross Ring", "Silver", "8", 10, "Sterling silver cross motif ring", 1500.00);
    inventory.autoAddItem("Jewelry", "Chrome Hearts", "Dagger Pendant", "Silver", "One Size", 9, "925 Silver dagger necklace", 1200.00);

    // Special Items
    inventory.autoAddItem("Jacket", "Kapital", "Century Denim", "Indigo", "L", 8, "Sashiko-stitched denim jacket", 680.00);
    inventory.autoAddItem("Shoes", "Saint Laurent", "Wyatt Boots", "Black", "9.5", 8, "Chelsea boots with harness detailing", 990.00);
    inventory.autoAddItem("Jacket", "Comme des Garçons", "Homme Plus Blazer", "Navy", "M", 9, "Deconstructed asymmetric blazer", 1200.00);


    new ClothingInventoryGUI(inventory);
}
}

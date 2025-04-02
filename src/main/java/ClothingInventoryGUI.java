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
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

public class ClothingInventoryGUI {
    private ClothingInventory inventory;
    private FinancialData financialData = new FinancialData();
    private DefaultListModel<String> categoryListModel = new DefaultListModel<>();
    private JLabel netWorthLabel, cashBalanceLabel, inventoryValueLabel;
    private static final Font STATUS_ICON_FONT = new Font("Arial", Font.BOLD, 14);
    private JFrame frame;
    private JPanel topPanel;
    private JPanel panel;
    private JButton addItemButton;
    private JButton leftArrowButton, rightArrowButton, upArrowButton, downArrowButton;
    private JLabel[] categoryHeaders;
    private JButton categoryMenuButton;
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
    private double totalProfit = 0.0;
    private JLabel totalProfitLabel;
    private Map<JCheckBox, ClothingItem> checkBoxItemMap;
    private JButton confirmButton;
    private Set<ClothingItem> addedItems = new HashSet<>();

    public ClothingInventoryGUI(ClothingInventory inventory) {
        this.inventory = inventory;
        this.categoryOffset = 2;
        this.checkBoxItemMap = new HashMap<>();
        this.categoryHeaders = new JLabel[MAX_DISPLAY_CATEGORIES];
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(54, 57, 63));

        categoryHeaders = new JLabel[MAX_DISPLAY_CATEGORIES];
        leftArrowButton = createArrowButton("<", e -> navigateCategories(-1));
        rightArrowButton = createArrowButton(">", e -> navigateCategories(1));
        categoryMenuButton = new JButton("≡");

        categoryMenuButton.setFont(new Font("Arial", Font.BOLD, 14));
        categoryMenuButton.addActionListener(e -> showCategoryDialog());
        categoryMenuButton.setPreferredSize(new Dimension(40, 40));

        JPanel categoryNavPanel = new JPanel(new GridLayout(1, MAX_DISPLAY_CATEGORIES + 2));
        categoryNavPanel.setBackground(new Color(47, 49, 54));

        for(int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            categoryHeaders[i] = new JLabel("", SwingConstants.CENTER);
            categoryHeaders[i].setForeground(Color.WHITE);
            categoryHeaders[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            categoryHeaders[i].addMouseListener(createCategoryHeaderListener());
        }

        categoryNavPanel.add(leftArrowButton);
        for(JLabel header : categoryHeaders) {
            categoryNavPanel.add(header);
        }
        categoryNavPanel.add(rightArrowButton);

        topPanel = new JPanel(new BorderLayout());
        topPanel.add(categoryMenuButton, BorderLayout.WEST);
        topPanel.add(categoryNavPanel, BorderLayout.CENTER);

        panel = new JPanel(new GridLayout(MAX_DISPLAY_ITEMS, 1));
        panel.setBackground(new Color(54, 57, 63));
        refreshInventoryDisplay();

        JPanel navigationPanel = new JPanel(new BorderLayout());
        navigationPanel.setBackground(new Color(54, 57, 63));
        upArrowButton = createArrowButton("▲", e -> navigateItems(-1));
        downArrowButton = createArrowButton("▼", e -> navigateItems(1));

        navigationPanel.add(upArrowButton, BorderLayout.NORTH);
        navigationPanel.add(new JScrollPane(panel), BorderLayout.CENTER);
        navigationPanel.add(downArrowButton, BorderLayout.SOUTH);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(47, 49, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createFinancialPanel(sidePanel);

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setForeground(Color.WHITE);
        sidePanel.add(totalLabel);

        String[] buttonLabels = {"Mark Sold", "Mark Cancelled", "Add to Total", "Clear Total", "Copy Total", "Quick Edit Price", "New Trade", "Export to CSV", "Reset Item Statuses"};
        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.addActionListener(e -> handleSideButtonAction(label));
            sidePanel.add(button);
        }

        confirmButton = new JButton("CONFIRM");
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
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
    private void clearTotal() {
        totalPrice = 0.0;
        addedItems.clear();
        totalLabel.setText("Total: $0.00");
    }
    private void copyTotalToClipboard() {
        String totalText = totalLabel.getText(); // e.g., "Total: $980.00"
        StringSelection selection = new StringSelection(totalText);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
        JOptionPane.showMessageDialog(frame, "Total copied to clipboard!");
    }
    private void handleSideButtonAction(String action) {
        switch(action) {
            case "Mark Sold": showCheckboxesForAction("Sold"); break;
            case "Mark Cancelled": showCheckboxesForAction("Cancelled"); break;
            case "Add to Total": showCheckboxesForAction("AddToTotal"); break;
            case "Clear Total": clearTotal(); break;
            case "Copy Total": copyTotalToClipboard(); break;
            case "Quick Edit Price": quickEditPrice(); break;
            case "Export to CSV": exportToCSV(); break;
            case "Reset Item Statuses": resetAllItemStatuses(); break;
            case "New Trade": new TradeWindow(frame, this).setVisible(true); break;
        }
    }

    public ClothingInventory getInventory() {
        return inventory;
    }
    private void addItemDialog(ClothingItem existingItem, int row, int col) {
        int currentCategoryIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        String currentCategory = PRESET_CATEGORIES[currentCategoryIndex];
        JTextField purchasePriceField = new JTextField(existingItem != null ? String.valueOf(existingItem.getPurchasePrice()) : "", 20);
        JTextField categoryField = new JTextField(existingItem != null ? existingItem.getCategory() : currentCategory, 20);
        JTextField brandField = new JTextField(existingItem != null ? existingItem.getBrand() : "", 20);
        JTextField nameField = new JTextField(existingItem != null ? existingItem.getName() : "", 20);
        JTextField colorField = new JTextField(existingItem != null ? existingItem.getColor1() : "", 20);
        JTextField sizeField = new JTextField(existingItem != null ? existingItem.getSize() : "", 20);
        JTextField conditionField = new JTextField(existingItem != null ? String.valueOf(existingItem.getCondition()) : "", 20);
        JTextField descriptionField = new JTextField(existingItem != null ? existingItem.getDescription() : "", 20);
        JTextField priceField = new JTextField(existingItem != null ? String.valueOf(existingItem.getPrice()) : "", 20);
        JTextField stockField = new JTextField(existingItem != null ?

                String.valueOf(existingItem.getStock()) : "1", 20);

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

        String[] labels = {"Category:", "Brand:", "Name:", "Color:", "Size:", "Condition (1-10):", "Description:", "Price:", "Initial Stock:", "Purchase Price:"};
        JTextField[] fields = {categoryField, brandField, nameField, colorField, sizeField, conditionField, descriptionField, priceField, stockField, purchasePriceField};

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
                int stock = Integer.parseInt(stockField.getText());
                String brand = brandField.getText();
                String name = nameField.getText();
                String color1 = colorField.getText();
                String size = sizeField.getText();
                int condition = Integer.parseInt(conditionField.getText());
                String description = descriptionField.getText();
                double price = Double.parseDouble(priceField.getText());




                if (existingItem == null) {
                    double purchasePrice = Double.parseDouble(purchasePriceField.getText());
                    inventory.autoAddItem(category, brand, name, color1, size, condition, description, price, stock, purchasePrice);
                } else {
                    inventory.updateItem(row, col, category, brand, name, color1, size, condition, description, price, stock);
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
        totalProfitLabel = createFinanceLabel("Total Profit: $0.00");
        financePanel.add(totalProfitLabel);
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
        sidePanel.add(Box.createVerticalStrut(20));
        updateFinancialDisplay();

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
        totalProfitLabel.setText(String.format("Total Profit: $%.2f", totalProfit));
    }

    private double calculateInventoryValue() {
        double total = 0;
        for(int row = 0; row < inventory.getRows(); row++) {
            for(int col = 0; col < inventory.getCols(); col++) {
                ClothingItem item = inventory.getItem(row, col);
                if(item != null && !item.isSold() && !item.isCancelled()) {
                    total += item.getPrice() * item.getStock();
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

        dialog.add(new JLabel());
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
        if (currentCategoryIndex >= 2 && currentCategoryIndex < PRESET_CATEGORIES.length - 2) {
            for (int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS && i < inventory.getCols(); i++) {
                final int column = i;
                JPanel itemPanel = new JPanel(new BorderLayout());
                itemPanel.setBackground(new Color(47, 49, 54));
                itemPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                itemPanel.setPreferredSize(new Dimension(100, 80));

                itemPanel.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        itemPanel.setBackground(new Color(64, 68, 75));
                    }

                    public void mouseExited(MouseEvent e) {
                        itemPanel.setBackground(new Color(47, 49, 54));
                    }

                    public void mouseClicked(MouseEvent e) {
                        ClothingItem item = inventory.getItem(currentCategoryIndex - 2, column);
                        if (item != null) {
                            addItemDialog(item, currentCategoryIndex - 2, column);
                        }
                    }
                });

                ClothingItem item = inventory.getItem(currentCategoryIndex - 2, column);
                if (item != null) {
                    // Stock Panel (LEFT)
                    JPanel stockPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                    stockPanel.setBackground(new Color(47, 49, 54));

                    JButton decrementButton = createStockButton("➖", new Color(255, 105, 97));
                    JLabel stockLabel = new JLabel(String.valueOf(item.getStock()));
                    JButton incrementButton = createStockButton("➕", new Color(119, 221, 119));

                    stockLabel.setForeground(Color.WHITE);
                    stockLabel.setFont(new Font("Arial", Font.BOLD, 14));
                    stockLabel.setPreferredSize(new Dimension(30, 20));
                    stockLabel.setHorizontalAlignment(SwingConstants.CENTER);

                    decrementButton.addActionListener(e -> {
                        if (item.getStock() > 0) {
                            item.adjustStock(-1);
                            stockLabel.setText(String.valueOf(item.getStock()));
                            updateFinancialDisplay();
                        }
                    });

                    incrementButton.addActionListener(e -> {
                        item.adjustStock(1);
                        stockLabel.setText(String.valueOf(item.getStock()));
                        updateFinancialDisplay();
                    });

                    stockPanel.add(decrementButton);
                    stockPanel.add(stockLabel);
                    stockPanel.add(incrementButton);
                    itemPanel.add(stockPanel, BorderLayout.WEST);

                    // Text Panel (CENTER)
                    JPanel textPanel = new JPanel();
                    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                    textPanel.setBackground(new Color(47, 49, 54));

                    JLabel brandLabel = new JLabel(item.getBrand(), SwingConstants.CENTER);
                    brandLabel.setForeground(new Color(255, 165, 0));
                    brandLabel.setFont(new Font("Arial", Font.BOLD, 14));
                    brandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    JLabel nameLabel = new JLabel(item.getName(), SwingConstants.CENTER);
                    nameLabel.setForeground(Color.WHITE);
                    nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    JPanel infoLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                    infoLine.setBackground(new Color(47, 49, 54));

                    JLabel catSizeLabel = new JLabel(item.getCategory() + " | " + item.getSize());
                    catSizeLabel.setForeground(Color.WHITE);
                    catSizeLabel.setFont(new Font("Arial", Font.PLAIN, 12));

                    JLabel priceLabel = new JLabel(String.format("$%.2f", item.getPrice()));
                    priceLabel.setForeground(new Color(0, 255, 127));
                    priceLabel.setFont(new Font("Arial", Font.BOLD, 14));

                    infoLine.add(catSizeLabel);
                    infoLine.add(priceLabel);

                    textPanel.add(brandLabel);
                    textPanel.add(nameLabel);
                    textPanel.add(infoLine);

                    itemPanel.add(textPanel, BorderLayout.CENTER);

                    JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                    iconsPanel.setBackground(new Color(47, 49, 54));

                    JLabel cancelledIcon = new JLabel("X");
                    cancelledIcon.setFont(new Font("Arial", Font.BOLD, 24));
                    cancelledIcon.setForeground(Color.RED);
                    cancelledIcon.setVisible(item.isCancelled());

                    JLabel soldIcon = new JLabel("$");
                    soldIcon.setFont(new Font("Arial", Font.BOLD, 26));
                    soldIcon.setForeground(new Color(0, 255, 127));
                    soldIcon.setVisible(item.isSold());

                    if (item.isSold()) {
                        soldIcon.setToolTipText("Sold for $" + String.format("%.2f", item.getPrice()));
                    }
                    if (item.isCancelled()) {
                        cancelledIcon.setToolTipText("This item was cancelled");
                    }
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


    private JButton createStockButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        button.setForeground(color);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setForeground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setForeground(color);
            }
        });

        return button;
    }
    private void updateCategoryHeaders() {
        int centerIndex = categoryOffset + MAX_DISPLAY_CATEGORIES/2;
        for (int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            int categoryIndex = categoryOffset + i;
            if (categoryIndex < PRESET_CATEGORIES.length) {
                categoryHeaders[i].setText(PRESET_CATEGORIES[categoryIndex]);

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
        int maxOffset = PRESET_CATEGORIES.length - MAX_DISPLAY_CATEGORIES;
        categoryOffset = Math.max(0, Math.min(newOffset, maxOffset));
        refreshInventoryDisplay();
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

    private void resetAllItemStatuses() {
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to unmark all items as Sold or Cancelled?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            for (int row = 0; row < inventory.getRows(); row++) {
                for (int col = 0; col < inventory.getCols(); col++) {
                    ClothingItem item = inventory.getItem(row, col);
                    if (item != null) {
                        item.setSold(false);
                        item.setCancelled(false);
                    }
                }
            }

            refreshInventoryDisplay();
            JOptionPane.showMessageDialog(frame, "All item statuses have been reset.");
        }
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
    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("clothing_inventory_export.csv"))) {
            writer.println("Category,Brand,Name,Color,Size,Condition,Description,Price,Purchase Price,Profit,Stock,Sold,Cancelled");

            for (int row = 0; row < inventory.getRows(); row++) {
                for (int col = 0; col < inventory.getCols(); col++) {
                    ClothingItem item = inventory.getItem(row, col);
                    if (item != null) {
                        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",%.2f,%.2f,%.2f,%d,%b,%b",
                                item.getCategory(), item.getBrand(), item.getName(), item.getColor1(),
                                item.getSize(), item.getCondition(), item.getDescription(),
                                item.getPrice(), item.getPurchasePrice(), item.getProfit(),
                                item.getStock(), item.isSold(), item.isCancelled());
                        writer.println(line);
                    }
                }
            }

            JOptionPane.showMessageDialog(frame, "Inventory exported to clothing_inventory_export.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to export: " + e.getMessage());
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
                        handleMultiSoldInput();
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
        item.setSold(true);
        System.out.println("Item sold: " + item.getName());
        refreshInventoryDisplay();
    }
    private void handleMultiSoldInput() {
        Map<ClothingItem, JTextField> itemPriceFields = new LinkedHashMap<>();

        for (Map.Entry<JCheckBox, ClothingItem> entry : checkBoxItemMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                ClothingItem item = entry.getValue();
                JTextField priceField = new JTextField(10);
                priceField.setBackground(new Color(54, 57, 63));
                priceField.setForeground(Color.WHITE);
                priceField.setCaretColor(Color.WHITE);
                itemPriceFields.put(item, priceField);
            }
        }

        if (itemPriceFields.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No items selected.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create dialog panel
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        dialogPanel.setBackground(new Color(180, 180, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        dialogPanel.add(new JLabel("Item"), gbc);
        gbc.gridx = 1;
        dialogPanel.add(new JLabel("Sold Price"), gbc);

        gbc.gridy++;

        for (Map.Entry<ClothingItem, JTextField> entry : itemPriceFields.entrySet()) {
            gbc.gridx = 0;
            JLabel nameLabel = new JLabel(entry.getKey().getName());
            nameLabel.setForeground(Color.WHITE);
            dialogPanel.add(nameLabel, gbc);

            gbc.gridx = 1;
            dialogPanel.add(entry.getValue(), gbc);

            gbc.gridy++;
        }

        int result = JOptionPane.showConfirmDialog(frame, dialogPanel, "Enter Sold Prices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            boolean success = true;

            for (Map.Entry<ClothingItem, JTextField> entry : itemPriceFields.entrySet()) {
                ClothingItem item = entry.getKey();
                JTextField priceField = entry.getValue();
                try {
                    double soldPrice = Double.parseDouble(priceField.getText());
                    item.setSold(true);
                    item.setProfit(soldPrice - item.getPurchasePrice());
                    item.setPrice(soldPrice);
                    totalProfit += item.getProfit();
                    financialData.setBankOfAmerica(financialData.getBankOfAmerica() + soldPrice);

                } catch (NumberFormatException ex) {
                    success = false;
                    JOptionPane.showMessageDialog(frame,
                            "Invalid price for item: " + item.getName(),
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

            if (success) {
                confirmButton.setVisible(false);
                refreshInventoryDisplay();
                updateFinancialDisplay();
            }
        }
        confirmButton.setVisible(false);
        checkBoxItemMap.clear();

    }


    private void markCancelled(ClothingItem item) {
        item.setCancelled(true);
        System.out.println("Item cancelled: " + item.getName());
        refreshInventoryDisplay();
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

    private void filterCategories(String query) {
        categoryListModel.clear();

        String searchQuery = query.toLowerCase().trim();

        for (String category : PRESET_CATEGORIES) {
            if (category == null || category.isEmpty()) {
                continue;
            }

            if (category.toLowerCase().contains(searchQuery)) {
                categoryListModel.addElement(category);
            }
        }
    }

    private void showCategoryDialog() {
        JDialog dialog = new JDialog(frame, "Select Category", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(300, 400);

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

        categoryListModel = new DefaultListModel<>();
        JList<String> categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


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
    try {


    ClothingInventory inventory = new ClothingInventory(PRESET_CATEGORIES.length, 30);


    inventory.autoAddItem("Shirt", "Chrome Hearts", "CH Plus Logo T-Shirt", "Black", "M", 9, "Oversized black cotton T-shirt with silver CH Plus logo", 350.00, 1);
    inventory.autoAddItem("Shirt", "Enfants Riches Déprimés", "Destroyed Band Logo Tee", "White", "L", 7, "Heavyweight cotton with screen-printed logo and intentional distress", 280.00, 1);
    inventory.autoAddItem("Shirt", "Undercover", "Scab Graphic T-Shirt", "White", "M", 8, "Distressed 'Scab' print with raw hem detailing", 320.00, 1);
    inventory.autoAddItem("Shirt", "Number (N)ine", "Destroyed Flannel Shirt", "Red/Black", "L", 8, "Vintage-inspired flannel with ripped details", 600.00, 1);
    inventory.autoAddItem("Shirt", "Raf Simons", "RS Logo Long Sleeve", "Navy", "M", 9, "Oversized fit with contrast logo taping", 450.00, 1);

    inventory.autoAddItem("Pants", "Dior Homme", "Slim-Fit Biker Jeans", "Black", "32x32", 9, "Waxed denim with leather knee panels", 1200.00, 1);
    inventory.autoAddItem("Pants", "Helmut Lang", "Bondage Pants", "Black", "34x30", 8, "Leather pants with strap detailing", 780.00, 1);
    inventory.autoAddItem("Pants", "Yohji Yamamoto", "Wide-Leg Trousers", "Charcoal", "34x34", 9, "Wool blend trousers with dramatic drape", 850.00, 1);
    inventory.autoAddItem("Pants", "Junya Watanabe", "Patchwork Denim", "Multi", "32x34", 7, "Distressed denim with patchwork and embroidery", 670.00, 1);
    inventory.autoAddItem("Pants", "Issey Miyake", "Pleated Trousers", "Beige", "S", 9, "Tech fabric with signature pleats", 420.00, 1);

    inventory.autoAddItem("Shoes", "Rick Owens", "DRKSHDW Ramones", "Black", "10", 9, "High-top sneakers with crepe sole", 550.00, 1);
    inventory.autoAddItem("Shoes", "Maison Margiela", "Replica Tabi Boots", "White", "9", 8, "Split-toe ankle boots in white leather", 890.00, 1);
    inventory.autoAddItem("Shoes", "Visvim", "FBT Shaman Folk", "Brown", "10", 9, "Moccasin-style shoes with elk leather", 950.00, 1);
    inventory.autoAddItem("Shoes", "Balenciaga", "Speed Trainer", "Black", "11", 9, "Stretch-knit sneakers with elastic cuff", 750.00, 1);
    inventory.autoAddItem("Shoes", "Undercover", "Grace Lace-Up Boots", "White", "10", 8, "Chunky sole boots with lace-up closure", 720.00, 1);

    inventory.autoAddItem("Jacket", "Enfants Riches Déprimés", "Distressed Denim Jacket", "Blue", "L", 8, "Acid wash denim with heavy distressing", 850.00, 1);
    inventory.autoAddItem("Jacket", "Raf Simons", "Consumed Parka", "Olive", "M", 9, "Longline parka with multiple pockets", 2200.00, 1);
    inventory.autoAddItem("Jacket", "Yohji Yamamoto", "Asymmetric Drape Blazer", "Black", "L", 8, "Wool blazer with peak lapels", 1500.00, 1);
    inventory.autoAddItem("Jacket", "Sacai", "Double-Layered Denim", "Blue/Black", "M", 8, "Deconstructed two-layer jacket", 890.00, 1);
    inventory.autoAddItem("Jacket", "Number (N)ine", "A Closing Echo Leather", "Black", "M", 9, "Lambskin jacket with distressed finish", 2400.00, 1);

    inventory.autoAddItem("Bag", "Prada", "Nylon Belt Bag", "Black", "One Size", 9, "Re-edition 2000 nylon with logo plaque", 950.00, 1);
    inventory.autoAddItem("Accessories", "Vivienne Westwood", "Orb Choker", "Gold", "One Size", 10, "Gold-plated choker with orb pendant", 320.00, 1);
    inventory.autoAddItem("Accessories", "Alexander McQueen", "Skull Scarf", "Black/Silver", "One Size", 9, "Silk twill skull print scarf", 250.00, 1);
    inventory.autoAddItem("Accessories", "Off-White", "Arrow Belt", "Yellow/Black", "120cm", 9, "Industrial yellow belt with metal details", 200.00, 1);

    inventory.autoAddItem("Sweater", "Comme des Garçons", "PLAY Striped", "Navy/White", "M", 9, "Striped sweater with heart logo", 450.00, 1);
    inventory.autoAddItem("Sweater", "Bape", "Shark Hoodie", "Camo", "L", 7, "Camouflage hoodie with shark face", 380.00, 1);
    inventory.autoAddItem("Sweater", "Vetements", "Metal Logo Hoodie", "Gray", "XL", 8, "Oversized with metal lettering", 650.00, 1);

    inventory.autoAddItem("Jewelry", "Chrome Hearts", "Cemetery Cross Ring", "Silver", "8", 10, "Sterling silver cross motif ring", 1500.00, 1);
    inventory.autoAddItem("Jewelry", "Chrome Hearts", "Dagger Pendant", "Silver", "One Size", 9, "925 Silver dagger necklace", 1200.00, 1);

    inventory.autoAddItem("Jacket", "Kapital", "Century Denim", "Indigo", "L", 8, "Sashiko-stitched denim jacket", 680.00, 1);
    inventory.autoAddItem("Shoes", "Saint Laurent", "Wyatt Boots", "Black", "9.5", 8, "Chelsea boots with harness detailing", 990.00, 1);
    inventory.autoAddItem("Jacket", "Comme des Garçons", "Homme Plus Blazer", "Navy", "M", 9, "Deconstructed asymmetric blazer", 1200.00, 1);


    new ClothingInventoryGUI(inventory);


    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Critical error: " + e.getMessage());
    }
}
}

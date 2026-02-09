import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.Timer;

public class ClothingInventoryGUI {
    private ClothingInventory inventory;
    private FinancialData financialData = new FinancialData();
    private DefaultListModel<String> categoryListModel = new DefaultListModel<>();
    private JLabel netWorthLabel, cashBalanceLabel, inventoryValueLabel;
    private JFrame frame;
    private JPanel panel;
    private JTabbedPane tabbedPane;
    private JPanel activePanel;
    private JPanel soldPanel;
    private JButton leftArrowButton, rightArrowButton, upArrowButton, downArrowButton;
    private JLabel[] categoryHeaders;
    private int categoryOffset = 0;
    private int itemOffset = 0;
    private static final int MAX_DISPLAY_CATEGORIES = 5;
    private static final int MAX_DISPLAY_ITEMS = 10;
    private static final String[] PRESET_CATEGORIES = {
            "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket", "Sweater", "Socks",
            "Shorts", "Scarf", "Jewelry", "Accessories", "Outerwear", "Leather"
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
        this.checkBoxItemMap = new HashMap<>();
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(54, 57, 63));

        categoryHeaders = new JLabel[MAX_DISPLAY_CATEGORIES];
        leftArrowButton = createArrowButton("<", e -> navigateCategories(-1));
        rightArrowButton = createArrowButton(">", e -> navigateCategories(1));

        JButton categoryMenuButton = new JButton("≡");
        categoryMenuButton.setFont(new Font("Arial", Font.BOLD, 14));
        categoryMenuButton.addActionListener(e -> showCategoryDialog());

        JPanel categoryNavPanel = new JPanel(new GridLayout(1, MAX_DISPLAY_CATEGORIES + 2));
        categoryNavPanel.setBackground(new Color(47, 49, 54));
        categoryNavPanel.add(leftArrowButton);
        for(int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            categoryHeaders[i] = new JLabel("", SwingConstants.CENTER);
            categoryHeaders[i].setForeground(Color.WHITE);
            categoryHeaders[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            categoryHeaders[i].addMouseListener(createCategoryHeaderListener());
            categoryNavPanel.add(categoryHeaders[i]);
        }
        categoryNavPanel.add(rightArrowButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(categoryMenuButton, BorderLayout.WEST);
        topPanel.add(categoryNavPanel, BorderLayout.CENTER);

        panel = new JPanel(new GridLayout(MAX_DISPLAY_ITEMS, 1, 0, 5));
        panel.setBackground(new Color(54, 57, 63));

        JPanel navigationPanel = new JPanel(new BorderLayout());
        upArrowButton = createArrowButton("▲", e -> navigateItems(-1));
        downArrowButton = createArrowButton("▼", e -> navigateItems(1));
        navigationPanel.add(upArrowButton, BorderLayout.NORTH);
        navigationPanel.add(new JScrollPane(panel), BorderLayout.CENTER);
        navigationPanel.add(downArrowButton, BorderLayout.SOUTH);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(47, 49, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidePanel.setPreferredSize(new Dimension(300, 0));

        createFinancialPanel(sidePanel);

        totalLabel = new JLabel("Cart Total: $0.00");
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidePanel.add(totalLabel);
        sidePanel.add(Box.createVerticalStrut(10));

        JPanel buttonGridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        buttonGridPanel.setBackground(new Color(47, 49, 54));
        buttonGridPanel.setMaximumSize(new Dimension(280, 300));

        String[] buttonLabels = {"Mark Sold", "Mark Cancelled", "Add to Total", "Clear Total", "Copy Total", "Quick Edit Price", "Export to CSV", "Reset Item Statuses", "New Trade"};
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.setBackground(suggestColor(label));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Arial", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> handleSideButtonAction(label));
            buttonGridPanel.add(btn);
        }
        sidePanel.add(buttonGridPanel);

        JButton addItemButton = new JButton("Create New Item");
        addItemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addItemButton.setMaximumSize(new Dimension(280, 40));
        addItemButton.setBackground(new Color(88, 101, 242));
        addItemButton.setForeground(Color.WHITE);
        addItemButton.setFont(new Font("Arial", Font.BOLD, 14));
        addItemButton.addActionListener(e -> addItemDialog(null));
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(addItemButton);

        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(new Color(47, 49, 54));
        confirmButton = new JButton("CONFIRM ACTION");
        confirmButton.setVisible(false);
        confirmButton.setBackground(new Color(0, 102, 51));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(new Font("Arial", Font.BOLD, 16));
        confirmButton.setPreferredSize(new Dimension(260, 50));
        confirmButton.addActionListener(e -> confirmAction());
        actionPanel.add(confirmButton);

        sidePanel.add(Box.createVerticalGlue());
        sidePanel.add(actionPanel);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(navigationPanel, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);

        refreshInventoryDisplay();
        frame.setVisible(true);
    }

    private List<ClothingItem> getItemsInCurrentCategory() {
        int centerIndex = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        if (centerIndex < 0 || centerIndex >= PRESET_CATEGORIES.length) return new ArrayList<>();
        String currentCat = PRESET_CATEGORIES[centerIndex];
        return inventory.getInventory().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(currentCat))
                .collect(Collectors.toList());
    }

    private void refreshInventoryDisplay() {
        panel.removeAll();
        updateCategoryHeaders();
        List<ClothingItem> filtered = getItemsInCurrentCategory();

        Timer fadeIn = new Timer(10, new ActionListener() {
            int count = itemOffset;
            int displayIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (displayIndex < MAX_DISPLAY_ITEMS) {
                    JPanel itemPanel = createSleekItemPanel(count < filtered.size() ? filtered.get(count) : null);
                    panel.add(itemPanel);
                    panel.revalidate();
                    panel.repaint();
                    count++;
                    displayIndex++;
                } else {
                    ((Timer)e.getSource()).stop();
                }
            }
        });
        fadeIn.start();
    }

    private JPanel createSleekItemPanel(ClothingItem item) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(new Color(47, 49, 54));
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(32, 34, 37), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        if (item != null) {
            JPanel identityPanel = new JPanel(new GridLayout(2, 1));
            identityPanel.setOpaque(false);
            JLabel brandLabel = new JLabel(item.getBrand().toUpperCase());
            brandLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            brandLabel.setForeground(Color.WHITE);
            JLabel nameLabel = new JLabel(item.getName());
            nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            nameLabel.setForeground(new Color(185, 187, 190));
            identityPanel.add(brandLabel);
            identityPanel.add(nameLabel);

            JPanel financeMatrix = new JPanel(new GridLayout(1, 4, 20, 0));
            financeMatrix.setOpaque(false);

            financeMatrix.add(createStatSubPanel("PURCHASE", String.format("$%.0f", item.getPurchasePrice()), new Color(114, 137, 218)));
            financeMatrix.add(createStatSubPanel("ESTIMATE", String.format("$%.0f", item.getPrice()), new Color(255, 255, 255)));

            String soldVal = item.isSold() ? String.format("$%.0f", item.getPrice()) : "—";
            financeMatrix.add(createStatSubPanel("SOLD", soldVal, new Color(255, 165, 0)));

            String profitVal = "";
            if (item.isSold()) {
                double profitAmt = item.getPrice() - item.getPurchasePrice();
                double margin = (item.getPurchasePrice() > 0) ? (profitAmt / item.getPurchasePrice()) * 100 : 0;
                profitVal = String.format("$%.0f (%.0f%%)", profitAmt, margin);
            }
            financeMatrix.add(createStatSubPanel("PROFIT", profitVal, new Color(0, 255, 127)));

            itemPanel.add(identityPanel, BorderLayout.WEST);
            itemPanel.add(financeMatrix, BorderLayout.EAST);

            itemPanel.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { itemPanel.setBackground(new Color(64, 68, 75)); }
                public void mouseExited(MouseEvent e) { itemPanel.setBackground(new Color(47, 49, 54)); }
                public void mouseClicked(MouseEvent e) { addItemDialog(item); }
            });
        } else {
            itemPanel.add(new JLabel("---", SwingConstants.CENTER), BorderLayout.CENTER);
        }
        return itemPanel;
    }

    private JPanel createStatSubPanel(String label, String value, Color valColor) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);

        JLabel l = new JLabel(label, SwingConstants.RIGHT);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(new Color(114, 118, 125));

        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(new Font("Monospaced", Font.BOLD, 12));
        v.setForeground(valColor);

        p.add(l);
        p.add(v);
        p.setPreferredSize(new Dimension(80, 30));
        return p;
    }

    private void addItemDialog(ClothingItem existingItem) {
        JTextField catField = new JTextField(existingItem != null ? existingItem.getCategory() : "");
        JTextField brandField = new JTextField(existingItem != null ? existingItem.getBrand() : "");
        JTextField nameField = new JTextField(existingItem != null ? existingItem.getName() : "");
        JTextField priceField = new JTextField(existingItem != null ? String.valueOf(existingItem.getPrice()) : "0.0");
        JTextField stockField = new JTextField(existingItem != null ? String.valueOf(existingItem.getStock()) : "1");
        JTextField buyPriceField = new JTextField(existingItem != null ? String.valueOf(existingItem.getPurchasePrice()) : "0.0");

        Object[] message = {"Category:", catField, "Brand:", brandField, "Name:", nameField, "Price:", priceField, "Stock:", stockField, "Purchase Price:", buyPriceField};
        int option = JOptionPane.showConfirmDialog(frame, message, "Item Details", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (existingItem == null) {
                    ClothingItem newItem = new ClothingItem(catField.getText(), brandField.getText(), nameField.getText(), "", "", 10, "", Double.parseDouble(priceField.getText()), Integer.parseInt(stockField.getText()), Double.parseDouble(buyPriceField.getText()));
                    inventory.addItem(newItem);
                } else {
                    existingItem.setCategory(catField.getText());
                    existingItem.setBrand(brandField.getText());
                    existingItem.setName(nameField.getText());
                    existingItem.setPrice(Double.parseDouble(priceField.getText()));
                    existingItem.setStock(Integer.parseInt(stockField.getText()));
                    existingItem.setPurchasePrice(Double.parseDouble(buyPriceField.getText()));
                }
                refreshInventoryDisplay();
                updateFinancialDisplay();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Invalid input.");
            }
        }
    }

    private void handleSideButtonAction(String action) {
        switch(action) {
            case "Mark Sold": showCheckboxesForAction("Sold"); break;
            case "Mark Cancelled": showCheckboxesForAction("Cancelled"); break;
            case "Add to Total": showCheckboxesForAction("AddToTotal"); break;
            case "Clear Total": totalPrice = 0; addedItems.clear(); totalLabel.setText("Total: $0.00"); break;
            case "Copy Total": copyTotalToClipboard(); break;
            case "Export to CSV": exportToCSV(); break;
            case "Reset Item Statuses": resetAllItemStatuses(); break;
            case "New Trade": new TradeWindow(frame, this).setVisible(true); break;
        }
    }

    private void showCheckboxesForAction(String action) {
        checkBoxItemMap.clear();
        panel.removeAll();
        List<ClothingItem> filtered = getItemsInCurrentCategory();

        for (int i = itemOffset; i < itemOffset + MAX_DISPLAY_ITEMS; i++) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBackground(new Color(47, 49, 54));
            if (i < filtered.size()) {
                ClothingItem item = filtered.get(i);
                JCheckBox cb = new JCheckBox(item.getName());
                cb.setBackground(new Color(47, 49, 54));
                cb.setForeground(Color.WHITE);
                checkBoxItemMap.put(cb, item);
                itemPanel.add(cb, BorderLayout.CENTER);
            }
            panel.add(itemPanel);
        }
        confirmButton.setActionCommand(action);
        confirmButton.setVisible(true);
        panel.revalidate();
        panel.repaint();
    }

    private void confirmAction() {
        String action = confirmButton.getActionCommand();
        for (Map.Entry<JCheckBox, ClothingItem> entry : checkBoxItemMap.entrySet()) {
            if (entry.getKey().isSelected()) {
                ClothingItem item = entry.getValue();
                if (action.equals("Sold")) {
                    item.setSold(true);
                    totalProfit += (item.getPrice() - item.getPurchasePrice());
                }
                else if (action.equals("Cancelled")) {
                    item.setCancelled(true);
                }
                else if (action.equals("AddToTotal") && !addedItems.contains(item)) {
                    totalPrice += item.getPrice();
                    addedItems.add(item);
                }
            }
        }
        totalLabel.setText(String.format("Cart Total: $%.2f", totalPrice));
        confirmButton.setVisible(false);
        refreshInventoryDisplay();
        updateFinancialDisplay();
    }

    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("inventory.csv"))) {
            writer.println("Category,Brand,Name,Price,Stock");
            for (ClothingItem item : inventory.getInventory()) {
                writer.printf("%s,%s,%s,%.2f,%d%n", item.getCategory(), item.getBrand(), item.getName(), item.getPrice(), item.getStock());
            }
            JOptionPane.showMessageDialog(frame, "Exported.");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void resetAllItemStatuses() {
        for (ClothingItem item : inventory.getInventory()) {
            item.setSold(false);
            item.setCancelled(false);
        }
        refreshInventoryDisplay();
    }

    private double calculateInventoryValue() {
        return inventory.getInventory().stream()
                .filter(item -> !item.isSold() && !item.isCancelled())
                .mapToDouble(item -> item.getPrice() * item.getStock())
                .sum();
    }

    private void updateFinancialDisplay() {
        double currentInvValue = calculateInventoryValue();

        double realizedProfit = inventory.getInventory().stream()
                .filter(ClothingItem::isSold)
                .mapToDouble(item -> item.getPrice() - item.getPurchasePrice())
                .sum();

        double totalLiquid = financialData.getTotalAssets() + realizedProfit;
        double netWorth = totalLiquid + currentInvValue - financialData.getTotalDebt();

        inventoryValueLabel.setText("<html><font color='#B9BBBE'>Inventory:</font> <font color='#FFFFFF'>$" + String.format("%.2f", currentInvValue) + "</font></html>");
        totalProfitLabel.setText("<html><font color='#B9BBBE'>Lifetime Profit:</font> <font color='##3eed7e'>$" + String.format("%.2f", realizedProfit) + "</font></html>");
        netWorthLabel.setText("<html><font color='#B9BBBE'>Net Worth:</font> <font color='#5865F2'>$" + String.format("%.2f", netWorth) + "</font></html>");
    }

    private void createFinancialPanel(JPanel sidePanel) {
        JPanel financePanel = new JPanel();
        financePanel.setLayout(new BoxLayout(financePanel, BoxLayout.Y_AXIS));
        financePanel.setBackground(new Color(40, 43, 48));
        financePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(66, 70, 77), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        financePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        financePanel.setMaximumSize(new Dimension(280, 200));

        inventoryValueLabel = createModernMetric("INVENTORY VALUE", "$0.00", Color.WHITE);
        totalProfitLabel = createModernMetric("LIFETIME PROFIT", "$0.00", new Color(0, 255, 127));
        netWorthLabel = createModernMetric("NET WORTH", "$0.00", new Color(88, 101, 242));

        financePanel.add(inventoryValueLabel);
        financePanel.add(Box.createVerticalStrut(15));
        financePanel.add(totalProfitLabel);
        financePanel.add(Box.createVerticalStrut(15));
        financePanel.add(netWorthLabel);

        sidePanel.add(financePanel);
        updateFinancialDisplay();
    }

    private JPanel createModernMetric(String title, String value, Color valColor) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        titleLabel.setForeground(new Color(185, 187, 190));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Monospaced", Font.BOLD, 15));
        valueLabel.setForeground(valColor);

        container.add(titleLabel, BorderLayout.NORTH);
        container.add(valueLabel, BorderLayout.CENTER);
        return container;
    }

    private JLabel createModernLabel(String title, String value, Color valColor) {
        JLabel label = new JLabel("<html><font color='#B9BBBE'>" + title + ":</font> <font color='" +
                String.format("#%02x%02x%02x", valColor.getRed(), valColor.getGreen(), valColor.getBlue()) +
                "'>" + value + "</font></html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        return label;
    }

    private void navigateCategories(int dir) {
        categoryOffset = Math.max(0, Math.min(categoryOffset + dir, PRESET_CATEGORIES.length - MAX_DISPLAY_CATEGORIES));
        itemOffset = 0;
        refreshInventoryDisplay();
    }

    private void navigateItems(int dir) {
        int listSize = getItemsInCurrentCategory().size();
        itemOffset = Math.max(0, Math.min(itemOffset + dir, Math.max(0, listSize - MAX_DISPLAY_ITEMS)));
        refreshInventoryDisplay();
    }

    private JButton createArrowButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setForeground(new Color(185, 187, 190));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { b.setForeground(new Color(185, 187, 190)); }
        });

        b.addActionListener(al);
        return b;
    }

    private void copyTotalToClipboard() {
        StringSelection ss = new StringSelection(totalLabel.getText());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }

    private Color suggestColor(String label) {
        if (label.contains("Sold") || label.contains("Add")) return new Color(0, 153, 76);
        if (label.contains("Cancelled") || label.contains("Reset") || label.contains("Clear")) return new Color(204, 0, 0);
        return new Color(88, 101, 242);
    }

    private void updateCategoryHeaders() {
        int center = categoryOffset + MAX_DISPLAY_CATEGORIES / 2;
        for (int i = 0; i < MAX_DISPLAY_CATEGORIES; i++) {
            int idx = categoryOffset + i;
            categoryHeaders[i].setText(idx < PRESET_CATEGORIES.length ? PRESET_CATEGORIES[idx] : "");
            categoryHeaders[i].setForeground(idx == center ? Color.GREEN : Color.WHITE);
        }
    }

    private MouseAdapter createCategoryHeaderListener() {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JLabel src = (JLabel) e.getSource();
                for (int i = 0; i < categoryHeaders.length; i++) {
                    if (categoryHeaders[i] == src) {
                        categoryOffset = Math.max(0, Math.min(categoryOffset + i - MAX_DISPLAY_CATEGORIES / 2, PRESET_CATEGORIES.length - MAX_DISPLAY_CATEGORIES));
                        itemOffset = 0;
                        refreshInventoryDisplay();
                        break;
                    }
                }
            }
        };
    }

    private void showCategoryDialog() {
        String sel = (String) JOptionPane.showInputDialog(frame, "Category:", "Select", JOptionPane.PLAIN_MESSAGE, null, PRESET_CATEGORIES, PRESET_CATEGORIES[0]);
        if (sel != null) {
            for (int i = 0; i < PRESET_CATEGORIES.length; i++) {
                if (PRESET_CATEGORIES[i].equals(sel)) {
                    categoryOffset = Math.max(0, Math.min(i - MAX_DISPLAY_CATEGORIES / 2, PRESET_CATEGORIES.length - MAX_DISPLAY_CATEGORIES));
                    itemOffset = 0;
                    refreshInventoryDisplay();
                    break;
                }
            }
        }
    }

    public ClothingInventory getInventory() { return inventory; }

    public static void main(String[] args) {
        try {
            ClothingInventory inventory = new ClothingInventory();

            inventory.addItem(new ClothingItem("Shirt", "Chrome Hearts", "CH Plus Logo T-Shirt", "Black", "M", 9, "Oversized black cotton T-shirt with silver CH Plus logo", 350.00, 1, 200.00));
            inventory.addItem(new ClothingItem("Shirt", "Enfants Riches Déprimés", "Destroyed Band Logo Tee", "White", "L", 7, "Heavyweight cotton with screen-printed logo and intentional distress", 280.00, 1, 160.00));
            inventory.addItem(new ClothingItem("Shirt", "Undercover", "Scab Graphic T-Shirt", "White", "M", 8, "Distressed 'Scab' print with raw hem detailing", 320.00, 1, 180.00));
            inventory.addItem(new ClothingItem("Shirt", "Number (N)ine", "Destroyed Flannel Shirt", "Red/Black", "L", 8, "Vintage-inspired flannel with ripped details", 600.00, 1, 350.00));
            inventory.addItem(new ClothingItem("Shirt", "Raf Simons", "RS Logo Long Sleeve", "Navy", "M", 9, "Oversized fit with contrast logo taping", 450.00, 1, 250.00));

            inventory.addItem(new ClothingItem("Pants", "Dior Homme", "Slim-Fit Biker Jeans", "Black", "32x32", 9, "Waxed denim with leather knee panels", 1200.00, 1, 700.00));
            inventory.addItem(new ClothingItem("Pants", "Helmut Lang", "Bondage Pants", "Black", "34x30", 8, "Leather pants with strap detailing", 780.00, 1, 400.00));
            inventory.addItem(new ClothingItem("Pants", "Yohji Yamamoto", "Wide-Leg Trousers", "Charcoal", "34x34", 9, "Wool blend trousers with dramatic drape", 850.00, 1, 500.00));
            inventory.addItem(new ClothingItem("Pants", "Junya Watanabe", "Patchwork Denim", "Multi", "32x34", 7, "Distressed denim with patchwork and embroidery", 670.00, 1, 390.00));
            inventory.addItem(new ClothingItem("Pants", "Issey Miyake", "Pleated Trousers", "Beige", "S", 9, "Tech fabric with signature pleats", 420.00, 1, 220.00));

            inventory.addItem(new ClothingItem("Shoes", "Rick Owens", "DRKSHDW Ramones", "Black", "10", 9, "High-top sneakers with crepe sole", 550.00, 1, 300.00));
            inventory.addItem(new ClothingItem("Shoes", "Maison Margiela", "Replica Tabi Boots", "White", "9", 8, "Split-toe ankle boots in white leather", 890.00, 1, 500.00));
            inventory.addItem(new ClothingItem("Shoes", "Visvim", "FBT Shaman Folk", "Brown", "10", 9, "Moccasin-style shoes with elk leather", 950.00, 1, 600.00));
            inventory.addItem(new ClothingItem("Shoes", "Balenciaga", "Speed Trainer", "Black", "11", 9, "Stretch-knit sneakers with elastic cuff", 750.00, 1, 400.00));
            inventory.addItem(new ClothingItem("Shoes", "Undercover", "Grace Lace-Up Boots", "White", "10", 8, "Chunky sole boots with lace-up closure", 720.00, 1, 420.00));

            inventory.addItem(new ClothingItem("Jacket", "Enfants Riches Déprimés", "Distressed Denim Jacket", "Blue", "L", 8, "Acid wash denim with heavy distressing", 850.00, 1, 500.00));
            inventory.addItem(new ClothingItem("Jacket", "Raf Simons", "Consumed Parka", "Olive", "M", 9, "Longline parka with multiple pockets", 2200.00, 1, 1300.00));
            inventory.addItem(new ClothingItem("Jacket", "Yohji Yamamoto", "Asymmetric Drape Blazer", "Black", "L", 8, "Wool blazer with peak lapels", 1500.00, 1, 900.00));
            inventory.addItem(new ClothingItem("Jacket", "Sacai", "Double-Layered Denim", "Blue/Black", "M", 8, "Deconstructed two-layer jacket", 890.00, 1, 550.00));
            inventory.addItem(new ClothingItem("Jacket", "Number (N)ine", "A Closing Echo Leather", "Black", "M", 9, "Lambskin jacket with distressed finish", 2400.00, 1, 1500.00));

            inventory.addItem(new ClothingItem("Bag", "Prada", "Nylon Belt Bag", "Black", "One Size", 9, "Re-edition 2000 nylon with logo plaque", 950.00, 1, 500.00));
            inventory.addItem(new ClothingItem("Accessories", "Vivienne Westwood", "Orb Choker", "Gold", "One Size", 10, "Gold-plated choker with orb pendant", 320.00, 1, 180.00));
            inventory.addItem(new ClothingItem("Accessories", "Alexander McQueen", "Skull Scarf", "Black/Silver", "One Size", 9, "Silk twill skull print scarf", 250.00, 1, 140.00));
            inventory.addItem(new ClothingItem("Accessories", "Off-White", "Arrow Belt", "Yellow/Black", "120cm", 9, "Industrial yellow belt with metal details", 200.00, 1, 100.00));

            inventory.addItem(new ClothingItem("Sweater", "Comme des Garçons", "PLAY Striped", "Navy/White", "M", 9, "Striped sweater with heart logo", 450.00, 1, 260.00));
            inventory.addItem(new ClothingItem("Sweater", "Bape", "Shark Hoodie", "Camo", "L", 7, "Camouflage hoodie with shark face", 380.00, 1, 220.00));
            inventory.addItem(new ClothingItem("Sweater", "Vetements", "Metal Logo Hoodie", "Gray", "XL", 8, "Oversized with metal lettering", 650.00, 1, 390.00));

            inventory.addItem(new ClothingItem("Jewelry", "Chrome Hearts", "Cemetery Cross Ring", "Silver", "8", 10, "Sterling silver cross motif ring", 1500.00, 1, 900.00));
            inventory.addItem(new ClothingItem("Jewelry", "Chrome Hearts", "Dagger Pendant", "Silver", "One Size", 9, "925 Silver dagger necklace", 1200.00, 1, 700.00));

            inventory.addItem(new ClothingItem("Jacket", "Kapital", "Century Denim", "Indigo", "L", 8, "Sashiko-stitched denim jacket", 680.00, 1, 400.00));
            inventory.addItem(new ClothingItem("Shoes", "Saint Laurent", "Wyatt Boots", "Black", "9.5", 8, "Chelsea boots with harness detailing", 990.00, 1, 600.00));
            inventory.addItem(new ClothingItem("Jacket", "Comme des Garçons", "Homme Plus Blazer", "Navy", "M", 9, "Deconstructed asymmetric blazer", 1200.00, 1, 720.00));

            new ClothingInventoryGUI(inventory);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Critical error: " + e.getMessage());
        }
    }}

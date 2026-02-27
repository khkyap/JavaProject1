import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.Timer;
import java.awt.event.ActionListener;

public class ClothingInventoryGUI {
    private ClothingInventory inventory;
    private FinancialData financialData = new FinancialData();
    private Stack<InventoryAction> undoStack = new Stack<>();
    private Map<ClothingItem, Timer> pendingSoldTimers = new HashMap<>();
    private JPanel soldArchiveListPanel;
    private JLabel netWorthLabel, inventoryValueLabel;
    private JFrame frame;
    private JPanel panel;
    private JTabbedPane tabbedPane;
    private String currentCategory = PRESET_CATEGORIES[0];
    private int itemOffset = 0;
    private Timer currentAnimationTimer;
    private String currentSortMethod = "Newest First";
    private static final int MAX_DISPLAY_ITEMS = 10;
    private static final String[] PRESET_CATEGORIES = {
            "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket", "Sweater", "Scarf", "Jewelry", "Accessories", "Outerwear", "Leather", "Hoodie"
    };
    private JLabel totalProfitLabel;
    private Map<JCheckBox, ClothingItem> checkBoxItemMap;
    private JButton confirmButton;

    public ClothingInventoryGUI(ClothingInventory inventory) {
            UIManager.put("OptionPane.background", new Color(54, 57, 63));
            UIManager.put("Panel.background", new Color(54, 57, 63));
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            UIManager.put("TextField.background", new Color(40, 43, 48));
            UIManager.put("TextField.foreground", Color.WHITE);
            UIManager.put("TextField.caretForeground", Color.WHITE);
            UIManager.put("ComboBox.background", new Color(40, 43, 48));
            UIManager.put("ComboBox.foreground", Color.WHITE);
            UIManager.put("Button.background", new Color(88, 101, 242));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Label.foreground", Color.WHITE);

            this.inventory = inventory;
            this.checkBoxItemMap = new HashMap<>();
            createGUI();
        }

    private void createGUI() {
        frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1250, 750);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(54, 57, 63));

        JPanel categoryRail = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        categoryRail.setBackground(new Color(40, 43, 48));
        categoryRail.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(32, 34, 37)));

        for (String cat : PRESET_CATEGORIES) {
            JButton catBtn = new JButton(cat.toUpperCase());
            catBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            catBtn.setForeground(new Color(185, 187, 190));
            catBtn.setContentAreaFilled(false);
            catBtn.setBorderPainted(false);
            catBtn.setFocusPainted(false);
            catBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            catBtn.addActionListener(e -> {
                for (Component c : categoryRail.getComponents()) if(c instanceof JButton) c.setForeground(new Color(185, 187, 190));
                catBtn.setForeground(new Color(88, 101, 242));
                this.currentCategory = cat;
                this.itemOffset = 0;
                refreshInventoryDisplay();
            });
            categoryRail.add(catBtn);
        }

        // 2. Central Lists
        panel = new JPanel(new GridLayout(MAX_DISPLAY_ITEMS, 1, 0, 5));
        panel.setBackground(new Color(54, 57, 63));

        JPanel navigationPanel = new JPanel(new BorderLayout());
        navigationPanel.setBackground(new Color(54, 57, 63));
        navigationPanel.add(createSlimNavButton("▲", e -> navigateItems(-1)), BorderLayout.NORTH);
        navigationPanel.add(new JScrollPane(panel), BorderLayout.CENTER);
        navigationPanel.add(createSlimNavButton("▼", e -> navigateItems(1)), BorderLayout.SOUTH);

        // TAB SETUP
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(40, 43, 48)); // Darker tab header background
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Create Dark Panels for the Tabs
        JPanel incomingPanel = new JPanel();
        incomingPanel.setBackground(new Color(54, 57, 63)); // Matches main background

        JPanel soldPanel = new JPanel();
        soldPanel.setBackground(new Color(54, 57, 63)); // Matches main background

        // Add listener to refresh logic
        tabbedPane.addChangeListener(e -> {
            itemOffset = 0;
            refreshInventoryDisplay();
        });


        tabbedPane.addTab("ACTIVE INVENTORY", navigationPanel);
        tabbedPane.addTab("INCOMING", new JScrollPane(incomingPanel) {
            { getViewport().setBackground(new Color(54, 57, 63)); setBorder(null); }
        });
        tabbedPane.addTab("SOLD ARCHIVE", new JScrollPane(soldPanel) {
            { getViewport().setBackground(new Color(54, 57, 63)); setBorder(null); }
        });

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(47, 49, 54));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        sidePanel.setPreferredSize(new Dimension(320, 0));

        createFinancialPanel(sidePanel);
        sidePanel.add(Box.createVerticalStrut(20));

        // SEARCH BAR (New Feature)
        JLabel searchLabel = new JLabel("SEARCH INVENTORY");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        searchLabel.setForeground(new Color(185, 187, 190));
        searchLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(300, 35));
        searchField.setBackground(new Color(40, 43, 48));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(32, 34, 37)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        // Real-time search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterBySearch(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { filterBySearch(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { filterBySearch(searchField.getText()); }
        });

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        searchPanel.setBackground(new Color(47, 49, 54));
        searchPanel.add(searchLabel);
        searchPanel.add(Box.createVerticalStrut(5));
        searchPanel.add(searchField);
        sidePanel.add(searchPanel);
        sidePanel.add(Box.createVerticalStrut(15));
        JLabel sortLabel = new JLabel("SORT INVENTORY");
        sortLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sortLabel.setForeground(new Color(185, 187, 190));
        sortLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] sortOptions = {"Newest First", "Oldest First", "Price: High to Low", "Price: Low to High"};
        JComboBox<String> sortComboBox = new JComboBox<>(sortOptions);
        sortComboBox.setMaximumSize(new Dimension(300, 35));
        sortComboBox.setBackground(new Color(40, 43, 48));
        sortComboBox.setForeground(Color.WHITE);
        sortComboBox.setFocusable(false);
        sortComboBox.addActionListener(e -> {
            currentSortMethod = (String) sortComboBox.getSelectedItem();
            itemOffset = 0;
            refreshInventoryDisplay();
        });

        JPanel sortPanel = new JPanel();
        sortPanel.setLayout(new BoxLayout(sortPanel, BoxLayout.Y_AXIS));
        sortPanel.setBackground(new Color(47, 49, 54));
        sortPanel.add(sortLabel);
        sortPanel.add(Box.createVerticalStrut(5));
        sortPanel.add(sortComboBox);
        sidePanel.add(sortPanel);
        sidePanel.add(Box.createVerticalStrut(15));
        // REFINED BUTTON GRID
        JPanel buttonGridPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        buttonGridPanel.setBackground(new Color(47, 49, 54));
        buttonGridPanel.setMaximumSize(new Dimension(300, 400));

        String[] buttonLabels = {
                "Mark Sold",       "Mark Delivered",
                "Stats Overview",  "Duplicate Item",
                "Master View",     "Export CSV",
                "Undo Last Action","New Trade"
        };

        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.setBackground(suggestColor(label));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> handleSideButtonAction(label));
            buttonGridPanel.add(btn);
        }
        sidePanel.add(buttonGridPanel);

        sidePanel.add(Box.createVerticalStrut(20));
        JButton addItemButton = new JButton("CREATE NEW LISTING");
        addItemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addItemButton.setMaximumSize(new Dimension(300, 45));
        addItemButton.setBackground(new Color(88, 101, 242));
        addItemButton.setForeground(Color.WHITE);
        addItemButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addItemButton.setFocusPainted(false);
        addItemButton.addActionListener(e -> addItemDialog(null));
        sidePanel.add(addItemButton);

        sidePanel.add(Box.createVerticalGlue());
        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(new Color(47, 49, 54));
        confirmButton = new JButton("CONFIRM ACTION");
        confirmButton.setVisible(false);
        confirmButton.setBackground(new Color(0, 102, 51));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        confirmButton.setPreferredSize(new Dimension(280, 45));
        confirmButton.setFocusPainted(false);
        confirmButton.addActionListener(e -> confirmAction());
        actionPanel.add(confirmButton);
        sidePanel.add(actionPanel);

        frame.add(categoryRail, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);

        if (currentCategory == null && PRESET_CATEGORIES.length > 0) currentCategory = PRESET_CATEGORIES[0];
        refreshInventoryDisplay();
        frame.setVisible(true);
    }

    private void filterByCategory(String category) {
        this.currentCategory = category;
        this.itemOffset = 0;
        refreshInventoryDisplay();
    }

    private List<ClothingItem> getItemsInCurrentCategory() {
        return inventory.getInventory().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(currentCategory))
                .collect(Collectors.toList());
    }


    private void refreshInventoryDisplay() {
        if (currentAnimationTimer != null && currentAnimationTimer.isRunning()) {
            currentAnimationTimer.stop();
        }
        panel.removeAll();
        panel.revalidate();
        panel.repaint();

        List<ClothingItem> catItems = getItemsInCurrentCategory();

        // 1. FILTER FIRST
        int tabIndex = tabbedPane.getSelectedIndex();
        List<ClothingItem> filtered = catItems.stream().filter(item -> {
            if (tabIndex == 0) return !item.isSold() && !item.isIncoming() && !item.isCancelled(); // Active
            if (tabIndex == 1) return item.isIncoming() && !item.isCancelled(); // Incoming
            if (tabIndex == 2) return item.isSold(); // Sold
            return false;
        }).collect(Collectors.toList());

        // 2. SORT SECOND
        filtered.sort((a, b) -> {
            switch (currentSortMethod) {
                case "Oldest First":
                    if(a.getDateAdded() == null || b.getDateAdded() == null) return 0;
                    return a.getDateAdded().compareTo(b.getDateAdded());
                case "Price: High to Low":
                    return Double.compare(b.getPrice(), a.getPrice());
                case "Price: Low to High":
                    return Double.compare(a.getPrice(), b.getPrice());
                case "Newest First":
                default:
                    if(a.getDateAdded() == null || b.getDateAdded() == null) return 0;
                    return b.getDateAdded().compareTo(a.getDateAdded());
            }
        });

        currentAnimationTimer = new Timer(15, new ActionListener() {
            int count = itemOffset;
            int displayIndex = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (displayIndex < MAX_DISPLAY_ITEMS) {
                    ClothingItem item = (count < filtered.size()) ? filtered.get(count) : null;
                    JPanel itemPanel = createSleekItemPanel(item);
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
        currentAnimationTimer.start();
    }

    private JPanel createSleekItemPanel(ClothingItem item) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(new Color(47, 49, 54));
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(32, 34, 37), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        if (item != null) {
            boolean isStale = (!item.isSold() && !item.isCancelled() && !item.isIncoming() && item.getDaysInInventory() > 90);

            // Apply warning border if stale
            if (isStale) {
                itemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 165, 0), 1),
                        BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }

            JPanel identityPanel = new JPanel(new GridLayout(2, 1));
            identityPanel.setOpaque(false);

            JLabel brandLabel = new JLabel();
            brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (isStale) {
                brandLabel.setText(item.getBrand().toUpperCase() + " ⚠ STALE (" + item.getDaysInInventory() + "d)");
                brandLabel.setForeground(new Color(255, 165, 0));
            } else {
                brandLabel.setText(item.getBrand().toUpperCase());
                brandLabel.setForeground(Color.WHITE);
            }

            JLabel nameLabel = new JLabel(item.getName());
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            nameLabel.setForeground(new Color(185, 187, 190));
            identityPanel.add(brandLabel);
            identityPanel.add(nameLabel);

            JPanel financeMatrix = new JPanel(new GridLayout(1, 4, 20, 0));
            financeMatrix.setOpaque(false);

            JLabel estPriceLabel = new JLabel();
            estPriceLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (isStale) {
                estPriceLabel.setText(String.format("<html><strike>$%.0f</strike> <font color='#00FF7F'>$%.0f</font></html>",
                        item.getPrice(), item.getRecommendedDiscountPrice()));
            } else {
                estPriceLabel.setText(String.format("$%.0f", item.getPrice()));
                estPriceLabel.setForeground(Color.WHITE);
            }

            estPriceLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { handleInlinePriceEdit(item, estPriceLabel); }
            });

            financeMatrix.add(createStatSubPanel("PURCHASE", String.format("$%.0f", item.getPurchasePrice()), new Color(114, 137, 218)));

            JPanel estContainer = new JPanel(new BorderLayout());
            estContainer.setOpaque(false);
            estContainer.add(new JLabel("ESTIMATE", SwingConstants.RIGHT), BorderLayout.NORTH);
            estContainer.add(estPriceLabel, BorderLayout.CENTER);
            financeMatrix.add(estContainer);

            String soldVal = item.isSold() ? String.format("$%.0f", item.getPrice()) : "—";
            financeMatrix.add(createStatSubPanel("SOLD", soldVal, new Color(255, 165, 0)));

            String profitVal = "";
            if (item.isSold()) {
                double profitAmt = item.getPrice() - item.getPurchasePrice();
                profitVal = String.format("+$%.0f", profitAmt);
            }
            financeMatrix.add(createStatSubPanel("PROFIT", profitVal, new Color(0, 255, 127)));

            // STATUS BUTTONS
            JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            btnWrapper.setOpaque(false);

            if (pendingSoldTimers.containsKey(item)) {
                JButton undoBtn = new JButton("UNDO SALE");
                undoBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
                undoBtn.setBackground(new Color(204, 0, 0));
                undoBtn.setForeground(Color.WHITE);
                undoBtn.addActionListener(e -> cancelSoldTimer(item));
                btnWrapper.add(undoBtn);
                itemPanel.add(btnWrapper, BorderLayout.SOUTH);
            }
            else if (item.isIncoming()) {
                JButton deliverBtn = new JButton("DELIVERED");
                deliverBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
                deliverBtn.setBackground(new Color(88, 101, 242));
                deliverBtn.setForeground(Color.WHITE);
                deliverBtn.addActionListener(e -> {
                    item.setIncoming(false);
                    trackUndoAction(new InventoryAction(InventoryAction.Type.EDIT, item, "Incoming"));
                    refreshInventoryDisplay();
                });
                btnWrapper.add(deliverBtn);
                itemPanel.add(btnWrapper, BorderLayout.SOUTH);
            }

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

    private JButton createSlimNavButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(0, 20));
        b.setBackground(new Color(47, 49, 54));
        b.setForeground(new Color(114, 118, 125));
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
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
        JCheckBox incomingCheck = new JCheckBox("Mark as Incoming (Pre-order)");
        if(existingItem != null) incomingCheck.setSelected(existingItem.isIncoming());

        Object[] message = {
                "Category:", catField,
                "Brand:", brandField,
                "Name:", nameField,
                "Price:", priceField,
                "Stock:", stockField,
                "Purchase Price:", buyPriceField,
                incomingCheck
        };

        int option = JOptionPane.showConfirmDialog(frame, message, "Item Details", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (existingItem == null) {
                    ClothingItem newItem = new ClothingItem(catField.getText(), brandField.getText(), nameField.getText(), "", "", 10, "", Double.parseDouble(priceField.getText()), Integer.parseInt(stockField.getText()), Double.parseDouble(buyPriceField.getText()));
                    newItem.setIncoming(incomingCheck.isSelected());
                    inventory.addItem(newItem);
                    trackUndoAction(new InventoryAction(InventoryAction.Type.ADD, newItem, null));
                } else {
                    existingItem.setCategory(catField.getText());
                    existingItem.setBrand(brandField.getText());
                    existingItem.setName(nameField.getText());
                    existingItem.setPrice(Double.parseDouble(priceField.getText()));
                    existingItem.setStock(Integer.parseInt(stockField.getText()));
                    existingItem.setPurchasePrice(Double.parseDouble(buyPriceField.getText()));
                    existingItem.setIncoming(incomingCheck.isSelected());
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
            case "Mark Delivered": showCheckboxesForAction("Delivered"); break;
            case "Stats Overview": showStatsDialog(); break;
            case "Master View": openMasterInventoryView(); break;
            case "Export CSV": exportToCSV(); break;
            case "New Trade": new TradeWindow(frame, this).setVisible(true); break;
            case "Reset Statuses": resetAllItemStatuses(); break;
            case "Duplicate Item": showCheckboxesForAction("Duplicate"); break;
            case "Undo Last Action": performUndo(); break;
        }
    }

    private void performUndo() {
        if (undoStack.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No recent actions to undo.");
            return;
        }

        InventoryAction action = undoStack.pop();

        switch(action.type) {
            case ADD:
                inventory.getInventory().remove(action.item);
                break;
            case EDIT:
                if (action.previousValue instanceof Double) {
                    action.item.setPrice((Double) action.previousValue); // Revert Price
                } else if (action.previousValue.equals("Incoming")) {
                    action.item.setIncoming(true); // Revert Delivery
                }
                break;
            case MARK_SOLD:
                action.item.setSold(false);
                break;
            case DELETE:
                inventory.addItem(action.item);
                break;
        }
        refreshInventoryDisplay();
        updateFinancialDisplay();
    }

    private void filterBySearch(String query) {
        if (currentAnimationTimer != null && currentAnimationTimer.isRunning()) {
            currentAnimationTimer.stop();
        }

        panel.removeAll();

        List<ClothingItem> results = inventory.getInventory().stream()
                .filter(i -> i.getName().toLowerCase().contains(query.toLowerCase()) ||
                        i.getBrand().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        if(results.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No items found matching '" + query + "'");
            refreshInventoryDisplay();
        } else {
            for(ClothingItem item : results) {
                panel.add(createSleekItemPanel(item));
            }
            panel.revalidate();
            panel.repaint();
        }
    }

    private void openMasterInventoryView() {
        JDialog sheetDialog = new JDialog(frame, "Master Inventory View", true);
        sheetDialog.setSize(1000, 600);
        sheetDialog.setLocationRelativeTo(frame);
        sheetDialog.getContentPane().setBackground(new Color(54, 57, 63));

        String[] columns = {"Brand", "Name", "Category", "Size", "Price", "Cost", "Stock", "Status"};
        List<ClothingItem> allItems = inventory.getInventory();
        Object[][] data = new Object[allItems.size()][8];

        for (int i = 0; i < allItems.size(); i++) {
            ClothingItem item = allItems.get(i);
            data[i][0] = item.getBrand();
            data[i][1] = item.getName();
            data[i][2] = item.getCategory();
            data[i][3] = item.getSize();
            data[i][4] = String.format("$%.2f", item.getPrice());
            data[i][5] = String.format("$%.2f", item.getPurchasePrice());
            data[i][6] = item.getStock();
            data[i][7] = item.isSold() ? "SOLD" : (item.isCancelled() ? "CANCELLED" : "ACTIVE");
        }

        JTable table = new JTable(data, columns);
        table.setBackground(new Color(47, 49, 54));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(32, 34, 37));
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);

        table.getTableHeader().setBackground(new Color(32, 34, 37));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(new Color(54, 57, 63));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        sheetDialog.add(scrollPane);
        sheetDialog.setVisible(true);
    }

    private void showStatsDialog() {
        long totalItems = inventory.getInventory().size();
        long soldItems = inventory.getInventory().stream().filter(ClothingItem::isSold).count();

        Map<String, Long> brandCounts = inventory.getInventory().stream()
                .collect(Collectors.groupingBy(ClothingItem::getBrand, Collectors.counting()));

        String topBrand = brandCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String msg = String.format(
                "Total Items Tracked: %d\n" +
                        "Items Sold: %d\n" +
                        "Conversion Rate: %.1f%%\n" +
                        "Top Brand: %s",
                totalItems, soldItems,
                (totalItems > 0 ? ((double)soldItems/totalItems)*100 : 0),
                topBrand
        );
        JOptionPane.showMessageDialog(frame, msg, "Inventory Statistics", JOptionPane.INFORMATION_MESSAGE);
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
                    startSoldBuffer(item);
                }
                else if (action.equals("Delivered")) {
                    item.setIncoming(false);
                }
                else if (action.equals("Duplicate")) {
                    ClothingItem copy = new ClothingItem(item.getCategory(), item.getBrand(), item.getName() + " (Copy)",
                            item.getColor1(), item.getSize(), item.getCondition(),
                            item.getDescription(), item.getPrice(), item.getStock(), item.getPurchasePrice());
                    inventory.addItem(copy);
                }
            }
        }
        confirmButton.setVisible(false);
        refreshInventoryDisplay();
        updateFinancialDisplay();
    }

    private void trackUndoAction(InventoryAction action) {
        if (undoStack.size() >= 5) {
            undoStack.removeElementAt(0);
        }
        undoStack.push(action);
    }

    private void handleInlinePriceEdit(ClothingItem item, JLabel label) {
        String input = JOptionPane.showInputDialog(frame, "Quick Edit Price:", item.getPrice());
        if (input != null) {
            try {
                double newPrice = Double.parseDouble(input);
                trackUndoAction(new InventoryAction(InventoryAction.Type.EDIT, item, item.getPrice()));
                item.setPrice(newPrice);
                refreshInventoryDisplay();
                updateFinancialDisplay();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid Price");
            }
        }
    }

    private void startSoldBuffer(ClothingItem item) {
        Timer timer = new Timer(10000, e -> finalizeSold(item));
        timer.setRepeats(false);
        pendingSoldTimers.put(item, timer);
        timer.start();
        refreshInventoryDisplay();
    }

    private void cancelSoldTimer(ClothingItem item) {
        if (pendingSoldTimers.containsKey(item)) {
            pendingSoldTimers.get(item).stop();
            pendingSoldTimers.remove(item);
            refreshInventoryDisplay();
        }
    }

    private void finalizeSold(ClothingItem item) {
        pendingSoldTimers.remove(item);
        item.setSold(true);
        updateFinancialDisplay();
        refreshInventoryDisplay();
    }

    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("inventory_export.csv"))) {
            writer.println("Date Added,Category,Brand,Name,Purchase Price,Est. Price,Stock,Days Old,Rec. Discount,Status");

            for (ClothingItem item : inventory.getInventory()) {
                String status = "ACTIVE";
                if (item.isSold()) status = "SOLD";
                else if (item.isCancelled()) status = "CANCELLED";
                else if (item.isIncoming()) status = "INCOMING";

                writer.printf("%s,%s,%s,%s,%.2f,%.2f,%d,%d,%.2f,%s%n",
                        item.getDateAdded() != null ? item.getDateAdded().toString() : "N/A",
                        item.getCategory(),
                        item.getBrand(),
                        item.getName(),
                        item.getPurchasePrice(),
                        item.getPrice(),
                        item.getStock(),
                        item.getDaysInInventory(),
                        item.getRecommendedDiscountPrice(),
                        status
                );
            }
            JOptionPane.showMessageDialog(frame, "Inventory successfully exported to CSV.");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error exporting to CSV: " + e.getMessage());
        }
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

        double projectedProfit = inventory.getInventory().stream()
                .filter(item -> !item.isSold() && !item.isCancelled() && !item.isIncoming())
                .mapToDouble(item -> item.getPrice() - item.getPurchasePrice())
                .sum();

        double totalLiquid = financialData.getTotalAssets() + realizedProfit;
        double netWorth = totalLiquid + currentInvValue - financialData.getTotalDebt();

        long soldCount = inventory.getInventory().stream().filter(ClothingItem::isSold).count();
        long totalCount = inventory.getInventory().size();
        double turnoverRate = (totalCount == 0) ? 0.0 : ((double) soldCount / totalCount) * 100.0;

        inventoryValueLabel.setText(String.format("$%.2f (Turnover: %.1f%%)", currentInvValue, turnoverRate));
        totalProfitLabel.setText(String.format("$%.2f", realizedProfit));
        netWorthLabel.setText(String.format("$%.2f (Proj: $%.2f)", netWorth, projectedProfit));
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

        JPanel invPanel = createModernMetric("INVENTORY VALUE", "$0.00", Color.WHITE);
        JPanel profitPanel = createModernMetric("LIFETIME PROFIT", "$0.00", new Color(0, 255, 127));
        JPanel worthPanel = createModernMetric("NET WORTH", "$0.00", new Color(88, 101, 242));

        financePanel.add(invPanel);
        financePanel.add(Box.createVerticalStrut(15));
        financePanel.add(profitPanel);
        financePanel.add(Box.createVerticalStrut(15));
        financePanel.add(worthPanel);

        sidePanel.add(financePanel);
        updateFinancialDisplay();
    }

    private JPanel createModernMetric(String title, String value, Color valColor) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        titleLabel.setForeground(new Color(185, 187, 190));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        valueLabel.setForeground(valColor);

        container.add(titleLabel, BorderLayout.NORTH);
        container.add(valueLabel, BorderLayout.CENTER);

        if (title.contains("INVENTORY")) this.inventoryValueLabel = valueLabel;
        else if (title.contains("PROFIT")) this.totalProfitLabel = valueLabel;
        else if (title.contains("WORTH")) this.netWorthLabel = valueLabel;

        return container;
    }

    private void navigateItems(int dir) {
        int listSize = getItemsInCurrentCategory().size();
        itemOffset = Math.max(0, Math.min(itemOffset + dir, Math.max(0, listSize - MAX_DISPLAY_ITEMS)));
        refreshInventoryDisplay();
    }

    private Color suggestColor(String label) {
        if (label.contains("Sold") || label.contains("Add")) return new Color(0, 153, 76);
        if (label.contains("Cancelled") || label.contains("Reset") || label.contains("Clear")) return new Color(204, 0, 0);
        return new Color(88, 101, 242);
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
    }
    private static class InventoryAction {
        enum Type { ADD, DELETE, MARK_SOLD, EDIT }
        Type type;
        ClothingItem item;
        Object previousValue;

        public InventoryAction(Type type, ClothingItem item, Object previousValue) {
            this.type = type;
            this.item = item;
            this.previousValue = previousValue;
        }
    }
}

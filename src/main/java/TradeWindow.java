import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.List;

class TradeWindow extends JDialog {
    private final ClothingInventoryGUI parentGUI;
    private final ClothingItem[] theirItems = new ClothingItem[5];
    private final ClothingItem[] ourItems = new ClothingItem[5];
    private JLabel theirTotalLabel, ourTotalLabel;
    private JPanel[] theirSlots = new JPanel[5];
    private JPanel[] ourSlots = new JPanel[5];

    public TradeWindow(JFrame frame, ClothingInventoryGUI parent) {
        super(frame, "Trade Manager", true);
        this.parentGUI = parent;
        setSize(1000, 600);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(54, 57, 63));

        JPanel tradeContainer = new JPanel(new GridLayout(2, 1, 10, 10));
        tradeContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tradeContainer.add(createTradeSide("Their Offer", theirItems, theirSlots));
        tradeContainer.add(createTradeSide("Our Offer", ourItems, ourSlots));

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(47, 49, 54));
        JButton calculateButton = new JButton("Calculate Profit");
        JTextArea profitDisplay = new JTextArea(3, 20);
        profitDisplay.setBackground(new Color(47, 49, 54));
        profitDisplay.setForeground(Color.WHITE);
        profitDisplay.setEditable(false);

        calculateButton.addActionListener(e -> {
            double ourTotal = calculateTotal(ourItems);
            double theirTotal = calculateTotal(theirItems);
            double profit = theirTotal - ourTotal;
            double margin = (ourTotal > 0) ? (profit / ourTotal) * 100 : 0;
            profitDisplay.setText(String.format("Dollar Profit: $%.2f\nMargin: %.1f%%\nRatio: %.2f:1", profit, margin, (ourTotal > 0) ? theirTotal/ourTotal : 0));
        });

        controlPanel.add(calculateButton);
        controlPanel.add(profitDisplay);
        add(tradeContainer, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createTradeSide(String title, ClothingItem[] items, JPanel[] slots) {
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBackground(new Color(54, 57, 63));
        JLabel header = new JLabel(title, SwingConstants.CENTER);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 16));
        sidePanel.add(header, BorderLayout.NORTH);

        JPanel slotsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        slotsPanel.setBackground(new Color(54, 57, 63));
        for (int i = 0; i < 5; i++) {
            slots[i] = createTradeSlot(i, items);
            slotsPanel.add(slots[i]);
        }

        JPanel totalPanel = new JPanel();
        totalPanel.setBackground(new Color(47, 49, 54));
        JLabel totalLabel = new JLabel("Total: $0.00");
        totalLabel.setForeground(Color.WHITE);
        totalPanel.add(totalLabel);
        if (title.equals("Their Offer")) theirTotalLabel = totalLabel;
        else ourTotalLabel = totalLabel;

        sidePanel.add(slotsPanel, BorderLayout.CENTER);
        sidePanel.add(totalPanel, BorderLayout.SOUTH);
        return sidePanel;
    }

    private JPanel createTradeSlot(int slotIndex, ClothingItem[] items) {
        JPanel slotPanel = new JPanel(new BorderLayout());
        slotPanel.setBackground(new Color(47, 49, 54));
        slotPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        slotPanel.setPreferredSize(new Dimension(150, 150));
        JButton addButton = new JButton("+");
        addButton.setFont(new Font("Arial", Font.BOLD, 24));
        addButton.setBackground(new Color(88, 101, 242));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> showSlotSelectionDialog(slotIndex, items));
        slotPanel.add(addButton, BorderLayout.CENTER);
        return slotPanel;
    }

    private void showSlotSelectionDialog(int slotIndex, ClothingItem[] items) {
        Object[] options = {"Select Item", "Add Cash"};
        int choice = JOptionPane.showOptionDialog(this, "Add to trade slot:", "Trade Slot Content", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) new ItemSelectionDialog(this, slotIndex, items).setVisible(true);
        else if (choice == 1) handleCashInput(slotIndex, items);
    }

    private void handleCashInput(int slotIndex, ClothingItem[] items) {
        String amount = JOptionPane.showInputDialog(this, "Enter cash amount:");
        try {
            double cash = Double.parseDouble(amount);
            items[slotIndex] = new ClothingItem("Cash", "", "Cash", "", "", 10, "", cash);
            updateTradeDisplay();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid cash amount!");
        }
    }

    private void updateTradeDisplay() {
        updateSideDisplay(theirSlots, theirItems);
        updateSideDisplay(ourSlots, ourItems);
        updateTotals();
    }

    private void updateSideDisplay(JPanel[] slots, ClothingItem[] items) {
        for (int i = 0; i < slots.length; i++) {
            slots[i].removeAll();
            final int index = i;
            if (items[i] != null) {
                JPanel contentPanel = new JPanel(new BorderLayout());
                contentPanel.setBackground(new Color(47, 49, 54));
                JTextArea itemInfo = new JTextArea(items[i].getTradeDisplayText());
                itemInfo.setEditable(false);
                itemInfo.setForeground(Color.WHITE);
                itemInfo.setBackground(new Color(47, 49, 54));
                contentPanel.add(itemInfo, BorderLayout.CENTER);
                JButton removeButton = new JButton("X");
                removeButton.setBackground(new Color(200, 0, 0));
                removeButton.setForeground(Color.WHITE);
                removeButton.addActionListener(e -> { items[index] = null; updateTradeDisplay(); });
                contentPanel.add(removeButton, BorderLayout.NORTH);
                slots[i].add(contentPanel);
            } else {
                JButton addButton = new JButton("+");
                addButton.setBackground(new Color(88, 101, 242));
                addButton.setForeground(Color.WHITE);
                addButton.addActionListener(e -> showSlotSelectionDialog(index, items));
                slots[i].add(addButton);
            }
            slots[i].revalidate();
            slots[i].repaint();
        }
    }

    private void updateTotals() {
        theirTotalLabel.setText("Their Total: $" + String.format("%.2f", calculateTotal(theirItems)));
        ourTotalLabel.setText("Our Total: $" + String.format("%.2f", calculateTotal(ourItems)));
    }

    private double calculateTotal(ClothingItem[] items) {
        return Arrays.stream(items).filter(Objects::nonNull).mapToDouble(ClothingItem::getPrice).sum();
    }

    private class ItemSelectionDialog extends JDialog {
        private final JTextField searchField = new JTextField(20);
        private final JPanel itemsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        private final int targetSlot;
        private final ClothingItem[] targetItems;

        public ItemSelectionDialog(JDialog parent, int slotIndex, ClothingItem[] items) {
            super(parent, "Select Item", true);
            this.targetSlot = slotIndex;
            this.targetItems = items;
            setSize(600, 400);
            setLayout(new BorderLayout());
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filterItems(); }
                public void removeUpdate(DocumentEvent e) { filterItems(); }
                public void changedUpdate(DocumentEvent e) { filterItems(); }
            });
            add(searchField, BorderLayout.NORTH);
            add(new JScrollPane(itemsPanel), BorderLayout.CENTER);
            loadAllItems();
            setLocationRelativeTo(parent);
        }

        private void loadAllItems() {
            itemsPanel.removeAll();
            List<ClothingItem> inventory = parentGUI.getInventory().getInventory();
            for (ClothingItem item : inventory) {
                if (item.isActive() && !item.isCash() && !isItemInTrade(item)) {
                    addItemToPanel(item);
                }
            }
            itemsPanel.revalidate();
            itemsPanel.repaint();
        }

        private boolean isItemInTrade(ClothingItem target) {
            return Arrays.stream(TradeWindow.this.theirItems).anyMatch(item -> item == target) || Arrays.stream(TradeWindow.this.ourItems).anyMatch(item -> item == target);
        }

        private void addItemToPanel(ClothingItem item) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.add(new JLabel("<html>" + item.getName() + "<br/>$" + item.getPrice() + "</html>"), BorderLayout.CENTER);
            JButton selectButton = new JButton("Select");
            selectButton.addActionListener(e -> { targetItems[targetSlot] = item; updateTradeDisplay(); dispose(); });
            itemPanel.add(selectButton, BorderLayout.EAST);
            itemsPanel.add(itemPanel);
        }

        private void filterItems() {
            String query = searchField.getText().toLowerCase();
            for (Component comp : itemsPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    boolean match = false;
                    for (Component child : panel.getComponents()) {
                        if (child instanceof JLabel) match = ((JLabel) child).getText().toLowerCase().contains(query);
                    }
                    panel.setVisible(match);
                }
            }
        }
    }
}
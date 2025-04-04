import java.util.Scanner;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClothingInventory {
    private ClothingItem[][] inventory;
    private int rows;
    private int cols;
    private Scanner scanner;

    // Predefined categories
    private static final String[] PRESET_CATEGORIES = {
            "", "",
            "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket", "Sweater", "Socks", "Shorts", "Scarf", "Jewelry", "Accessories", "Outerwear", "Leather",
            "", ""
    };

    public ClothingInventory(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.inventory = new ClothingItem[rows][cols];
        this.scanner = new Scanner(System.in);
    }

    // Method to manually add an item at a specific position
    public void manualAddItem(String category, String brand, String name, String color1, String size, int condition, String description, double price) {
        System.out.println("Input row, then input column");
        int row = scanner.nextInt();
        int col = scanner.nextInt();

        // Normalize and validate the category
        category = normalizeCategoryName(category);
        if (!isValidCategory(category)) {
            System.out.println("Invalid category: " + category);
            return;
        }

        // Add the item to the inventory
        inventory[row][col] = new ClothingItem(category, brand, name, color1, size, condition, description, price);
        System.out.println("Item added successfully!");
    }

    // Method to automatically add an item to the appropriate category row
    public void autoAddItem(String category, String brand, String name, String color1, String size, int condition, String description, double price, int stock, double purchasePrice) {
        // Normalize and validate the category
        category = normalizeCategoryName(category);
        if (!isValidCategory(category)) {
            System.out.println("Invalid category: " + category);
            return;
        }

        // Find the row for the category
        int row = getCategoryRow(category);
        if (row == -1) {
            System.out.println("No space available to add the item.");
            return;
        }

        // Find the first empty column in the row
        for (int col = 0; col < cols; col++) {
            if (inventory[row][col] == null) {
                inventory[row][col] = new ClothingItem(category, brand, name, color1, size, condition, description, price, stock, purchasePrice);
                System.out.println("Item added successfully in row " + row + ", column " + col);
                return;
            }
        }

        System.out.println("No space available in category row: " + category);
    }

    // Helper method to normalize category names (trim and capitalize)
    private String normalizeCategoryName(String category) {
        if (category == null || category.isEmpty()) {
            return category;
        }

        // Capitalize the first letter of each word
        StringBuilder result = new StringBuilder();
        String[] words = category.trim().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1).toLowerCase());
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    // Helper method to validate if a category exists in PRESET_CATEGORIES
    private boolean isValidCategory(String category) {
        for (String presetCategory : PRESET_CATEGORIES) {
            if (presetCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to get the row index for a category
    private int getCategoryRow(String category) {
        for (int i = 0; i < PRESET_CATEGORIES.length; i++) {
            if (PRESET_CATEGORIES[i].equalsIgnoreCase(category)) {
                return i - 2; // Adjust for the offset in PRESET_CATEGORIES
            }
        }
        return -1; // Category not found
    }

    // Existing methods (unchanged)
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public ClothingItem getItem(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            System.out.println("Invalid position.");
            return null;
        }
        return inventory[row][col];
    }

    public void updateItem(int row, int col, String category, String brand, String name, String color1, String size, int condition, String description, double price, int stock) {
        if (row >= 0 && row < rows && col >= 0 && col < cols && inventory[row][col] != null) {
            ClothingItem item = inventory[row][col];
            item.setCategory(category);
            item.setBrand(brand);
            item.setName(name);
            item.setColor1(color1);
            item.setSize(size);
            item.setCondition(condition);
            item.setDescription(description);
            item.setPrice(price);
            item.setStock(stock);
            System.out.println("Item updated successfully at row " + row + ", column " + col);
        } else {
            System.out.println("Invalid update location or no item exists at this position.");
        }
    }

    public void addClothingItem(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            System.out.println("Invalid position.");
            return;
        }

        ClothingItem newItem = new ClothingItem(scanner);
        newItem.setCategory(normalizeCategoryName(newItem.getCategory()));

        if (!isValidCategory(newItem.getCategory())) {
            System.out.println("Invalid category: " + newItem.getCategory());
            return;
        }

        inventory[row][col] = newItem;
        System.out.println("Item added successfully!");
    }

    public void displayInventoryGUI() {
        JFrame frame = new JFrame("Clothing Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel(new GridLayout(rows, cols));

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JLabel label = new JLabel();
                label.setHorizontalAlignment(JLabel.CENTER);
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                if (inventory[i][j] != null) {
                    label.setText(inventory[i][j].getName());
                } else {
                    label.setText("Empty");
                }

                panel.add(label);
            }
        }

        frame.add(panel);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        ClothingInventory inventory = new ClothingInventory(2, 2);

        inventory.addClothingItem(0, 0);
        inventory.manualAddItem("Hat", "Chrome Hearts", "Trucker Cap", "Camo Green", "OS", 9, "no description", 450);
        inventory.displayInventoryGUI();
    }
}
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class ClothingInventory {
    private List<ClothingItem> inventory;
    private Scanner scanner;

    private static final String[] PRESET_CATEGORIES = {
            "Hat", "Shirt", "Pants", "Shoes", "Bag", "Jacket", "Sweater", "Socks",
            "Shorts", "Scarf", "Jewelry", "Accessories", "Outerwear", "Leather"
    };

    public ClothingInventory() {
        this.inventory = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    public void addItem(ClothingItem item) {
        String category = normalizeCategoryName(item.getCategory());
        if (!isValidCategory(category)) {
            System.out.println("Invalid category: " + category);
            return;
        }
        item.setCategory(category);
        inventory.add(item);
        System.out.println("Item added successfully!");
    }

    public void sortByName() {
        inventory.sort(Comparator.comparing(ClothingItem::getName, String.CASE_INSENSITIVE_ORDER));
        System.out.println("Inventory sorted by name.");
    }

    public int findItemByName(String name) {
        sortByName();
        ClothingItem key = new ClothingItem();
        key.setName(name);

        return Collections.binarySearch(inventory, key,
                Comparator.comparing(ClothingItem::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private String normalizeCategoryName(String category) {
        if (category == null || category.isEmpty()) return category;

        StringBuilder result = new StringBuilder();
        String[] words = category.trim().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    private boolean isValidCategory(String category) {
        for (String presetCategory : PRESET_CATEGORIES) {
            if (presetCategory.equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }

    public List<ClothingItem> getInventory() {
        return inventory;
    }
}
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.time.LocalDate;
public class ClothingItem implements Comparable<ClothingItem> {
    private double purchasePrice;
    private double profit;
    private String category;
    private String brand;
    private String name;
    private String color1;
    private String size;
    private int stock;
    private int condition;
    private String description;
    private double price;
    private boolean sold = false;
    private boolean cancelled = false;
    private boolean incoming = false;
    private LocalDate dateAdded;

    public boolean isIncoming() { return incoming; }
    public void setIncoming(boolean incoming) { this.incoming = incoming; }

    public ClothingItem() {}

    public ClothingItem(String category, String brand, String name, String color1, String size,
                        int condition, String description, double price, int stock, double purchasePrice) {
        this.category = capitalizeWords(category);
        this.brand = capitalizeWords(brand);
        this.name = capitalizeWords(name);
        this.color1 = capitalizeWords(color1);
        this.size = size;
        this.condition = condition;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.purchasePrice = purchasePrice;
        this.profit = 0;
        this.dateAdded = LocalDate.now();
    }

    public ClothingItem(String category, String brand, String name, String color1, String size,
                        int condition, String description, double price) {
        this(category, brand, name, color1, size, condition, description, price, 1, 0.0);
    }

    public ClothingItem(Scanner scanner) {
        System.out.println("Enter details for the new ClothingItem:");
        System.out.print("Enter category name: ");
        this.category = capitalizeWords(scanner.nextLine());
        System.out.print("Enter brand name: ");
        this.brand = capitalizeWords(scanner.nextLine());
        System.out.print("Enter item name: ");
        this.name = capitalizeWords(scanner.nextLine());
        System.out.print("Enter item color: ");
        this.color1 = scanner.nextLine();
        System.out.print("Enter item size: ");
        this.size = scanner.nextLine();
        System.out.print("Enter item condition: ");
        this.condition = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Enter item description (optional): ");
        this.description = scanner.nextLine();
        System.out.print("Enter item price: ");
        this.price = scanner.nextDouble();
        System.out.print("Enter stock: ");
        this.stock = scanner.nextInt();
        System.out.print("Enter purchase price: ");
        this.purchasePrice = scanner.nextDouble();
        this.dateAdded = LocalDate.now();
        scanner.nextLine();
    }

    @Override
    public int compareTo(ClothingItem other) {
        return this.name.toLowerCase().compareTo(other.name.toLowerCase());
    }

    public boolean isActive() {
        return !isSold() && !isCancelled();
    }

    public boolean isCash() {
        return "Cash".equalsIgnoreCase(this.category);
    }

    public String getTradeDisplayText() {
        if(isCash()) {
            return String.format("Cash\n$%.2f", price);
        }
        return String.format("%s\n%s\nSize: %s\n$%.2f",
                category, name, size, price);
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder result = new StringBuilder();
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getDaysInInventory() {
        if (this.dateAdded == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(this.dateAdded, LocalDate.now());
    }

    public double getRecommendedDiscountPrice() {
        long daysOld = getDaysInInventory();
        if (daysOld > 90) {
            return this.getPrice() * 0.80;
        } else if (daysOld > 180) {
            return this.getPrice() * 0.60;
        }
        return this.getPrice();
    }

    public boolean isSold() { return sold; }
    public void setSold(boolean sold) { this.sold = sold; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = capitalizeWords(category); }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = capitalizeWords(brand); }
    public String getName() { return name; }
    public void setName(String name) { this.name = capitalizeWords(name); }
    public String getColor1() { return color1; }
    public void setColor1(String color1) { this.color1 = color1; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public int getCondition() { return condition; }
    public void setCondition(int condition) { this.condition = condition; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public void adjustStock(int delta) { this.stock += delta; }
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
}
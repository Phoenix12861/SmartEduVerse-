package modules.management.library;

public class Book {
    public int id;
    public String title, author, category;
    public int quantity, availableCount;
    public double price;

    public Book(int id, String title, String author, String category, int quantity, int availableCount, double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.quantity = quantity;
        this.availableCount = availableCount;
        this.price = price;
    }
}

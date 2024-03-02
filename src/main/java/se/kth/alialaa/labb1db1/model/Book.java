package se.kth.alialaa.labb1db1.model;

import java.sql.Array;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representation of a book.
 * 
 * @author anderslm@kth.se
 */
public class Book {
    
    private int bookId;
    private String isbn; // should check format
    private String title;
    private Date published;
    private String storyLine = "";
    private String genre;
    private List<Author> authors;
    private int rating;
    
    public Book(int bookId, String isbn, String title, Date published, String genre, List<Author> authors, int rating) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.published = published;
        this.authors = new ArrayList<>();
        this.genre = genre;
        fillAuthor(authors);
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getBookId() { return bookId; }

    public String getIsbn() { return isbn; }

    public String getTitle() { return title; }

    public Date getPublished() { return published; }

    public String getGenre() { return genre;}

    public List<Author> getAuthors() {
        ArrayList<Author> copy = new ArrayList<>();
        for (Author author: authors) {
            copy.add(new Author(author.getAuthorId(),author.getName(),author.getLastName()));
        }
        return copy;
    }

    private void fillAuthor(List<Author> authors) {
        for (Author author: authors) {
            this.authors.add(new Author(author.getAuthorId(),author.getName(),author.getLastName()));
        }
    }
    
    @Override
    public String toString() {
        return title + ", " + isbn + ", " + published + ", " + authors.toString() + ", " + genre;
    }
}

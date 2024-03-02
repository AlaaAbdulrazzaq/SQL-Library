package se.kth.alialaa.labb1db1.model;

public class Author {
    private int authorId;
    private String name;
    private String lastName;

    public Author(int authorId,String name, String lastName) {
        this.authorId = authorId;
        this.name = name;
        this.lastName = lastName;
    }

    public int getAuthorId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return name + ", " + lastName + ", Id: " + authorId;
    }
}

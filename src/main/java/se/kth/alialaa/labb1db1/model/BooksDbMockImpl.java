/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.alialaa.labb1db1.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Class.forName;


/**
 * A mock implementation of the BooksDBInterface interface to demonstrate how to
 * use it together with the user interface.
 * <p>
 * Your implementation must access a real database.
 *
 * @author anderslm@kth.se
 */
public class BooksDbMockImpl implements BooksDbInterface {
    private Connection con;
    private final List<Book> books;
    public BooksDbMockImpl() {
        con = null;
        books = new ArrayList<>();
    }

    //host: root, 1234567890
    //klient: klientAnvändare, lösenord
    @Override
    public boolean connect(String database) throws BooksDbException {
        String url = "jdbc:mysql://localhost:3306/";
        url += database;
        String username= "klientAnvändare";
        String password = "lösenord";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url,username,password);
            return true;
        } catch (Exception e) {
            throw new BooksDbException("rad 42");
        }
    }

    @Override
    public void disconnect() throws BooksDbException {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
                System.out.println("rad 52");
            }
        }catch (Exception e) {
            throw new BooksDbException("rad 53");
        }
    }



    @Override
    public List<Book> searchBooksByTitle(String searchTitle) throws BooksDbException, SQLException {
        if (con == null) {
            throw new BooksDbException("No Connection");
        }
        List<Book> result = new ArrayList<>();
        String searchSql = "SELECT * FROM book WHERE title LIKE ?";
        try (PreparedStatement searchStatement = con.prepareStatement(searchSql)){
            searchStatement.setString(1, "%" + searchTitle + "%");
            ResultSet resultSet = searchStatement.executeQuery();

            while (resultSet.next()) {
                result.add(new Book(resultSet.getInt("bookId"), resultSet.getString("ISBN"),resultSet.getString("title"),
                        resultSet.getDate("published"),resultSet.getString("genre"),getAuthorsForBook(resultSet.getInt("bookID")), resultSet.getInt("rating")));
            }
        }
        return result;
    }

    public Book searchByBookId(int bookId) throws BooksDbException, SQLException {
        if(con == null) {
            throw new BooksDbException("No Connectiont");
        }
        String searchSql = "SELECT * FROM book WHERE bookId = ?";
        try (PreparedStatement searchStatement = con.prepareStatement(searchSql)) {
            searchStatement.setInt(1,bookId);
            ResultSet resultSet = searchStatement.executeQuery();
            if (resultSet.next()) {
            return new Book(resultSet.getInt("bookId"),resultSet.getString("ISBN"),resultSet.getString("title"),
                    resultSet.getDate("published"),resultSet.getString("genre"),getAuthorsForBook(resultSet.getInt("bookId")), resultSet.getInt("rating"));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Author returnAuthor(int authorId) throws SQLException, BooksDbException {
        if(con == null) {
            throw new BooksDbException("No Connection");
        }
        String searchSql = "SELECT name, lastname FROM author WHERE authorId = ?";
        try (PreparedStatement searchStatement = con.prepareStatement(searchSql)){
            searchStatement.setInt(1, authorId);
            ResultSet resultSet = searchStatement.executeQuery();
            if (resultSet.next()) {
                return new Author(authorId, resultSet.getString("name"), resultSet.getString("lastname"));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Book> searchBooksByISBN(String ISBN) throws BooksDbException {
        if(con == null) {
            throw new BooksDbException("No Connection!");
        }
        List<Book> result = new ArrayList<>();
        String searchSql = "SELECT * FROM Book WHERE ISBN LIKE ?";
        try (PreparedStatement searchStatement = con.prepareStatement(searchSql)){
            searchStatement.setString(1,"%" + ISBN + "%");
            ResultSet resultSet = searchStatement.executeQuery();
            while (resultSet.next()) {
                result.add(new Book(resultSet.getInt("bookId"),resultSet.getString("ISBN"),resultSet.getString("title"),
                        resultSet.getDate("published"),resultSet.getString("genre"),getAuthorsForBook(resultSet.getInt("bookId")), resultSet.getInt("rating")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public List<Book> searchBooksByAuthor(String author) throws BooksDbException {
        if(con == null) {
            throw new BooksDbException("No Connection!");
        }
        List<Book> result = new ArrayList<>();
        String searchSql = "SELECT book.* FROM book INNER JOIN book_author ON book.bookId = book_author.bookId INNER JOIN author ON book_author.authorId = author.authorId WHERE CONCAT(author.name, ' ', author.lastname) LIKE ?";

        try (PreparedStatement searchByAuthorStatement = con.prepareStatement(searchSql)){
            searchByAuthorStatement.setString(1, "%" + author + "%");
            ResultSet resultSet = searchByAuthorStatement.executeQuery();

            while (resultSet.next()) {
                int bookID = resultSet.getInt("bookId");
                if(!containsBookWithId(result, bookID))
                result.add(new Book(resultSet.getInt("bookId"), resultSet.getString("ISBN"), resultSet.getString("title"),
                        resultSet.getDate("published"), resultSet.getString("genre"), getAuthorsForBook(resultSet.getInt("bookId")), resultSet.getInt("rating")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private boolean containsBookWithId(List<Book> books, int bookId) {
        for (Book book : books) {
            if (book.getBookId() == bookId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean insert(Book book) throws BooksDbException, SQLException {
        if(con == null) {
            throw new BooksDbException("No Connection!");
        }
        boolean success = false;
        try {
            con.setAutoCommit(false);

            boolean BookInserted = insertBookTable(book);
            if (!BookInserted) {
                con.rollback();
                return false;
            }

            boolean authorsInserted = insertAuthorTable(book);
            if (!authorsInserted) {
                con.rollback();
                return false;
            }

            boolean linkCreated = linkBookAuthor(book);
            if (!linkCreated) {
                con.rollback();
                return false;
            }

            con.commit();
            success = true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new BooksDbException("Rollback failed" + ex.getMessage());
            }
            throw new BooksDbException("Insertion failed" + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true); // Reset auto-commit mode
            } catch (SQLException e) {
                throw new BooksDbException("Failed to reset auto-commit: " + e.getMessage());
            }
        }
        return success;
    }

    @Override
    public void delete(Book book) throws BooksDbException {
        if (con == null) {
            throw new BooksDbException("No Connection!");
        }

        try {
            con.setAutoCommit(false);

            // First, check if the book exists in the database
            String checkBookSql = "SELECT COUNT(*) FROM Book WHERE bookId = ?";
            try (PreparedStatement checkBookStatement = con.prepareStatement(checkBookSql)){
                checkBookStatement.setInt(1, book.getBookId());
                ResultSet resultSet = checkBookStatement.executeQuery();
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    throw new BooksDbException("Book does not exist");
                }
            }

            // Delete links in Book_Author table
            String deleteLinkSql = "DELETE FROM Book_Author WHERE BookId = ?";
            try (PreparedStatement deleteLinkStatement = con.prepareStatement(deleteLinkSql)){
                deleteLinkStatement.setInt(1, book.getBookId());
                deleteLinkStatement.executeUpdate();
            }

            // Delete the book
            String deleteBookSql = "DELETE FROM Book WHERE BookId = ?";
            try (PreparedStatement deleteBookStatement = con.prepareStatement(deleteBookSql)){
                deleteBookStatement.setInt(1, book.getBookId());
                deleteBookStatement.executeUpdate();
            }

            // Check if authors are linked to other books and delete them if not
            for (Author author : book.getAuthors()) {
                String checkLinkSql = "SELECT COUNT(*) FROM Book_Author WHERE authorId = ?";
                try (PreparedStatement checkLinkStatement = con.prepareStatement(checkLinkSql)){
                    checkLinkStatement.setInt(1, author.getAuthorId());
                    ResultSet linkResultSet = checkLinkStatement.executeQuery();
                    if (linkResultSet.next() && linkResultSet.getInt(1) == 0) {
                        String deleteAuthorSql = "DELETE FROM Author WHERE authorId = ?";
                        try (PreparedStatement deleteAuthorStatement = con.prepareStatement(deleteAuthorSql)){
                            deleteAuthorStatement.setInt(1, author.getAuthorId());
                            deleteAuthorStatement.executeUpdate();
                        }
                    }
                }
            }

            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new BooksDbException("Rollback failed: " + ex.getMessage());
            }
            throw new BooksDbException("Deletion failed: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                throw new BooksDbException("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    @Override
    public void addAuthorToBook(Book book, List<Author> authors) throws SQLException, BooksDbException {
        if (con == null) {
            throw new BooksDbException("No connection");
        }

        String checkAuthorSql = "SELECT COUNT(*) FROM Author WHERE authorId = ?";
        String insertAuthorSQL = "INSERT INTO Author (authorId, name, lastname) VALUES (?, ?, ?)";
        String checkLinkSql = "SELECT COUNT(*) FROM Book_Author WHERE BookId = ? AND authorId = ?";
        String insertLinkSQL = "INSERT INTO Book_Author (bookId, authorId) VALUES (?, ?)";

        try (PreparedStatement checkAuthorStatement = con.prepareStatement(checkAuthorSql);
             PreparedStatement insertAuthorStatement = con.prepareStatement(insertAuthorSQL);
             PreparedStatement checkLinkStatement = con.prepareStatement(checkLinkSql);
             PreparedStatement insertLinkStatement = con.prepareStatement(insertLinkSQL)) {

            for (Author author : authors) {
                // Check if author exists in the Author table
                checkAuthorStatement.setInt(1, author.getAuthorId());
                ResultSet authorResultSet = checkAuthorStatement.executeQuery();
                if (!authorResultSet.next() || authorResultSet.getInt(1) == 0) {
                    // Author does not exist, insert new author
                    insertAuthorStatement.setInt(1, author.getAuthorId());
                    insertAuthorStatement.setString(2, author.getName());
                    insertAuthorStatement.setString(3, author.getLastName());
                    insertAuthorStatement.executeUpdate();
                }

                // Check if a link between book and author already exists
                checkLinkStatement.setInt(1, book.getBookId());
                checkLinkStatement.setInt(2, author.getAuthorId());
                ResultSet linkResultSet = checkLinkStatement.executeQuery();
                if (!linkResultSet.next() || linkResultSet.getInt(1) == 0) {
                    // Link does not exist, create it
                    insertLinkStatement.setInt(1, book.getBookId());
                    insertLinkStatement.setInt(2, author.getAuthorId());
                    insertLinkStatement.executeUpdate();
                }
            }
        }
    }



    @Override
    public void addReview(int bookId, int rating) throws BooksDbException {
        if (con == null) {
            throw new BooksDbException("No connection");
        }
        try {
            int affectedRows = 0;
            String updateSql = "UPDATE book SET rating = ? WHERE bookId = ?";
            PreparedStatement preparedStatement = con.prepareStatement(updateSql);
            preparedStatement.setInt(1, rating);
            preparedStatement.setInt(2, bookId);
            if(rating > 1 || rating < 5) {
                affectedRows = preparedStatement.executeUpdate();
            }
            if (affectedRows == 0) {
                throw new SQLException("Adding review failed, no rows affected");
            }
            preparedStatement.close();

        } catch (SQLException e) {
            throw new BooksDbException("Error adding review: " + e.getMessage());
        }
    }

    private List<Author> getAuthorsForBook(int bookId) throws SQLException {
        List<Author> authors = new ArrayList<>();

        String getAuthorsSql = "SELECT a.* FROM Author a JOIN Book_Author ba ON a.authorId = ba.authorId WHERE ba.bookId = ?";

        try (PreparedStatement getAuthorsStatement = con.prepareStatement(getAuthorsSql)) {
            getAuthorsStatement.setInt(1, bookId);

            ResultSet resultSet = getAuthorsStatement.executeQuery();

            while (resultSet.next()) {
                int authorId = resultSet.getInt("authorId");
                String name = resultSet.getString("name");
                String lastName = resultSet.getString("lastname");

                // Skapa Author-objekt och lägg till i listan
                Author author = new Author(authorId, name, lastName);
                authors.add(author);
            }
        }

        return authors;
    }

    private boolean insertBookTable(Book book) {
        String checkBookSql = "SELECT COUNT(*) FROM Book WHERE bookID = ?";
        try (PreparedStatement preparedStatement = con.prepareStatement(checkBookSql)){
            preparedStatement.setInt(1,book.getBookId());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);

            if (count == 0) {
                String insertBookSQL= "INSERT INTO book (bookId, isbn, title, published, genre, rating) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertBookStatement = con.prepareStatement(insertBookSQL)){
                    insertBookStatement.setInt(1, book.getBookId());
                    insertBookStatement.setString(2,book.getIsbn());
                    insertBookStatement.setString(3,book.getTitle());
                    insertBookStatement.setDate(4, book.getPublished());
                    insertBookStatement.setString(5,book.getGenre());
                    insertBookStatement.setInt(6, book.getRating());

                    insertBookStatement.executeUpdate();

                }catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            else {
                return false;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean insertAuthorTable(Book book) {
        List<Author> authors = book.getAuthors();
        String checkAuthorSql = "SELECT COUNT(*) FROM Author WHERE AuthorId = ?";
        try (PreparedStatement checkAuthorStatement = con.prepareStatement(checkAuthorSql)) {
            for (Author author : authors) {
                checkAuthorStatement.setInt(1, author.getAuthorId());
                ResultSet resultSet = checkAuthorStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count == 0) {
                    String insertAuthorSQL = "INSERT INTO author(authorId, name, lastname) VALUES (?, ?, ?)";
                    try (PreparedStatement preparedStatement = con.prepareStatement(insertAuthorSQL)) {
                        preparedStatement.setInt(1, author.getAuthorId());
                        preparedStatement.setString(2, author.getName());
                        preparedStatement.setString(3, author.getLastName());
                        preparedStatement.executeUpdate();
                    }
                }
                // Do not return false here; continue with the next author
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Return false only if there's an SQL exception
        }
        return true;
    }

    private boolean linkBookAuthor(Book book) {
        List<Author> authors = book.getAuthors();
        String checkLink = "SELECT COUNT(*) FROM Book_Author WHERE bookID = ? AND authorID = ?";
        try (PreparedStatement checkBookAuthorStatement = con.prepareStatement(checkLink)){
            for (int a=0; a<authors.size(); a++) {
                checkBookAuthorStatement.setInt(1, book.getBookId());
                checkBookAuthorStatement.setInt(2,authors.get(a).getAuthorId());

                ResultSet resultSet = checkBookAuthorStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count == 0) {
                    String linkBookAuthorSQL = "INSERT INTO Book_Author (BookId, authorId) VALUES (?, ?)";
                    try (PreparedStatement preparedStatement = con.prepareStatement(linkBookAuthorSQL)){
                        preparedStatement.setInt(1,book.getBookId());
                        preparedStatement.setInt(2,authors.get(a).getAuthorId());
                        preparedStatement.executeUpdate();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    return false;
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void deleteLink(Book book) {
        String deleteBookAuthorSql = "DELETE FROM Book_Author WHERE BookId = ?";
        try (PreparedStatement deleteBookAuthorStatement = con.prepareStatement(deleteBookAuthorSql)){
            deleteBookAuthorStatement.setInt(1, book.getBookId());
            deleteBookAuthorStatement.executeUpdate();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteBook(Book book) {
        String deleteBookSql = "DELETE FROM Book WHERE BookId = ?";

        try (PreparedStatement deleteBookStatement = con.prepareStatement(deleteBookSql)){
            deleteBookStatement.setInt(1,book.getBookId());
            deleteBookStatement.executeUpdate();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAuthor(Book book) throws SQLException {
        List<Author> authors = book.getAuthors();
        for (Author author : authors) {
            String sql = "SELECT COUNT(*) FROM book_author WHERE authorId = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)){
                preparedStatement.setInt(1,author.getAuthorId());
                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count == 0) {
                    String deleteAuthorSql = "DELETE author FROM author WHERE authorId = ?";
                    try (PreparedStatement deleteAuthorStatement = con.prepareStatement(deleteAuthorSql)){
                        deleteAuthorStatement.setInt(1, author.getAuthorId());
                        deleteAuthorStatement.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
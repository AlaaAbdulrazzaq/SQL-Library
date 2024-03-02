package se.kth.alialaa.labb1db1.view;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import se.kth.alialaa.labb1db1.model.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static javafx.scene.control.Alert.AlertType.*;

/**
 * The controller is responsible for handling user requests and update the view
 * (and in some cases the model).
 *
 * @author anderslm@kth.se
 */
public class Controller {

    private final BooksPane booksView; // view
    final BooksDbInterface booksDb; // model
    private ExecutorService executorService;
    public Controller(BooksDbInterface booksDb, BooksPane booksView) {
        this.booksDb = booksDb;
        this.booksView = booksView;
        executorService = Executors.newFixedThreadPool(10);
    }

    protected void onSearchSelected(String searchFor, SearchMode mode) {
        executorService.submit(() -> {
            try {
                if (searchFor != null && searchFor.length() > 1) {
                    List<Book> result = null;
                    switch (mode) {
                        case Title:
                            result = booksDb.searchBooksByTitle(searchFor);
                            break;
                        case ISBN:
                            result = booksDb.searchBooksByISBN(searchFor);
                            break;
                        case Author:
                            result = booksDb.searchBooksByAuthor(searchFor);
                            break;
                        default:
                            result = new ArrayList<>();
                    }
                    if (result == null || result.isEmpty()) {
                        booksView.showAlertAndWait(
                                "No results found.", INFORMATION);
                    } else {
                        booksView.displayBooks(result);
                    }
                } else {
                    booksView.showAlertAndWait(
                            "Enter a search string!", WARNING);
                }
            } catch (Exception e) {
                booksView.showAlertAndWait("Database error.", ERROR);
            }
        });
    }

    protected void handleConnection(String database) {
        executorService.submit(() -> {
        try {
            booksDb.connect(database);
        }catch (Exception e) {
            e.printStackTrace();
        }
        });
    }
    protected Author handleGetAuthor(int authorId) {
        Future<Author> future = executorService.submit(() -> {
            try {
                return booksDb.returnAuthor(authorId);
            } catch (SQLException | BooksDbException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            // Wait for the task to complete and get the result.
            // This will block the current thread (should not be the JavaFX Application Thread)
            return future.get();
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
            return null;
        }
    }

    protected void handleDisconnect() {
        executorService.submit(() -> {
            try {
                booksDb.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    protected void handleRemove(int bookId) {
        executorService.submit(() -> {
            try {
                booksDb.delete(booksDb.searchByBookId(bookId));
            } catch (SQLException | BooksDbException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void handleAdd(Book book) {
        executorService.submit(() -> {
            try {
                if (booksDb.insert(book)) {
                    booksView.showAlertAndWait("Book added sucessfully!", CONFIRMATION);
                } else
                    booksView.showAlertAndWait("Book ID or ISBN or Author ID was not unique!", ERROR);
            } catch (SQLException | BooksDbException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void handleAddAuthorToBook(Book book, List<Author> author) {
        executorService.submit(() -> {
            try {
                booksDb.addAuthorToBook(book, author);
                Platform.runLater(() -> booksView.showAlertAndWait("Author added successfully to the book!", Alert.AlertType.INFORMATION));
            } catch (SQLException | BooksDbException e) {
                Platform.runLater(() -> booksView.showAlertAndWait("Error adding author to book: " + e.getMessage(), Alert.AlertType.ERROR));
                e.printStackTrace();
            }
        });
    }

    protected void handleReviewAdd(int bookId ,int review) {
        executorService.submit(() -> {
            try {
                booksDb.addReview(bookId, review);
            } catch (BooksDbException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdownApplication() {
        // Stäng ner ExecutorService
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        // Stäng databasanslutningen
        try {
            if (booksDb != null) {
                booksDb.disconnect();
            }
        } catch (BooksDbException e) {
            // Hantera eventuella fel vid avstängning av databasanslutningen
        }
    }

    // TODO:
    // Add methods for all types of user interaction (e.g. via  menus).
}

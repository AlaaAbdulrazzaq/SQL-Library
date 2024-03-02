package se.kth.alialaa.labb1db1.view;

import java.sql.Array;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.Text;
import se.kth.alialaa.labb1db1.model.*;

import javax.security.auth.callback.Callback;


/**
 * The main pane for the view, extending VBox and including the menus. An
 * internal BorderPane holds the TableView for books and a search utility.
 *
 * @author anderslm@kth.se
 */
public class BooksPane extends VBox {

    private TableView<Book> booksTable;
    private ObservableList<Book> booksInTable; // the data backing the table view
    private ComboBox<SearchMode> searchModeBox;
    private TextField searchField;
    private Button searchButton;
    private MenuBar menuBar;
    private Controller controller;

    public BooksPane(BooksDbMockImpl booksDb) {
        this.controller = new Controller(booksDb, this);
        this.init(controller);
    }

    /**
     * Display a new set of books, e.g. from a database select, in the
     * booksTable table view.
     *
     * @param books the books to display
     */
    public void displayBooks(List<Book> books) {
        booksInTable.clear();
        booksInTable.addAll(books);
    }
    
    /**
     * Notify user on input error or exceptions.
     * 
     * @param msg the message
     * @param type types: INFORMATION, WARNING et c.
     */
    protected void showAlertAndWait(String msg, Alert.AlertType type) {
        // types: INFORMATION, WARNING et c.
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }

    private void init(Controller controller) {

        booksInTable = FXCollections.observableArrayList();

        // init views and event handlers
        initBooksTable();
        initSearchView(controller);
        initMenus(controller);

        FlowPane bottomPane = new FlowPane();
        bottomPane.setHgap(10);
        bottomPane.setPadding(new Insets(10, 10, 10, 10));
        bottomPane.getChildren().addAll(searchModeBox, searchField, searchButton);

        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(booksTable);
        mainPane.setBottom(bottomPane);
        mainPane.setPadding(new Insets(10, 10, 10, 10));

        this.getChildren().addAll(menuBar, mainPane);
        VBox.setVgrow(mainPane, Priority.ALWAYS);
    }

    private void initBooksTable() {
        booksTable = new TableView<>();
        booksTable.setEditable(false); // don't allow user updates (yet)
        booksTable.setPlaceholder(new Label("No rows to display"));

        // define columns
        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        TableColumn<Book, Date> publishedCol = new TableColumn<>("Published");
        TableColumn<Book, String> genreCol = new TableColumn<>("Genre");
        TableColumn<Book, Float> ratingCol = new TableColumn<>("Rating");
        booksTable.getColumns().addAll(titleCol, isbnCol, publishedCol, genreCol, ratingCol);
        // give title column some extra space
        titleCol.prefWidthProperty().bind(booksTable.widthProperty().multiply(0.5));

        // define how to fill data for each cell, 
        // get values from Book properties
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        publishedCol.setCellValueFactory(new PropertyValueFactory<>("published"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));

        
        // associate the table view with the data
        booksTable.setItems(booksInTable);
    }

    private void initSearchView(Controller controller) {
        searchField = new TextField();
        searchField.setPromptText("Search for...");
        searchModeBox = new ComboBox<>();
        searchModeBox.getItems().addAll(SearchMode.values());
        searchModeBox.setValue(SearchMode.Title);
        searchButton = new Button("Search");
        
        // event handling (dispatch to controller)
        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String searchFor = searchField.getText();
                SearchMode mode = searchModeBox.getValue();
                controller.onSearchSelected(searchFor, mode);
            }
        });
    }

    private void initMenus(Controller controller) {

        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem connectItem = new MenuItem("Connect to Db");
        MenuItem disconnectItem = new MenuItem("Disconnect");
        fileMenu.getItems().addAll(exitItem, connectItem, disconnectItem);

        Menu manageMenu = new Menu("Manage");
        MenuItem addItem = new MenuItem("Add");
        MenuItem removeItem = new MenuItem("Remove Book");
        MenuItem addReview = new MenuItem("Add review");
        MenuItem addAuthorToBook = new MenuItem("Add Author");

        manageMenu.getItems().addAll(addItem, removeItem, addAuthorToBook, addReview);

        menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, manageMenu);

        addAuthorToBook.setOnAction(event -> {
            TextInputDialog bookIdDialog = new TextInputDialog();
            bookIdDialog.setTitle("Add Author to Book");
            bookIdDialog.setHeaderText("Enter the Book ID:");
            bookIdDialog.setContentText("Book ID:");
            Optional<String> bookIdResult = bookIdDialog.showAndWait();

            if (bookIdResult.isPresent()) {
                try {
                    int bookId = Integer.parseInt(bookIdResult.get().trim());
                    Book book = controller.booksDb.searchByBookId(bookId);
                    if (book == null) {
                        showAlertAndWait("Book not found!", Alert.AlertType.ERROR);
                        return;
                    }

                    List<Author> authors = askForAuthorsDetails();
                        controller.handleAddAuthorToBook(book, authors);
                } catch (NumberFormatException e) {
                    showAlertAndWait("Invalid Book ID. Please enter a valid number!", Alert.AlertType.ERROR);
                } catch (Exception e) {
                    showAlertAndWait("Error: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });



        addReview.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TextInputDialog dialog = new TextInputDialog("Review");
                dialog.setTitle("Review");
                dialog.setHeaderText("Set review");

                dialog.getDialogPane().getChildren().remove(1);

                TextField ratingField = new TextField();
                ratingField.setPromptText("Rating (1 - 5)");
                TextField bookIdField = new TextField();
                bookIdField.setPromptText("Book ID");

                VBox vBox = new VBox(10, ratingField, bookIdField);
                dialog.getDialogPane().setContent(vBox);

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(input -> {
                    try {
                        int rating = Integer.parseInt(ratingField.getText());
                        int bookId = Integer.parseInt(bookIdField.getText());
                        controller.handleReviewAdd(bookId, rating);
                    } catch (NumberFormatException e) {
                        throw new NumberFormatException("Invalid input " + e.getMessage());
                    }
                });
            }
        });

        connectItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TextInputDialog dialog = new TextInputDialog("defaultDbName");
                dialog.setTitle("Database Connection");
                dialog.setHeaderText("Connect to Database");
                dialog.setContentText("Please enter the database name:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(databaseName -> controller.handleConnection(databaseName));
            }
        });

        exitItem.setOnAction(e -> {
                controller.shutdownApplication();
                Platform.exit();
        });

        disconnectItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                controller.handleDisconnect();
            }
        });

        removeItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TextInputDialog dialog = new TextInputDialog(" ");
                dialog.setTitle("Remove");
                dialog.setHeaderText("Remove a book");
                dialog.setContentText("Please enter the bookid:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(bookId -> controller.handleRemove(Integer.parseInt(bookId)));
            }
        });

        addItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Multi-Input Dialog");
                dialog.setHeaderText("Ange information");
                dialog.getDialogPane().setContentText("Ange följande information:");

                GridPane gridPane = makeGridPane();

                Label label1 = makeLabel("Book Id?");
                Label label2 = makeLabel("ISBN?");
                Label label3 = makeLabel("Title?");
                Label label4 = makeLabel("Published?");
                Label label5 = makeLabel("Genre?");
                Label label6 = makeLabel("Rating?");
                Label label7 = makeLabel("Number of authors?");

                TextField textField1 = new TextField();
                TextField textField2 = new TextField();
                TextField textField3 = new TextField();
                TextField textField4 = new TextField();
                TextField textField5 = new TextField();
                TextField textField6 = new TextField();
                TextField textField7 = new TextField();

                gridPane.add(label1, 0, 0);
                gridPane.add(label2, 0, 1);
                gridPane.add(label3, 0, 2);
                gridPane.add(label4, 0, 3);
                gridPane.add(label5, 0, 4);
                gridPane.add(label6, 0, 5);
                gridPane.add(label7, 0, 6);

                gridPane.add(textField1, 1, 0);
                gridPane.add(textField2, 1, 1);
                gridPane.add(textField3, 1, 2);
                gridPane.add(textField4, 1, 3);
                gridPane.add(textField5, 1, 4);
                gridPane.add(textField6, 1, 5);
                gridPane.add(textField7, 1, 6);

                dialog.getDialogPane().setContent(gridPane);
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(text -> {System.out.println("Användarens input: " + text);});

                int numberOfAuthors = 0;
                try {
                    numberOfAuthors = Integer.parseInt(textField7.getText());
                } catch (NumberFormatException e) {
                    showAlertAndWait("Invalid number of authors!", Alert.AlertType.ERROR);
                    return;
                }

                if (numberOfAuthors > 0) {
                    List<Author> authors = addAuthor(numberOfAuthors);

                    int bookId = Integer.parseInt(textField1.getText());
                    String isbn = textField2.getText();
                    String title = textField3.getText();
                    Date published = Date.valueOf(textField4.getText()); // Antag att formatet är YYYY-MM-DD
                    String genre = textField5.getText();
                    int rating = Integer.parseInt(textField6.getText());
                    controller.handleAdd(new Book(bookId, isbn, title, published, genre, authors, rating));
                }
            }
        });
    }

    public Controller getController() {
        return this.controller;
    }

    private List<Author> askForAuthorsDetails() {
        List<Author> authors = new ArrayList<>();
        boolean moreAuthors = true;

        while (moreAuthors) {
            ChoiceDialog<String> authorTypeDialog = new ChoiceDialog<>("Existing Author", "Existing Author", "New Author");
            authorTypeDialog.setTitle("Author Type");
            authorTypeDialog.setHeaderText("Choose Author Type");
            authorTypeDialog.setContentText("Select author type:");

            Optional<String> authorTypeResult = authorTypeDialog.showAndWait();
            if (authorTypeResult.isPresent()) {
                if ("Existing Author".equals(authorTypeResult.get())) {
                    TextInputDialog existingAuthorDialog = new TextInputDialog();
                    existingAuthorDialog.setTitle("Existing Author");
                    existingAuthorDialog.setHeaderText("Enter Existing Author ID");
                    existingAuthorDialog.setContentText("Author ID:");

                    Optional<String> existingAuthorId = existingAuthorDialog.showAndWait();
                    existingAuthorId.ifPresent(authorId -> {
                        try {
                            Author author = controller.handleGetAuthor(Integer.parseInt(authorId));
                            if (author != null) {
                                authors.add(author);
                            } else {
                                showAlertAndWait("No author found with this ID", Alert.AlertType.ERROR);
                            }
                        } catch (NumberFormatException e) {
                            showAlertAndWait("Invalid Author ID", Alert.AlertType.ERROR);
                        }
                    });
                } else {
                    TextInputDialog newAuthorDialog = new TextInputDialog();
                    newAuthorDialog.setTitle("New Author Details");
                    newAuthorDialog.setHeaderText("Enter New Author Details");

                    GridPane grid = new GridPane();
                    grid.setVgap(10);
                    grid.setHgap(10);
                    grid.setPadding(new Insets(20, 150, 10, 10));

                    TextField idField = new TextField();
                    idField.setPromptText("Author ID");
                    TextField nameField = new TextField();
                    nameField.setPromptText("First Name");
                    TextField lastNameField = new TextField();
                    lastNameField.setPromptText("Last Name");

                    grid.add(new Label("Author ID:"), 0, 0);
                    grid.add(idField, 1, 0);
                    grid.add(new Label("First Name:"), 0, 1);
                    grid.add(nameField, 1, 1);
                    grid.add(new Label("Last Name:"), 0, 2);
                    grid.add(lastNameField, 1, 2);

                    newAuthorDialog.getDialogPane().setContent(grid);

                    Optional<String> newAuthorResult = newAuthorDialog.showAndWait();
                    newAuthorResult.ifPresent(result -> {
                        try {
                            int authorId = Integer.parseInt(idField.getText());
                            String firstName = nameField.getText();
                            String lastName = lastNameField.getText();
                            authors.add(new Author(authorId, firstName, lastName));
                        } catch (NumberFormatException e) {
                            showAlertAndWait("Invalid input for Author ID", Alert.AlertType.ERROR);
                        }
                    });
                }
            }

            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Add another author?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> confirmResult = confirmDialog.showAndWait();
            moreAuthors = confirmResult.isPresent() && confirmResult.get() == ButtonType.YES;
        }

        return authors;
    }

    private List<Author> addAuthor(int numberOfAuthors) {
        List<Author> authors = new ArrayList<>();
        for (int i = 0; i < numberOfAuthors; i++) {
            List<String> choices = Arrays.asList("Existing Author", "New Author");
            ChoiceDialog<String> authorChoiceDialog = new ChoiceDialog<>("Existing Author", choices);
            authorChoiceDialog.setTitle("Author Selection");
            authorChoiceDialog.setHeaderText("Choose Author Type");
            authorChoiceDialog.setContentText("Select:");

            Optional<String> authorType = authorChoiceDialog.showAndWait();
            if (authorType.isPresent()) {
                if (authorType.get().equals("Existing Author")) {
                    TextInputDialog authorIdDialog = new TextInputDialog();
                    authorIdDialog.setTitle("Existing Author ID");
                    authorIdDialog.setHeaderText("Enter Author ID");
                    authorIdDialog.setContentText("Author ID:");
                    Optional<String> authorIdInput = authorIdDialog.showAndWait();
                    authorIdInput.ifPresent(authorId -> {
                        authors.add(controller.handleGetAuthor(Integer.parseInt(authorId)));
                    });
                } else {
                    // New Author: Ask for ID, First Name, and Last Name
                    TextInputDialog newAuthorDialog = new TextInputDialog();
                    newAuthorDialog.setTitle("New Author Details");
                    newAuthorDialog.setHeaderText("Enter Author Details");

                    GridPane newAuthorGridPane = makeGridPane(); // Create a new GridPane for each author

                    TextField idField = new TextField();
                    idField.setPromptText("Author ID");
                    TextField firstNameField = new TextField();
                    firstNameField.setPromptText("First Name");
                    TextField lastNameField = new TextField();
                    lastNameField.setPromptText("Last Name");
                    newAuthorGridPane.add(new Label("Author ID:"), 0, 0);
                    newAuthorGridPane.add(idField, 1, 0);
                    newAuthorGridPane.add(new Label("First Name:"), 0, 1);
                    newAuthorGridPane.add(firstNameField, 1, 1);
                    newAuthorGridPane.add(new Label("Last Name:"), 0, 2);
                    newAuthorGridPane.add(lastNameField, 1, 2);
                    newAuthorDialog.getDialogPane().setContent(newAuthorGridPane);

                    Optional<String> newAuthorResult = newAuthorDialog.showAndWait();
                    if (newAuthorResult.isPresent()) {
                        authors.add(new Author(Integer.parseInt(idField.getText()),
                                firstNameField.getText(),
                                lastNameField.getText()));
                    }
                }
            }
        }
        return authors;
    }

    private Label makeLabel(String text) {
        return new Label(text);
    }

    private GridPane makeGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));
        return gridPane;
    }
}
package se.kth.alialaa.labb1db1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import se.kth.alialaa.labb1db1.model.Author;
import se.kth.alialaa.labb1db1.model.Book;
import se.kth.alialaa.labb1db1.model.BooksDbException;
import se.kth.alialaa.labb1db1.model.BooksDbMockImpl;
import se.kth.alialaa.labb1db1.view.BooksPane;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Application start up.
 *
 * @author anderslm@kth.se
 */
public class Main extends Application {
    private BooksPane root;
    @Override
    public void start(Stage primaryStage) throws BooksDbException, SQLException {
        BooksDbMockImpl booksDb = new BooksDbMockImpl(); // model
        // Don't forget to connect to the db, somewhere...
        root = new BooksPane(booksDb);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Books Database Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (root != null) {
            root.getController().shutdownApplication();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

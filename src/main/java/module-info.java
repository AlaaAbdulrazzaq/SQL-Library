module se.kth.alialaa.labb1db1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens se.kth.alialaa.labb1db1 to javafx.fxml;
    opens se.kth.alialaa.labb1db1.model to javafx.base;
    exports se.kth.alialaa.labb1db1;
}
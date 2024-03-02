package se.kth.alialaa.labb1db1.model;

public class Review {
    private int bookId;
    private int reviewId;
    private float rating;
    public Review(int bookId, int reviewId, float rating) {
        this.bookId = bookId;
        this.reviewId = reviewId;
        this.rating = rating;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}

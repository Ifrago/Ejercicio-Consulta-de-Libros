package edu.upc.eetac.dsa.ifrago.books.android;

public interface MediaType {
	public final static String BOOKS_API_BOOKS = "application/vnd.books.api.books+json";
	public final static String BOOKS_API_BOOKS_COLLECTION = "application/vnd.books.api.books.collection+json";
	public final static String BOOKS_API_REVIEWS = "application/vnd.books.api.reviews+json";
	public final static String BOOKS_API_REVIEWS_COLLECTION = "application/vnd.books.api.reviews.collection+json";
	public final static String BOOKS_API_ERROR = "application/vnd.dsa.books.error+json";//Para el error
}
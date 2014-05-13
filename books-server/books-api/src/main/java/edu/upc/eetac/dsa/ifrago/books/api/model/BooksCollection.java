package edu.upc.eetac.dsa.ifrago.books.api.model;

import java.util.ArrayList;
import java.util.List;

public class BooksCollection {
	
	private List<Books> books;
	private int beforebook;
	private int afterbook;
	
	
	
	public BooksCollection() {
		super();
		books = new ArrayList<Books>();
	}
	
	public void addBook(Books book) {
		books.add(book);
	}
	
	public List<Books> getBooks() {
		return books;
	}
	public void setBooks(List<Books> books) {
		this.books = books;
	}
	public int getBeforebook() {
		return beforebook;
	}
	public void setBeforebook(int beforebook) {
		this.beforebook = beforebook;
	}
	public int getAfterbook() {
		return afterbook;
	}
	public void setAfterbook(int afterbook) {
		this.afterbook = afterbook;
	}
	
	

}

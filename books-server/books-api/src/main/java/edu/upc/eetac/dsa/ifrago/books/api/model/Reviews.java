package edu.upc.eetac.dsa.ifrago.books.api.model;

import java.util.Date;

public class Reviews {
	
	int reviewsid=0;
	String username=null;
	Date dateupdate=null;
	String text=null;
	int bookid=0;
	
	public int getReviewsid() {
		return reviewsid;
	}
	public void setReviewsid(int reviewsid) {
		this.reviewsid = reviewsid;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Date getDateupdate() {
		return dateupdate;
	}
	public void setDateupdate(Date dateupdate) {
		this.dateupdate = dateupdate;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getBookid() {
		return bookid;
	}
	public void setBookid(int bookid) {
		this.bookid = bookid;
	}
	
	

}

package edu.upc.eetac.dsa.ifrago.books.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.ifrago.books.api.model.BookRootAPI;

@Path("/")
public class BookRootAPIResource {
	@Context
	SecurityContext security;
	
	private boolean admin, registered;
	
	
	@GET
	public BookRootAPI getBookAPI() {
		BookRootAPI api = new BookRootAPI();
		return api;
	}
	
	public boolean isAdministrador(){
		return this.admin;
	}
	public void setAdministrador(boolean administrador){
		this.admin=administrador;
	}
	
	public boolean isRegistered(){
		return this.registered;
	}
	public void setRegitered(boolean registrado){
		registered=registrado;
	}
	
}

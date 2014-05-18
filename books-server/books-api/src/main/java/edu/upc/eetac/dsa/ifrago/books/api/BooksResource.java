package edu.upc.eetac.dsa.ifrago.books.api;


import java.awt.print.Book;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;
import javax.validation.constraints.Null;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


import edu.upc.eetac.dsa.ifrago.books.api.DataSourceSPA;
import edu.upc.eetac.dsa.ifrago.books.api.MediaType;
import edu.upc.eetac.dsa.ifrago.books.api.model.Books;
import edu.upc.eetac.dsa.ifrago.books.api.model.BooksCollection;
import edu.upc.eetac.dsa.ifrago.books.api.model.Reviews;

@Path("/books")
public class BooksResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	@Context
	SecurityContext security;

	@GET
	@Produces(MediaType.BOOKS_API_BOOKS_COLLECTION)
	public BooksCollection getBooks(@QueryParam("length") int length, @QueryParam("after") int after) {
		
		BooksCollection books = new BooksCollection ();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();// Conectamos con la base de datos
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		
		try{
			boolean updateFromLast= after>0;
			stmt = conn.prepareStatement(buildGetBooksQuery(updateFromLast));// Para preparar la query con el metodo buildGetStingsQuery ( metodo de abajo )
			if (updateFromLast) {
				if(length==0){
					stmt.setInt(1, after);
					stmt.setInt(2,5);
				}else{
					stmt.setInt(1, after);
					stmt.setInt(2,length);
				}
			}else{
				
				if(length==0)
					stmt.setInt(1,5);
				else
					stmt.setInt(1,length);
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()){
				Books book = new Books();
				
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));
				
				//Nos encargamos de las reviews de cada libro
				PreparedStatement stmtr=null;
				stmtr=conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, book.getId());
				
				ResultSet rsr = stmtr.executeQuery();
				
				while(rsr.next()) {							
					Reviews review = new Reviews();
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));
					
					book.addReviews(review);
			
				}
				
				books.addBook(book);//Añadimos toda la info dellibro ( inclusive sus reviews ).
							
			}
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return books;
	}

	private String buildGetBooksQuery(boolean updateFromLast) {
		
		if(updateFromLast)
			return "select * from books where bookid>? limit ?;";
		else
			return "select *from books limit ?;";
	}

	@POST
	@Consumes(MediaType.BOOKS_API_BOOKS)
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Books createBook(Books book) {//Dates can are null
		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to create a book");
		
		ValidateBook(book);
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt = null;
		
		try{
			String sql= buildInsertBook();
			stmt=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, book.getTitle());
			stmt.setString(2, book.getAuthor());
			stmt.setString(3, book.getLanguage());
			stmt.setString(4, book.getEdition());
			//stmt.setDate(5, book.getEditiondate());
			//stmt.setDate(6, book.getPrintdate());
			stmt.setString(5, book.getEditorial());
			
			
			stmt.executeUpdate();// Ejecuto la actualización
			ResultSet rs = stmt.getGeneratedKeys();// query para saber si ha ido
													// bien la inserción
			if (rs.next()) {// Si ha ido bien me da la id del sting
				int bookid = rs.getInt(1);

				book = getBookFromDatabase(Integer.toString(bookid));
			} else {
				throw new BadRequestException("Can't create a Book");
			}
			
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return book;
		
	}

	private String buildInsertBook() {
		return "insert into books (title,author,language,edition,editorial) values(?,?,?,?,?); ";
	}

	private void ValidateBook(Books book) {//condiciones para poder ingresar enla BD
		if (book.getTitle() == null)
			throw new BadRequestException("Title can't be null.");
		if (book.getAuthor() == null)
			throw new BadRequestException("Author can't be null.");
		if (book.getEdition() == null)
			throw new BadRequestException("Edition can't be null.");
		//if (book.getEditiondate() == null)
		//	throw new BadRequestException("Editionale can't be null.");
		if (book.getLanguage() == null)
			throw new BadRequestException("Language can't be null.");
		if (book.getEditorial() == null)
			throw new BadRequestException("Language can't be null.");
		//if (book.getPrintdate() == null)
			//throw new BadRequestException("Printdate can't be null.");
		if (book.getTitle().length() > 80)
			throw new BadRequestException(
					"Title can't be greater than 80 characters.");
		if (book.getAuthor().length() > 20)
			throw new BadRequestException(
					"Author can't be greater than 20 characters.");
		if (book.getLanguage().length() > 15)
			throw new BadRequestException(
					"Language can't be greater than 15 characters.");
		if (book.getEdition().length() > 20)
			throw new BadRequestException(
					"Edition can't be greater than 20 characters.");
		if (book.getEditorial().length() > 20)
			throw new BadRequestException(
					"Editorial can't be greater than 20 characters.");
		
	}

	@GET
	@Path("/{bookid}")
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Response getBook(@PathParam("bookid") String bookid,@Context Request request) {
		
		//Creamos CacheControl
		CacheControl cc= new CacheControl();
		
		//Sacamos un book de la base de datos
		Books book = getBookFromDatabase(bookid);
		
		//Calculamos ETag de la ultima modificación de la reseña
		
		String s= book.getReviews()+book.getAuthor()+book.getEdition()+"21";

		
		EntityTag eTag = new EntityTag(Long.toString(s.hashCode()));
		
		
		//Comparamos el eTag creado con el que viene de la peticiOn HTTP
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);// comparamos
		
		if (rb != null) {// Si el resultado no es nulo, significa que no ha sido modificado el contenido ( o es la 1º vez )
				return rb.cacheControl(cc).tag(eTag).build();
		}

		
		// Si es nulo construimos la respuesta de cero.
		rb = Response.ok(book).cacheControl(cc).tag(eTag);
		
		return rb.build();
		
	}
	private Books getBookFromDatabase(String bookid){
		
		Books book=new Books();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		PreparedStatement stmtr=null;
		
		try{
			stmt=conn.prepareStatement(buildGetBookByIdQuery());
			stmt.setInt(1, Integer.valueOf(bookid));
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));
				
			}else{
				throw new NotFoundException("There's no sting with stingid ="
						+ bookid);
			}
			
			//Cogemos las reviews de un libro
			stmtr=conn.prepareStatement(buildGetReviewBookByIdQuery());
			stmtr.setInt(1, Integer.valueOf(bookid));
			ResultSet rsr = stmtr.executeQuery();
			
			while(rsr.next()) {							
				Reviews review = new Reviews();
				review.setDateupdate(rsr.getDate("dateupdate"));
				review.setText(rsr.getString("text"));
				review.setUsername(rsr.getString("username"));
				review.setBookid(rsr.getInt("bookid"));
				
				book.addReviews(review);
		
			}
			
			
			
		}catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		

		
		return book;
	}
	
	private String buildGetReviewBookByIdQuery() {
		return "select * from reviews  where bookid=?;";
	}

	private String buildGetBookByIdQuery() {
		return "select * from books  where  bookid=?;";
	}
	
	@GET
	@Path("/search")
	@Produces(MediaType.BOOKS_API_BOOKS_COLLECTION)
	public BooksCollection searchByAuthorBook (@QueryParam("author") String author,@QueryParam("length") int length){
		BooksCollection books = new BooksCollection();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();// Conectamos con la base de datos
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the d ( linea 413 )atabase",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		String sql =buildSearchByAhutor(author);
		PreparedStatement stmt = null;
		
		
		try {
			System.out.println("Query a construir: "+sql);
			stmt = conn.prepareStatement(sql);
			if (length != 0) {		
				stmt.setString(1,"%"+author+"%");
				stmt.setInt(2, length);// Limitamos el numero de resultados,				
			} else if (length==0) {
				stmt.setString(1,"%"+author+"%");
				stmt.setInt(2, 3);// Limitamos el numero de resultados,
			}
			
			System.out.println("Query salida: "+ stmt);
			ResultSet rs = stmt.executeQuery();//mandamos la query
			System.out.println("Resultado: "+ rs);
			while (rs.next()){
				Books book = new Books();
				
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));
				
				//Nos encargamos de las reviews de cada libro
				PreparedStatement stmtr=null;
				stmtr=conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, book.getId());
				
				ResultSet rsr = stmtr.executeQuery();
				
				while(rsr.next()) {							
					Reviews review = new Reviews();
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));
					
					book.addReviews(review);
			
				}
				
				books.addBook(book);//Añadimos toda la info dellibro ( inclusive sus reviews ).
							
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}		
		return books;
	}
	
	private String buildSearchByAhutor(String author) {
		
		if(author==null)
			throw new BadRequestException("Se tiene que poner algo para buscar por el autor.");
		else
			return"SELECT * FROM books WHERE  author LIKE ? LIMIT ? ;";
	}

	@PUT
	@Path("/{bookid}")
	@Consumes(MediaType.BOOKS_API_BOOKS)
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Books updateBook(@PathParam("bookid") int bookid, Books book){

		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");
		System.out.println("Eres admin");
		
		ValidateBookforUpdate(book);
		System.out.println("Book validado");
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("BD establecida");
		PreparedStatement stmt = null;
		
		try{
			String sql = buildUpdateBook();
			System.out.println("Query escrita");
			stmt=conn.prepareStatement(sql);
			System.out.println("Query cargada");
			stmt.setString(1, book.getTitle());
			stmt.setString(2, book.getAuthor());
			stmt.setString(3, book.getLanguage());
			stmt.setString(4, book.getEdition());
			stmt.setDate(5, (Date) book.getEditiondate());
			stmt.setDate(6, (Date) book.getPrintdate());
			stmt.setString(7, book.getEditorial());
			stmt.setInt(8,bookid);
			System.out.println("Query lista");
			int rows = stmt.executeUpdate();
			System.out.println("Query ejecutada");
			String sbookid= Integer.toString(bookid);
			System.out.println("Miramos si hay contestación row="+sbookid);
			if (rows == 1){
				System.out.println("Cogemos el book modificado");
				book = getBookFromDatabase( sbookid);
			System.out.println("Hemos cogido el book modificado");
			}else {
				throw new NotFoundException("There's no book with bookid="
						+ bookid);// Updating inexistent sting
			}
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		
		return book;
	}
	
	private void ValidateBookforUpdate(Books book) {
		System.out.println("Dentro del validate");
		if (book.getTitle()!= null && book.getTitle().length() > 80)
			throw new BadRequestException(
					"Title can't be greater than 80 characters.");
		if (book.getAuthor()!= null && book.getAuthor().length() > 20)
			throw new BadRequestException(
					"Author can't be greater than 20 characters.");
		if (book.getLanguage()!= null && book.getLanguage().length() > 15)
			throw new BadRequestException(
					"Language can't be greater than 15 characters.");
		if (book.getEdition()!= null && book.getEdition().length() > 20)
			throw new BadRequestException(
					"Edition can't be greater than 20 characters.");
		if (book.getEditorial()!= null && book.getEditorial().length() > 20)
			throw new BadRequestException(
					"Editorial can't be greater than 20 characters.");
		System.out.println("Fuera del validate");
	}

	private String buildUpdateBook() {
		return "update books set title=ifnull(?, title), author=ifnull(?, author), language=ifnull(?, language), edition=ifnull(?, edition), editiondate=ifnull(?, editiondate), printdate=ifnull(?, printdate), editorial=ifnull(?, editorial) where bookid=?;";
	}

	@DELETE
	@Path("/{bookid}")
	public void deleteBook(@PathParam("bookid") String bookid){
		
		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		
		try{
			String sql = buildDeleteBook();
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, Integer.valueOf(bookid));

			int rows = stmt.executeUpdate();

			if (rows == 0)
				throw new NotFoundException("There's no sting with book="
						+ bookid);
			
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		
	}

	private String buildDeleteBook() {
		return "delete from books where bookid=?;";
	}

	@POST
	@Path("/reviews")
	@Consumes(MediaType.BOOKS_API_REVIEWS)
	@Produces(MediaType.BOOKS_API_REVIEWS)
	public Reviews createReview( Reviews review) {
		if (!security.isUserInRole("registered"))
			throw new ForbiddenException("You are not allowed to create reviews for a book");
		System.out.println("Eres el registred");
		
		ValidateReview(review);
		System.out.println("Review validada");
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("Connexion BD establecida");
		
		PreparedStatement stmt = null;
		PreparedStatement stmt2 = null;
		boolean truedate;
		
		if( review.getDateupdate() !=null)
			 truedate=false;
		else
			 truedate=true;
		
		System.out.println("Conociemiento de Dateupdate");
		
		try{
			String sql= buildInsertReview(truedate);
			stmt=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			
			if(truedate){
				stmt.setString(1, review.getUsername());
				stmt.setString(2, review.getText());
				stmt.setInt(3, review.getBookid());
				stmt.setDate(4, (Date) review.getDateupdate());
			}else{
				stmt.setString(1, review.getUsername());
				stmt.setString(2, review.getText());
				stmt.setInt(3, review.getBookid());
			}

			
			stmt.executeUpdate();// Ejecuto la actualización
			System.out.println("Query ejecutada");
			
			
				
/*				//Cogemos las reviews de un libro
				PreparedStatement stmtr = conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, Integer.valueOf(bookid));
				ResultSet rsr = stmtr.executeQuery();
				
				while(rsr.next()) {							
					Reviews review = new Reviews();
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));
					
					book.addReviews(review);*/
			
			//	}
		
			sql= locateReview();
			System.out.println("Query para ver la review: "+sql);
			stmt2=conn.prepareStatement(sql);
			stmt2.setInt(1, review.getBookid());
			stmt2.setString(2, review.getUsername());
			System.out.println("QLe metemos bookid: "+review.getBookid());
			System.out.println("Lemetemos Username: "+review.getUsername());

			
			ResultSet rs = stmt2.executeQuery();
			
			System.out.println("Query ejecutada");
			
			if (rs.next()) {
				System.out.println("Miramos contestacion query");

				review.setBookid(rs.getInt("bookid"));
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));
			} else {
				throw new BadRequestException("Can't create a Review");
			}
			
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return review;
	}
	
	private boolean reviseDuplicateReview (Reviews review) {//miramos que no hay ninguna reseña con el mismo bookid y username
	
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		
		try{
			stmt=conn.prepareStatement(locateReview());
			stmt.setInt(1,review.getBookid());
			stmt.setString(2, review.getUsername());

			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				review.setBookid(rs.getInt("bookid"));
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));
				
				return false;
				
			}else{
				return true;
			}
				
		}catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	private String buildInsertReview(boolean truedate) {
		if (truedate)
			return "insert into reviews (username,text,bookid,dateupdate) values (?,?,?,?);";
		else
			return "insert into reviews (username,text,bookid) values (?,?,?);";
	}
	
	private Reviews getReviewFromDatabase(String username, int bookid ){
		
		Reviews review = new Reviews();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;

		
		try{
			stmt=conn.prepareStatement(locateReview());
			stmt.setInt(1,bookid);
			stmt.setString(2,username);

			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));
				review.setBookid(rs.getInt("bookid"));
				
			}else{
				throw new NotFoundException("There's no review with bookid ="
						+ bookid+ "and username = "+ username);
			}
						
		}catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		
		return review;
	}
	private String locateReview(){
		return"select * from reviews where bookid=? and username=?;";
	}

	private void ValidateReview(Reviews review) {//Para poder crear una reseña tiene que  aporbar todos los if
		if (review.getUsername() == null)
			throw new BadRequestException("Username can't be null.");
		if (review.getText() == null)
			throw new BadRequestException("Text can't be null.");
		if (review.getBookid() == 0)
			throw new BadRequestException("Bookid can't be 0.");
		if (review.getUsername().length() > 20)
			throw new BadRequestException(
					"Username can't be greater than 20 characters.");
		if (review.getText().length() > 500)
			throw new BadRequestException(
					"Text can't be greater than 500 characters.");
		if( reviseDuplicateReview(review)== false)
			throw new BadRequestException(
					"No puedes hacer mas reseñas en esta ficha.");
			
		
	}

	@PUT
	@Path("/reviews")
	@Consumes(MediaType.BOOKS_API_REVIEWS)
	@Produces(MediaType.BOOKS_API_REVIEWS)
	public Reviews updateReview(@QueryParam("username") String username,
			@QueryParam("bookid") int bookid, Reviews review){
		
		//if (!security.isUserInRole("register"))
		//	throw new ForbiddenException("You are not allowed to update a review");
		validateReviewfroUpdate(review);
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt = null;
		
		try {
			String sql = buildUpdateReview();
			stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, review.getText());
			stmt.setDate(2, (Date) review.getDateupdate());
			stmt.setInt(3, review.getBookid());
			stmt.setString(4, review.getUsername());

			int rows = stmt.executeUpdate();
			if (rows == 1)
				review = getReviewFromDatabase(username,bookid);
			else {
				;// Updating inexistent sting
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		
		return review;
	}


	private String buildUpdateReview() {
		return "update reviews set  text=ifnull(?, text), dateupdate=ifnull(?, dateupdate) where bookid=? and username=?;";
	}

	private void validateReviewfroUpdate(Reviews review) {
		if (review.getUsername() != null && review.getUsername().length()>20)
			throw new BadRequestException("Username not be greater 20");
		if ( review.getUsername()!= null && review.getUsername().length() > 20)
			throw new BadRequestException(
					"Username can't be greater than 20 characters.");
		if (review.getText() != null && review.getText().length() > 500)
			throw new BadRequestException(
					"Text can't be greater than 500 characters.");
		
	}

	@DELETE
	@Path("/reviews")
	public void deleteReview(@QueryParam("username") String username,
			@QueryParam("bookid") int bookid){
		
		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");
		
		System.out.println("Eres el admin");
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("Conectado a la BD");
		
		PreparedStatement stmt=null;
		
		try{
			System.out.println("query haciendose");
			String sql = buildDeleteReview();
			stmt = conn.prepareStatement(sql);
			System.out.println("query casi hecha");
			stmt.setString(1, username);
			stmt.setInt(2,bookid);
		
			System.out.println("query rellena");
			int rows = stmt.executeUpdate();
			System.out.println("query enviada");
			if (rows == 0)
				throw new NotFoundException("There's no review with bookid="
						+ bookid+"and username= "+username);
			
			
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		
	}

	private String buildDeleteReview() {
		return "delete from reviews where username=? and bookid=?;";
	}
	
	
}

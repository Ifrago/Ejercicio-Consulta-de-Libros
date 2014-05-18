Proyecto Java de Consultas de libro.

Esta colgado los scripts de la base de datos. Por si está mal planteada.
Los modelos de Books, y reviews estan en: books-server\books-api\src\main\java\edu\upc\eetac\dsa\ifrago\books\api\model

Los .java para recibir los metodos HTTP: books-server\books-api\src\main\java\edu\upc\eetac\dsa\ifrago\books\api

El problema está enel archivo BookResource, el metodo getBook.

Estan todas las funciones hechas:
-GetBook(ID)
-GetBooks
-UpdateBook
-UpdateReview( tiene un problema, no se relacina con el libro)
-DeleteBook
-DeleteReview
-CreateBook( hay que añadirle cuando le metes las fechas; printdate y editiondate)
-CreateReview

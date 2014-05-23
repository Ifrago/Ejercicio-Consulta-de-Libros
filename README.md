Proyecto Java de Consultas de libro.

SCRIPTS MYSQL
Localizacion de los scripts Base de datos, booksdb: sql.
Para ejecutarlos:
    -Ir al directorio donde se encuentre la carpeta sql del proyecto en local.
    -Entrar desde consola en mysql.
    -Cargar los scripts en este orden: source booksdb-schema.sql, source booksdb-users.sql, booksdb-data.sql

MODELOS
Los modelos de Books, y reviews estan en: books-server\books-api\src\main\java\edu\upc\eetac\dsa\ifrago\books\api\model

METODOS
Los .java para recibir los metodos HTTP: books-server\books-api\src\main\java\edu\upc\eetac\dsa\ifrago\books\api

FUNCIONALIDADES
-Obtener una lista de libros , sin paginar y paginados.
-Obtener un solo libro a partir de su ID.
-Obtener un libro a partir de una busqueda del autor y el titulo del libro.
-Crear un libro a partir de su ID, solo el administrador.
-Actualizar un libro a partir de su ID, solo el administrador.
-Borrar un libro a partir de su ID, solo el administrador.
-Crear una Review de un libro, solo un user registrado y solo una review por libro.
-Actualizar una Review de un libro, solo el user registrado que lo ha creado.
-Borrar una Review de un libro, solo el usuario registrado que lo ha creado y el administrador.

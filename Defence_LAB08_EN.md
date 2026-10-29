# Laboratory Work 8 Defence Guide

Akhmet Didar and Magazhan Nurdaulet, IS-2405

## Ready to say during the defence

Good afternoon. Our eighth laboratory work replaces the in-memory catalogue with PostgreSQL. We kept the HTML forms and the REST API from the earlier labs, but the repository now extends JpaRepository. The development profile connects to the springlab database. Hibernate manages the schema, and we can inspect the real rows through the database page or psql. Restarting Java keeps the saved books because the development profile uses update rather than create-drop.

Book is now a regular entity class. It has a protected no-argument constructor and an identifier that PostgreSQL generates through an identity column. Genre is stored as a string, so the database contains PROGRAMMING instead of an enum ordinal. The server assigns createdAt. A version column lets Hibernate detect a stale concurrent update. Our test loads the same book in two persistence contexts and confirms that the second transaction cannot overwrite the first update.

Author has a list of books and Book has one author. The foreign key lives in book.author_id, so Book owns this association. Author.books uses mappedBy to refer to Book.author. The addBook helper changes both Java references. Book also owns a many-to-many association with Tag. Hibernate creates book_tag with book_id and tag_id. A shared tag survives when a book or link is deleted because the mapping does not cascade REMOVE to tags.

The repository contains five derived queries. Their names describe genre equality, a case-insensitive title fragment, a year range with descending order, the newest three books of a genre, and a count through the nested author name. A JPQL query combines genre and a minimum year using named parameters. The bulk method uses Modifying and a transaction. It archives books older than the supplied year, increments their versions and clears the persistence context. Against the original seed, the year 2010 updates three rows with one SQL statement.

The N+1 experiment starts with an empty persistence context. Reading eight books and then their author names produces four statements, because those books share three authors. The fetch join loads the books and authors together and needs one statement. We clear the context again before the second measurement so an earlier cache hit cannot make the result misleading. For paged API reads, an entity graph fetches only to-one associations. We do not fetch a tag collection together with a database limit.

The collection endpoint always returns a page. Spring MVC builds Pageable from page, size and sort. The default size is five and the default order is title. We append id to make ties stable. Page one with size three and year descending contains the expected next three records and the total of eight. Our Page experiment requires a content query and a count query. Slice needs one query and tells us whether another slice exists, but does not provide the exact total. We publish Page because this catalogue needs a total count for navigation.

Our first variant adds Publisher. Publisher.books is inverse, and Book owns publisher_id. The derived method findByPublisherNameIgnoreCase follows that association and returns the three Manning books. The database page shows the actual foreign key column. This demonstrates that a repository method name can traverse a related entity without handwritten SQL.

The build passed sixty-two tests. A separate packaged-application run checked thirty-three HTTP responses with ninety-four assertions. It created a book, stopped and restarted the application, and retrieved the same identifier and creation time from PostgreSQL. The ORM removed repetitive CRUD code, but we still had to choose transaction boundaries, ownership, fetching and pagination carefully. Thank you for your attention.

## Demonstration sequence

1. Start the database, then Lab08Application. Show the PostgreSQL connection and successful startup in IntelliJ.
2. Open /lab8 and show the eight initial books with author_id and publisher_id.
3. Open Book, Author, Tag and Publisher. Explain the owning sides and the addBook helper.
4. Run the repository query experiment. Read one generated SQL statement and its returned titles.
5. Show N+1 before and after, then Page versus Slice.
6. Open /api/books?page=1&size=3&sort=year,desc and explain content and totalElements.
7. Run the Publisher experiment for Manning.
8. Show the bulk update SQL and archived/version values. This POST changes the demonstration data.

## Questions and answers

1. **Why can Book no longer be a record?** JPA needs an entity class with an accessible no-argument constructor and a shape suitable for managed identity and proxies. A Java record is final and its components are final. Our entity uses a protected constructor and mutable persistent fields.
2. **What does IDENTITY do?** PostgreSQL generates the identity value during the insert. Hibernate reads that value back and assigns it to the managed entity.
3. **Why EnumType.STRING?** The stored value names the genre. Reordering enum constants does not change the meaning of old rows, while an ordinal mapping would reinterpret their positions.
4. **What is Version for?** Hibernate includes the expected version in an update condition and advances it. A stale update affects no matching versioned row and triggers optimistic locking failure. Our API maps database and optimistic update conflicts to 409.
5. **Who owns Book and Author?** Book owns it because Book.author declares JoinColumn and book contains author_id.
6. **What does mappedBy mean?** It names the owning Java attribute. Author.books refers to Book.author. Removing mappedBy creates a separate association mapping rather than the intended inverse side.
7. **Where is the many-to-many table?** Hibernate creates book_tag from Book.tags and its JoinTable. Its columns are book_id and tag_id, each with a foreign key.
8. **Why set both sides?** JPA writes the owning field, while Java code may navigate the inverse collection. Changing only Author.books leaves the foreign key unset. The helper adds to the list and assigns Book.author.
9. **What does cascade ALL do when an author is deleted?** It propagates the removal to that author's books. Hibernate also removes their join-table links. Shared Tag and Publisher rows remain.
10. **What does orphanRemoval add?** Removing a book from the managed author collection deletes that child at flush. This differs from deleting the parent itself.
11. **Who implements BookRepository?** Spring Data creates a repository proxy at startup, combines its generic implementation with parsed method queries and validates declared queries.
12. **How do you read findTop3ByGenreOrderByYearDesc?** findTop3 is the limiting subject. Genre is the predicate. OrderByYearDesc specifies the descending order. Hibernate sends a limited query to PostgreSQL.
13. **What SQL does the title fragment query produce?** It uses an upper-case comparison with LIKE and a surrounding percent pattern. Spring Data escapes a percent or underscore supplied as part of the fragment. Our PostgreSQL test checks both literal characters.
14. **Why named JPQL parameters?** Genre and year identify their meaning without relying on argument position. The Param annotations bind those names explicitly.
15. **Why Modifying and clearAutomatically?** Modifying tells Spring Data to execute DML rather than read a result list. Bulk DML bypasses ordinary managed-state updates, so clearing prevents stale entity state. A service transaction encloses the update. We also advance versions explicitly.
16. **What are the N+1 numbers?** Four before and one after on the original seed. There is one book query and three distinct author loads. Eight books do not imply eight extra author queries because the persistence context reuses a loaded author.
17. **How do join and join fetch differ?** A normal join can filter by related rows without initialising the association. Join fetch loads the associated entity into the persistence context with the owner.
18. **How many statements do Page and Slice cause?** For our full middle page, Page uses two and Slice uses one. Page needs a total count. Slice reads one extra row for hasNext. Spring can infer totals and skip the count on some short pages.
19. **What happens to sort=nosuchfield?** Our service validates the property at request time and returns 400 problem JSON. Arbitrary client property names cannot be validated when Java code is compiled.
20. **What is ddl-auto?** The default development profile uses update for persistence across restarts. An explicit classroom profile uses create-drop. PostgreSQL does not normally get automatic schema creation by default. Production should use reviewed migrations and validate or none.
21. **Why open-in-view=false?** A web view cannot quietly trigger additional queries after the service transaction. We fetch the required author information deliberately and map API responses to DTOs. Tag details are mapped within a transaction.
22. **How does the Publisher variant work?** Publisher has an inverse one-to-many collection. Book.publisher owns publisher_id. The repository traverses publisher.name in a derived method, and the measured Manning result contains three books.

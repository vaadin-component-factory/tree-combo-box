package com.vaadin.componentfactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;

@Route("mixed")
public class Mixed extends Div {

    Random rand = new Random();

    public Mixed() {
        setSizeFull();
        Library library = new Library();
        TreeComboBox<TreeObject> treeComboBox = new TreeComboBox<>(item -> {
            return createTreeObjectLabel(item);
        });
        treeComboBox.setItems(library.getAuthors(), library::getChildren);
        treeComboBox.setClearButtonVisible(false);
        treeComboBox.setDisableFiltering(true);
        treeComboBox.setIcon(VaadinIcon.BOOK.create());
        treeComboBox.setPopupWidth("400px");
        TreeObject defaultValue = library
                .getChildren(
                        library.getChildren(library.getAuthors().get(0)).get(0))
                .get(0);
        treeComboBox.setValue(defaultValue);

        Button button = new Button("Reset",
                e -> treeComboBox.setValue(defaultValue));

        treeComboBox.addValueChangeListener(e -> {
            Notification.show(
                    "Value change: " + createTreeObjectLabel(e.getValue()));
        });

        add(treeComboBox, button);
    }

    private String createTreeObjectLabel(TreeObject item) {
        if (item instanceof Book) {
            Book book = (Book) item;
            return book.getName() + " (" + book.getPages() + " pages)";
        } else if (item instanceof Chapter) {
            Chapter chapter = (Chapter) item;
            return chapter.getBook().getName() + ": " + chapter.getName() + " ("
                    + chapter.getPages() + " pages)";
        } else {
            return item.getName();
        }
    }

    public class Library {
        List<Book> books = new ArrayList<>();

        public Library() {
            var numAuthors = rand.nextInt(4) + 2;
            for (int i = 1; i < numAuthors; i++) {
                Author author = new Author();
                author.setName("Author " + i);
                var numBooks = rand.nextInt(4) + 2;
                for (int j = 1; j < numBooks; j++) {
                    Book book = new Book(author);
                    book.setName("Book " + i + "/" + j);
                    books.add(book);
                }
            }
        }

        public List<TreeObject> getAuthors() {
            return books.stream().map(book -> book.getAuthor()).distinct()
                    .collect(Collectors.toList());
        }

        public List<TreeObject> getChildren(TreeObject object) {
            if (object instanceof Book) {
                Book book = (Book) object;
                return book.getChapters();
            } else if (object instanceof Author) {
                return books.stream()
                        .filter(book -> book.getAuthor().equals(object))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }
    }

    public abstract class TreeObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class Author extends TreeObject {

    }

    public class Book extends TreeObject {
        private int pages;
        private Author author;
        private List<TreeObject> chapters = new ArrayList<>();

        public Book(Author author) {
            setPages(pages);
            setAuthor(author);
            int chaps = rand.nextInt(4) + 1;
            for (int i = 0; i < chaps; i++) {
                Chapter chapter = new Chapter(this);
                chapter.setName("Chapter " + i);
                chapters.add(chapter);
            }
        }

        public int getPages() {
            return chapters.stream()
                    .mapToInt(chap -> ((Chapter) chap).getPages()).sum();
        }

        public void setPages(int pages) {
            this.pages = pages;
        }

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(Author author) {
            this.author = author;
        }

        public List<TreeObject> getChapters() {
            return chapters;
        }
    }

    public class Chapter extends TreeObject {
        private int pages;
        private Book book;

        public Chapter(Book book) {
            this.book = book;
            setPages(rand.nextInt(10) + 1);
        }

        public int getPages() {
            return pages;
        }

        public void setPages(int pages) {
            this.pages = pages;
        }

        public Book getBook() {
            return book;
        }

        public void setBook(Book book) {
            this.book = book;
        }
    }
}

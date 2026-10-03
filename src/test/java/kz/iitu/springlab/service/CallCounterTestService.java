package kz.iitu.springlab.service;

/** Fast test target in the package selected by the real service-layer pointcut. */
public class CallCounterTestService {
    public String findById(long id) {
        return "Item no. " + id;
    }

    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }

    public String removeTwice(long id) {
        return this.remove(id) + "; " + this.remove(id + 1);
    }
}

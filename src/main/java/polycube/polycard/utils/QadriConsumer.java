package polycube.polycard.utils;

@FunctionalInterface
public interface QadriConsumer<T, U, V, W> {
    void accept(T t, U u, V v, W w);

    static <T, U, V, W> QadriConsumer<T, U, V, W> empty() {
        return (_, _, _, _) -> {};
    }
}

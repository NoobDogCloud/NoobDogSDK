package common.java.Concurrency;

@FunctionalInterface
public interface AsyncTaskRunnable {
    boolean run(AsyncStruct asyncStruct);
}

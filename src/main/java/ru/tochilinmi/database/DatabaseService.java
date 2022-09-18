package ru.tochilinmi.database;

public interface DatabaseService <T> {
    public T findById(Object primaryKey);
    public void save(T object);
}

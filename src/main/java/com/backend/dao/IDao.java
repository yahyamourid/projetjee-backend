package com.backend.dao;

import java.util.List;

public interface IDao <T>{
    List<T> getAll();
    T findById (long id);
    T create(T o);
    T update(T o);
    boolean delete(T o);


}

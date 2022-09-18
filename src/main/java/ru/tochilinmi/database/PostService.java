package ru.tochilinmi.database;

import ru.tochilinmi.entities.PostEntity;

public class PostService implements DatabaseService<PostEntity>{

    private DatabaseConnector parentConnector;

    public PostService(DatabaseConnector parentConnector){ this.parentConnector = parentConnector;}

    @Override
    public PostEntity findById(Object primaryKey){ return this.parentConnector.getManager().find(PostEntity.class, primaryKey);}

    @Override
    public  void  save(PostEntity object){ this.parentConnector.getManager().persist(object);}
}

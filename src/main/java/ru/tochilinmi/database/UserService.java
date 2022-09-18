package ru.tochilinmi.database;

import ru.tochilinmi.entities.UserEntity;

public class UserService implements DatabaseService<UserEntity>{

    private DatabaseConnector parentConnector;

    public UserService(DatabaseConnector parentConnector){ this.parentConnector = parentConnector;}

    @Override
    public UserEntity findById(Object primaryKey){ return this.parentConnector.getManager().find(UserEntity.class, primaryKey);}

    @Override
    public  void  save(UserEntity object){ this.parentConnector.getManager().persist(object);}
}

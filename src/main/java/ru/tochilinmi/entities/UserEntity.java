package ru.tochilinmi.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class UserEntity {

    @Id
    private Long id;
    private String login;
    private String password;
    @OneToMany
    private List<PostEntity> postEntityList;

    public UserEntity(Long id,String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
        postEntityList = new ArrayList<>();
    }

    public UserEntity() {
        postEntityList = new ArrayList<>();
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PostEntity> getPostList() {
        return postEntityList;
    }

    public void addPost(PostEntity post) {
        this.postEntityList.add(post);
    }
}

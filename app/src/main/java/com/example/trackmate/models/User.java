package com.example.trackmate.models;
    

public class User {
    private String fullName;
    private String email;
    private String contact;
    private String home;
    private String street;
    private String city;
    private String country;
    private String displayName;

    public User() {
    }

    public User(String fullName, String email, String contact, String home, String street, String city, String country) {
        this.fullName = fullName;
        this.email = email;
        this.contact = contact;
        this.home = home;
        this.street = street;
        this.city = city;
        this.country = country;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

